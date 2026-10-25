#!/usr/bin/env bash
# 부하 테스트 전체 실행: 환경을 띄우고, 시나리오마다 캐시와 카카오 호출 기록을 비운 뒤 k6를 돌려 결과를 표로 정리한다.
#
#   ./loadtest/run.sh                  # 시나리오 4개 모두
#   ./loadtest/run.sh hit miss         # 고른 시나리오만
#   KEEP=1 ./loadtest/run.sh           # 끝나도 환경을 내리지 않는다
#   RATE=50 DURATION=60s ./loadtest/run.sh
#
# 필요한 것: Docker (Compose 포함), python3
set -euo pipefail

cd "$(dirname "$0")"
COMPOSE=(docker compose -f compose.yaml)
KAKAO_ADMIN=http://localhost:18089/__admin
SCENARIOS=("$@")
[ ${#SCENARIOS[@]} -eq 0 ] && SCENARIOS=(hit miss shared stampede)

mkdir -p results
"${COMPOSE[@]}" up -d --build --quiet-pull
[ "${KEEP:-0}" = 1 ] || trap '"${COMPOSE[@]}" down' EXIT

echo "백엔드가 준비되기를 기다리는 중..."
for port in 18081 28081; do
  until curl -sf "localhost:$port/actuator/health/readiness" >/dev/null; do sleep 2; done
done

reset() {
  "${COMPOSE[@]}" exec -T valkey valkey-cli flushall >/dev/null
  curl -sf -X DELETE "$KAKAO_ADMIN/requests" >/dev/null
}

kakao_calls() {
  curl -sf "$KAKAO_ADMIN/requests/count" -H 'Content-Type: application/json' \
    -d '{"method":"GET","urlPath":"/v2/local/search/category.json"}' \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["count"])'
}

k6() {
  "${COMPOSE[@]}" run --rm -e SCENARIO="$1" -e RATE="${RATE:-30}" -e DURATION="${2:-${DURATION:-30s}}" \
    k6 run --quiet pick.js >/dev/null 2>&1
}

# JIT 컴파일 등으로 처음 몇 초가 느리므로 결과에 넣지 않는 예열 실행을 먼저 한다
echo "예열 중..."
k6 shared 15s || true

for scenario in "${SCENARIOS[@]}"; do
  reset
  echo "실행: $scenario"
  # 기준 미달(실패율 1% 이상)이어도 결과는 남긴다
  k6 "$scenario" || echo "  k6 기준 미달 (results/$scenario.json 확인)"
  python3 - "$scenario" "$(kakao_calls)" <<'EOF'
import json, sys
scenario, calls = sys.argv[1], int(sys.argv[2])
path = f"results/{scenario}.json"
result = json.load(open(path))
result["kakaoCalls"] = calls
json.dump(result, open(path, "w"), indent=2)
EOF
done

python3 - "${SCENARIOS[@]}" <<'EOF'
import json, sys
print()
print("| 시나리오 | 요청 수 | 실패율 | 중앙값 | p95 | p99 | 카카오 호출 |")
print("| --- | ---: | ---: | ---: | ---: | ---: | ---: |")
for scenario in sys.argv[1:]:
    r = json.load(open(f"results/{scenario}.json"))
    ms = r["ms"]
    print(f"| {scenario} | {r['requests']} | {r['failedRate']:.1%} | {ms['med']:.0f}ms | {ms['p95']:.0f}ms | {ms['p99']:.0f}ms | {r['kakaoCalls']} |")
EOF

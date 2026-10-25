// 메뉴 뽑기 API 부하 테스트. SCENARIO 환경변수로 하나를 골라 실행한다 (run.sh가 차례로 실행).
//
//   hit       같은 칸에서 계속 뽑기 (캐시 적중)
//   miss      매번 다른 칸에서 뽑기 (캐시 미스 → 매번 카카오 3페이지 호출)
//   shared    칸 20개를 두 백엔드에 번갈아 요청 (공유 캐시라면 칸마다 카카오를 한 번만 부른다)
//   stampede  비어 있는 한 칸에 동시에 50개 요청 (중복 호출 방지 전후 비교 기준)
import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';

const SCENARIO = __ENV.SCENARIO || 'hit';
const TARGETS = (__ENV.TARGETS || 'http://backend-1:8080,http://backend-2:8080').split(',');
// 초당 요청 수와 측정 시간 (hit·miss·shared)
const RATE = Number(__ENV.RATE || 30);
const DURATION = __ENV.DURATION || '30s';

// 칸 크기(0.001도, 약 100m)에 맞춰 n번째 칸의 중심 좌표를 만든다
const BASE_LAT = 37.4;
const BASE_LNG = 127.0;
function cell(n) {
  return {
    lat: (BASE_LAT + (n % 1000) * 0.001).toFixed(3),
    lng: (BASE_LNG + Math.floor(n / 1000) * 0.001).toFixed(3),
  };
}

const arrival = {
  executor: 'constant-arrival-rate',
  rate: RATE,
  timeUnit: '1s',
  duration: DURATION,
  preAllocatedVUs: 50,
  maxVUs: 200,
};

const scenarios = {
  hit: arrival,
  miss: arrival,
  shared: arrival,
  // 50명이 한 번씩, 거의 같은 순간에 요청한다
  stampede: { executor: 'per-vu-iterations', vus: 50, iterations: 1, maxDuration: '30s' },
};

export const options = {
  scenarios: { [SCENARIO]: scenarios[SCENARIO] },
  thresholds: { http_req_failed: ['rate<0.01'] },
  summaryTrendStats: ['avg', 'med', 'p(95)', 'p(99)', 'max'],
};

function cellFor(iteration) {
  switch (SCENARIO) {
    case 'hit':
    case 'stampede':
      return cell(0);
    case 'miss':
      return cell(iteration);
    case 'shared':
      return cell(iteration % 20);
    default:
      throw new Error(`알 수 없는 SCENARIO: ${SCENARIO}`);
  }
}

export default function () {
  const iteration = exec.scenario.iterationInTest;
  const { lat, lng } = cellFor(iteration);
  // 요청을 두 백엔드에 번갈아 보낸다 (운영의 Pod 2개)
  const target = TARGETS[iteration % TARGETS.length];
  const res = http.get(`${target}/api/menus/pick?lat=${lat}&lng=${lng}`, { tags: { name: 'pick' } });
  check(res, { 'status 200': (r) => r.status === 200 });
}

export function handleSummary(data) {
  const d = data.metrics.http_req_duration.values;
  const result = {
    scenario: SCENARIO,
    requests: data.metrics.http_reqs.values.count,
    failedRate: data.metrics.http_req_failed.values.rate,
    ms: { avg: d.avg, med: d.med, p95: d['p(95)'], p99: d['p(99)'], max: d.max },
  };
  return {
    [`/results/${SCENARIO}.json`]: JSON.stringify(result, null, 2),
    stdout: `${JSON.stringify(result)}\n`,
  };
}

# menu-pick-backend

근처 음식점을 기준으로 오늘의 메뉴를 랜덤으로 뽑아 주는 사이트의 백엔드 (Spring Boot, Java 21).

이 파일은 문서 지도다. 작업 전에 아래에서 필요한 문서를 찾아 읽는다.

## 문서 지도

| 문서 | 언제 읽나 |
| --- | --- |
| [README.md](README.md) | 실행 방법, 환경변수, API 명세, 패키지 구조, 캐시 구조 |
| [docs/git-conventions.md](docs/git-conventions.md) | 이슈·브랜치·커밋·PR·머지 규칙. **코드를 바꾸기 전에 반드시 읽는다** |
| [loadtest/README.md](loadtest/README.md) | 부하 테스트 실행 방법과 측정 결과 |
| [.github/workflows/ci.yml](.github/workflows/ci.yml) | CI 단계(테스트 → 이미지 → gitops 배포) |
| [.github/workflows/conventions.yml](.github/workflows/conventions.yml) | PR의 브랜치 이름·제목·`Closes #N` 검사 |
| [.github/ISSUE_TEMPLATE/](.github/ISSUE_TEMPLATE) | 이슈를 만들 때 쓰는 기능·버그 템플릿 |
| [.github/pull_request_template.md](.github/pull_request_template.md) | PR 본문 템플릿 |

## 관련 저장소

- [menu-pick-frontend](../menu-pick-frontend) — 이 API를 호출하는 프론트엔드
- [menu-pick-gitops](../menu-pick-gitops) — 배포 설정. `main` 머지 시 CI가 이미지 태그를 갱신한다

## 자주 쓰는 명령

```sh
./gradlew bootRun   # 실행 (.env에 KAKAO_REST_KEY 필요)
./gradlew test      # 테스트 (Valkey 통합 테스트는 Docker 필요)
```

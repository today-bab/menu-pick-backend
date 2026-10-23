# menu-pick-backend

근처 음식점을 기준으로 오늘의 메뉴를 랜덤으로 뽑아 주는 사이트의 백엔드.
카카오 로컬 API로 반경 1km 안의 음식점을 찾아 메뉴별로 묶고, 그중 메뉴 하나와 그 메뉴를 파는 가게들을 돌려준다.

## 실행

```sh
cp .env.example .env   # KAKAO_REST_KEY 채우기
./gradlew bootRun
```

설정값은 `.env`(git 제외) 또는 환경변수로 넣는다. 둘 다 있으면 환경변수가 우선한다.

| 환경변수               | 설명                                  | 기본값                  |
| ---------------------- | ------------------------------------- | ----------------------- |
| `KAKAO_REST_KEY`       | 카카오 REST API 키 (필수)             | 없음                    |
| `CORS_ALLOWED_ORIGINS` | API를 호출할 프론트엔드 주소 (쉼표 구분) | `http://localhost:5173` |

## API

`GET /api/menus/pick?lat={위도}&lng={경도}&exclude={직전 메뉴}`

```json
{
  "menu": "냉면",
  "restaurants": [
    {
      "name": "평양면옥",
      "distanceMeters": 210,
      "lat": 37.567,
      "lng": 126.979,
      "placeUrl": "http://place.map.kakao.com/123"
    }
  ]
}
```

오류는 `application/problem+json`(RFC 9457)으로 응답한다.

| 상태 | 경우                      |
| ---- | ------------------------- |
| 400  | 좌표 누락 또는 범위 오류  |
| 404  | 근처에 음식점이 없음      |
| 502  | 카카오 API 호출 실패      |

## 패키지 구조

```
com.menupick
├── domain
│   ├── menu         메뉴 뽑기 (controller, service, model, dto, exception)
│   └── restaurant   근처 음식점 조회 (client, model, dto, config, exception)
└── global           CORS 설정, 공통 예외 처리
```

음식점 조회는 `RestaurantClient` 인터페이스 뒤에 카카오 구현(`KakaoRestaurantClient`)과
캐시(`CachingRestaurantClient`, 약 100m 칸 단위로 10분)를 겹쳐 둔다.

## 테스트

```sh
./gradlew test
```

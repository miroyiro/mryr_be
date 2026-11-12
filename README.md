# miroyiro-backend (Spring Boot 3.5 / Java 21)

구조·규칙: 노션 03 디렉토리 구조, 06 API 규칙. 이전 Node 코드는 `legacy-node/` (참고용, 실행 안 함).

## 현재 상태 (2026-11-11)
| 영역 | 내용 | 확인 |
|---|---|---|
| 전환 | Node(Express) → Spring Boot 3.5 / Java 21. 옛 코드는 `legacy-node/` | — |
| DB | Flyway `V1__init.sql` = ERD v1.2 main (22 tables) | 로컬 Postgres 적용 ✅ |
| 공통 | 내부 키 필터, 공통 에러 응답, `/api/health` (DB 확인 포함) | `./gradlew build` ✅, health ✅ |
| 내부 API | `/internal/pipeline/events·news` (멱등 게시), `/internal/catalog/anime·streaming·names` | 키 있으면 200 / 없으면 401 ✅ |
| 남은 것 | auth/user (JWT·OAuth), 서비스 API(`/api/**`), admin, job, CI | — |

## 로컬 실행
```bash
cp .env.example .env            # 값 채우기 (커밋 금지)
docker compose up -d            # Postgres 16 (main DB)
set -a; . ./.env; set +a        # 환경변수 로드 (IntelliJ 는 Run Configuration 에 넣기)
./gradlew bootRun               # 기동 시 Flyway 가 V1__init.sql 적용
curl localhost:8080/api/health  # {"status":"ok","db":"ok",...}
```

## 내부 API 확인 (scrap → backend)
```bash
curl -H "X-Internal-Key: $INTERNAL_KEY" localhost:8080/internal/catalog/names
```

## 빌드 / 배포
```bash
./gradlew build                 # 테스트 포함
docker build -t miroyiro-backend .
```

## 패키지 (도메인별)
- `global/`  config · security(InternalKeyFilter, JWT 는 auth 담당) · error · response
- `domain/`  anime · artist · event · news (+ auth · user · follow · search · home 추가 예정)
- `internal/` scrap 이 부르는 API: `/internal/pipeline/events·news`, `/internal/catalog/anime·streaming·names`
- `src/main/resources/db/migration/` Flyway. **적용된 V 파일은 수정 금지**, 변경은 `V2__*.sql`

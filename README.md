# miroyiro-backend (Spring Boot 3.5 / Java 21)

구조·규칙: 노션 03 디렉토리 구조, 06 API 규칙. 이전 Node 코드는 `legacy-node/` (참고용, 실행 안 함).

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

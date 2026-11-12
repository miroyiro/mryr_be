# legacy-node (참고용, 실행 안 함)

11-10 기준 Node(Express) 백엔드 코드. Spring Boot 로 옮기면서 **참고용으로만** 남겨 둠.

옮긴 이유 (11-10 점검)
- `node src/server.js` 기동 불가: `./jobs/popular-search.job.js` 없음, 깨진 import 17곳
  (예: `services/* → ../repositories/anime.repository.js`, 실제 위치는 `repositories/anime/`)
- 빈 파일 22개 (follow.*, admin/validation middleware, validators, utils, constants, jobs 3개, resolver.service)
- 팔로우·내부 API(/internal/**)·마이그레이션 없음
→ 실제로 동작하는 부분이 적어서 지금 Spring 으로 시작하는 쪽이 비용이 적음 (03 문서 'Node → Spring 대응표')

로직(쿼리·검증 규칙)을 옮길 때 여기 파일을 보고 `src/main/java/com/miroyiro/backend/domain/*` 로 이식.
다 옮기면 이 폴더 통째로 삭제.

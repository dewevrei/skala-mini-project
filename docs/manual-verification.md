# 통합 계약 검증 기록

검증일: 2026-08-18 (Asia/Seoul)
상태: `DONE_WITH_CONCERNS`

자동 테스트와 정적 계약 검증은 완료했다. 다만 이 작업 환경에는 MySQL, Redis, Google OAuth, Gemini 실행 환경이 없으므로 실제 스키마 검증, 서버 기동, OAuth·세션·Gemini 수동 검증은 성공으로 간주하지 않고 아래에 `PENDING`으로 남긴다.

## 자동 검증 결과

| 구분 | 명령 | 결과 | 근거 |
|---|---|---:|---|
| Backend 전체 테스트 | Java `25.0.4`에서 `backend\.\mvnw.cmd clean test` | exit `0` | 17개 test suite, 97 tests, failures 0, errors 0, skipped 0 |
| Frontend production build | `frontend\npm run build` | exit `0` | Vite가 실제 router 진입점부터 1,691 modules를 변환하고 `dist`를 생성 |
| diff 공백 검사(검증 문서 작성 전) | `git diff --check` | exit `0` | 오류 출력 없음 |

Frontend build에는 의존성의 `/* #__PURE__ */` 주석 제거 안내와 500 kB 초과 chunk 경고가 있었지만 빌드는 성공했다. 기능·계약을 막는 오류는 아니며 MVP 성능 SLA는 범위 밖이다.

## 문서-구현 정적 대조

| 계약 영역 | 결과 | 확인 내용 |
|---|---|---|
| 공통 응답과 code/status | PASS | `ApiResponse` 네 필드, 실패 `data=null`, 성공/실패 code 종류 방어를 확인했다. 오류·성공 명세의 60개 code와 `ApiCode` 60개가 정확히 일치하며 누락·추가 code가 없다. Controller의 생성 `201`, 조회·수정·삭제 `200`과 응답 data wrapper 형태를 REST 명세와 대조했다. |
| 소유권과 404 | PASS(정적/단위) | Project는 `id + userId`, Column은 `id + projectId + project.userId`, Task는 `id + projectId + project.userId`로 조회한다. 타인 또는 다른 Project의 ID는 `PROJECT_NOT_FOUND`, `COLUMN_NOT_FOUND`, `TASK_NOT_FOUND` 404로 숨기며 서비스 테스트가 이를 포함한다. 실제 두 계정 HTTP 검증은 아래 PENDING이다. |
| OAuth와 화면 이동 | PASS(정적) | `/oauth2/authorization/google`, `/login/oauth2/code/google` 허용, 성공 `/projects`, 일반 실패 `/login?error=oauth`, 세션 저장 실패 `/login?error=session-service-unavailable`, 로그아웃 후 Frontend `/login` 처리를 확인했다. |
| Redis 세션·쿠키·CSRF | PASS(정적/단위) | Spring Session Redis만 사용하고 namespace `ai-kanban:session`, idle timeout 24h, `SESSION` HttpOnly/SameSite=Lax/path `/`/max-age 7d를 확인했다. 로그아웃은 세션 무효화와 쿠키 즉시 만료를 수행한다. 상태 변경 요청은 세션 CSRF와 `X-CSRF-TOKEN`을 사용하고 Frontend가 토큰을 취득·전송한다. Redis 예외는 메모리 fallback 없이 JSON 503 또는 OAuth redirect로 변환되며 classifier 단위 테스트가 통과했다. |
| Project 불변식 | PASS(정적/단위) | 사용자 내 대소문자 무시 이름 유일성, `createdAt DESC, id DESC`, Project와 `Todo`/`In Progress`/`Done` 원자적 생성, Project cascade 삭제 계약을 확인했다. |
| Column 불변식 | PASS(정적/단위) | Project 내 대소문자 무시 이름 유일성, 새 Column 마지막 배치, 전체 ID 중복·누락·타 Project 거부, `sortOrder,id` 결정 순서, 첫 Column의 AI 대상화, 마지막 Column 삭제 409를 확인했다. |
| Task 생성·수정·날짜·검색 | PASS(정적/단위) | 일반 생성은 선택 Column 맨 아래, priority 1, 날짜 null이다. 내용 수정에서 priority/date/sort 등 읽기 전용 필드를 거부한다. 날짜 두 key의 지정·해제와 역순 날짜를 허용한다. 검색은 trim·대소문자 무시·LIKE escape를 적용하고 빈 Column group을 유지한다. |
| Task 이동·정렬 | PASS(정적/단위) | Items status는 대상 Column 맨 아래, Board position은 `targetColumnId`와 nullable `beforeTaskId`를 사용한다. 같은 열/열 간 이동 후 관련 순서를 정규화하며 타 Project Column과 잘못된 기준 Task를 거부한다. |
| AI 입력·검증·재시도 | PASS(정적/단위) | title/description 모두 필수다. 정확한 root/item 필드, 비어 있지 않은 배열, title/description, 정수 priority 1..5, 최종 description 5,000자 제한을 검증한다. 구조·값 실패만 동일 입력으로 1회 재시도하며 호출 실패는 즉시 fallback한다. |
| AI 옵션·저장·실패 정책 | PASS(정적/단위) | `gemini-2.5-flash`, timeout 30초, temperature 0.2, topP 0.9, max output 8,192, JSON schema, topK/thinking/safety 미지정을 확인했다. 성공은 배열 순서대로 첫 Column 맨 아래에 `원본 제목 - AI 설명`과 원래 priority를 저장한다. 호출·최종 검증 실패는 원본 1건/priority 1 fallback이다. batch DB 실패는 `TASK_BATCH_SAVE_FAILED`, fallback DB 실패는 `TASK_FALLBACK_SAVE_FAILED`이며 batch 실패에서 fallback하지 않는다. 두 저장 경로 모두 별도 transaction이고 rollback 대상 RuntimeException을 다시 던진다. |
| 시간대 | PASS(정적/단위) | Backend/JDBC/Hibernate가 `Asia/Seoul`을 사용하고 DB `DATETIME(6)`을 `OffsetDateTime +09:00`로 변환한다. 4개 시간대 단위 테스트가 통과했다. |
| DDL 수동 적용 | PASS(정적) | DDL은 `database/ai_kanban.sql`에만 있고 자동 migration이 없다. JPA는 `ddl-auto=validate`여서 스키마를 생성·수정하지 않는다. DDL의 utf8mb4/case-insensitive collation, nullability, 복합 FK, priority check, cascade를 엔티티와 대조했다. |
| 환경 변수와 비밀값 | PASS(정적) | DB, Google OAuth, Gemini key, Redis 연결·선택적 인증은 `${...}` placeholder로 받는다. 실제 비밀값은 이 기록에 조회·출력·기록하지 않았다. 현재 관련 환경 변수는 모두 미설정이다. |
| Frontend route 연결 | PASS(정적/build) | `/login`, `/projects`, `/projects/:projectId/board`, `/projects/:projectId/items`, `/profile`이 실제 view에 연결되고 production build에 포함된다. 별도 Task 등록 route나 AI 화면/모드는 없다. |
| Task 등록 action | PASS(정적/build) | 공통 `TaskCreateModal` action slot의 DOM 순서는 `Cancel`, `Create`, `AI Generate`다. Create는 선택 Column, AI Generate는 현재 첫 Column API를 호출한다. 처리 중 두 요청 버튼과 Cancel을 비활성화하고 완료 후 해제한다. |
| 이미지 기반 UI 범위 | PASS(정적/build) | Project 목록, Board, Items, Column 추가, Task 추가의 배치·밀도를 반영했다. Project 생성·수정과 Task 수정은 큰 공통 overlay를 사용한다. Open/Closed, Private, Project 검색·정렬, Column 색상·설명·숨김, Estimate, New view, Insights, Workflows, 첨부, Markdown 등 이미지에만 있는 기능은 구현하지 않았다. |

## 실행 환경 확인

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GEMINI_API_KEY`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_USERNAME`, `REDIS_PASSWORD`: 모두 미설정.
- `localhost:3306`, `localhost:6379`: listening process 없음.
- 따라서 실제 secret 값을 요구하거나 출력하지 않았고, 외부 연동이 필요한 검증은 실행하지 않았다.

## PENDING 수동 검증

### PENDING — MySQL/JPA schema validation과 runtime smoke

필요 환경: MySQL `8.0.46`, 사용자가 관리하는 DB 계정, Java 25, Redis 8.8, OAuth/Gemini 환경 변수.

1. MySQL Workbench에서 `database/ai_kanban.sql`을 사용자가 직접 실행한다. 애플리케이션이 DDL을 실행하게 하지 않는다.
2. 새 terminal/session에 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 주입한다. 값은 문서나 Git에 남기지 않는다.
3. Redis와 나머지 필수 환경 변수를 준비한 뒤 `backend\.\mvnw.cmd spring-boot:run`을 실행한다.
4. Hibernate schema validation 성공과 서버 기동을 확인하고 `GET http://localhost:8080/api/v1/auth/csrf`가 `200 CSRF_TOKEN_ISSUED`인지 확인한다.
5. 별도 임시 schema에서 의도적으로 column/nullability를 다르게 만든 뒤 해당 DB를 가리켜 시작이 실패하는지 확인하고, 검증 후 임시 schema만 제거한다.

### PENDING — Google OAuth callback/redirect

필요 환경: Google OAuth client, 승인된 redirect URI `http://localhost:8080/login/oauth2/code/google`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, 실행 중인 Backend/Frontend/MySQL/Redis.

1. `/oauth2/authorization/google`에서 로그인하고 callback 후 `http://localhost:5173/projects`로 이동하는지 확인한다.
2. OAuth 승인을 취소하거나 유효하지 않은 callback을 사용해 `/login?error=oauth`를 확인한다.
3. 로그인 후 `/api/v1/users/me`가 `200 USER_READ`인지 확인하고 최초 사용자 생성, 재로그인 name/email 동기화, nickname 유지 여부를 DB의 비밀값이 아닌 사용자 필드만으로 확인한다.

### PENDING — Redis session/cookie/CSRF/failure

필요 환경: Redis Open Source `8.8`(필요 시 username/password는 환경 변수), 실제 로그인 가능한 OAuth 환경.

1. 로그인 후 `SESSION` 쿠키가 HttpOnly, SameSite=Lax, Path=/, Max-Age=7일인지 브라우저 개발자 도구로 확인한다.
2. CSRF 없이 상태 변경 API가 `403 CSRF_TOKEN_INVALID`, `/auth/csrf`의 header/token을 사용하면 성공하는지 확인한다.
3. Backend만 재시작한 뒤 기존 세션이 유지되는지 확인한다.
4. 로그아웃 후 Redis session 삭제와 `SESSION` Max-Age=0, Frontend `/login` 이동을 확인한다.
5. Redis를 중지하고 인증 필요 JSON API가 `503 SESSION_SERVICE_UNAVAILABLE`인지 확인한다. OAuth 흐름에서는 `/login?error=session-service-unavailable`인지 확인한다. 메모리 세션으로 성공하면 안 된다.
6. Redis를 다시 시작하고 재로그인 가능 여부를 확인한다. Redis 재시작 전 세션 보존은 보장 항목이 아니다.

### PENDING — 실제 owner 404와 CRUD/UI 흐름

필요 환경: 서로 다른 Google 계정 2개, 실행 중인 전체 로컬 stack.

1. 계정 A와 B가 각각 Project/Column/Task를 만든다.
2. A의 session과 유효한 CSRF로 B의 각 resource ID에 GET/PATCH/DELETE/move 요청을 보내고 body가 `success=false`, 해당 `*_NOT_FOUND`, `data=null`인 404인지 확인한다.
3. Project 생성·수정·삭제, Column 추가·이름변경·drag reorder·마지막 Column 삭제 방지, Board Task 생성·수정·삭제·drag move, Items 검색·status 이동·날짜 지정/해제/역순을 새로고침 후 다시 확인한다.
4. Task modal에서 버튼이 `Cancel`, `Create`, `AI Generate` 순서인지, Cancel이 확인 없이 닫히고 원래 화면을 유지하는지, 별도 AI 화면으로 이동하지 않는지 확인한다.

### PENDING — 실제 Gemini 정상/fallback/DB rollback

필요 환경: `GEMINI_API_KEY`, 정상 실행 중인 전체 stack, 인증 session과 CSRF token. 키 값은 command history, 문서, screenshot, log에 남기지 않는다.

1. title/description으로 `POST /api/v1/projects/{projectId}/tasks/ai-generate`를 호출해 `201 TASKS_CREATED`, 첫 Column, 배열 순서, priority 1..5, 날짜 null, `원본 제목 - AI 설명`을 확인한다.
2. 별도의 일시적 네트워크 차단 또는 테스트용 무효 credential 환경에서 호출 실패를 유도해 오류 노출 없이 원본 fallback Task 1건이 저장되는지 확인하고 즉시 정상 환경으로 복구한다.
3. 격리된 검증 DB에서 AI batch 저장 중 제약 위반을 재현해 전체 rollback, `500 TASK_BATCH_SAVE_FAILED`, fallback 미생성을 확인한다. 운영성 데이터에는 수행하지 않는다.

## 남은 우려

- 외부 PENDING 항목은 현재 환경 부재 때문에 실행하지 못했으며 통과로 간주하지 않았다.
- Controller/Security/DB integration 및 Frontend browser 자동 테스트는 확정 범위 밖이므로, 위 수동 절차가 실제 HTTP·브라우저 계약의 최종 확인이다.
- Frontend production bundle의 큰 chunk 경고는 남아 있으나 현재 MVP의 기능·빌드 계약을 막지 않는다.

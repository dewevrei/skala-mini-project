# 구현 명세 — AI 칸반보드 & 개인 과제 관리자 MVP

## 목표와 동기

Google 사용자가 서로 완전히 격리된 개인 Project와 칸반보드를 사용하고, 직접 Task를 등록하거나 Gemini가 큰 할 일을 독립 실행 Task 목록으로 분해하도록 한다. Task에는 선택적인 시작일과 종료일을 기록할 수 있다. MVP는 Task 분해와 칸반 관리에 집중하며 계층, 협업, 캘린더와 자동 일정 편성을 만들지 않는다.

## 권위 있는 상세 계약

이 명세가 단독 실행 가능한 핵심 계약이다. 다음 문서는 같은 내용을 목적별로 더 상세히 표현한 부속 문서다.

- `docs/functional-requirements.md`
- `docs/non-functional-requirements.md`
- `docs/domain-model.md`
- `docs/erd.md`
- `docs/rest-api-spec.md`
- `docs/validation-rules.md`
- `docs/authorization-rules.md`
- `docs/error-response-specification.md`
- `docs/ai-integration-contract.md`

충돌 시 이 명세가 제품 동작의 최종 기준이다. 상세 문서는 이 명세를 구체화하며, 구현 중 임의로 요구사항을 추가할 수 없다. 상세 문서가 없어도 아래 데이터·API·오류·AI 계약만으로 핵심 구현을 결정할 수 있어야 한다.

## 제품 동작

### 인증과 사용자

- 모든 Google 계정의 OAuth 2.0 로그인을 허용한다.
- Google `sub`를 `google_id`로 사용해 User를 식별한다.
- 최초 로그인은 User와 `Google name + UUID` nickname을 만들고, 이후 로그인은 name/email만 갱신한다.
- nickname은 수정 가능하고 대소문자를 무시해 전역 유일하다.
- Spring Session Data Redis와 Redis Open Source 8.8 기반 서버 세션, HttpOnly 쿠키, CSRF를 사용하고 JWT는 사용하지 않는다.
- Redis 세션은 마지막 요청 이후 24시간 미사용 시 만료하고 `SESSION` 쿠키는 7일간 유지한다.
- 로그인 사용자는 로그아웃해 Redis 세션과 쿠키를 즉시 종료할 수 있다.
- 로그인 성공은 `/projects`, OAuth 실패는 `/login?error=oauth`, 로그아웃 성공은 `/login`으로 이동한다.
- 각 User는 자신의 Project·BoardColumn·Task만 접근한다.

### Project와 BoardColumn

- Project name은 소유자 범위에서 대소문자를 무시하고 유일하며 description은 선택이다.
- 생성 시 `Todo`, `In Progress`, `Done` BoardColumn을 순서대로 함께 생성한다.
- Project 목록은 `createdAt DESC, id DESC`의 최근 생성순이다.
- BoardColumn name은 Project 범위에서 대소문자를 무시하고 유일하다.
- 새 BoardColumn은 현재 마지막 열 뒤에 추가한다.
- 모든 Column은 이름·순서를 변경하고 삭제할 수 있으나 Project에 최소 한 Column은 남아야 한다.
- Column 삭제는 경고 확인 후 포함 Task까지 완전 삭제한다.
- 현재 첫 번째 Column이 AI Task와 AI fallback Task의 등록 대상이다. 열 순서가 바뀌면 즉시 대상도 바뀐다.
- Project 삭제는 경고 확인 후 모든 하위 데이터를 완전 삭제한다.

### 일반 Task

- Board의 Column `+` 또는 Items의 Column 그룹 `Add item`에서만 등록한다.
- 선택 Column 맨 아래에 생성한다.
- title 필수, description 선택, priority는 항상 1이다.
- 일반 생성 시 startDate와 endDate는 모두 null이다.
- title/description은 수정 가능하고 priority는 수정 불가다.
- 삭제는 확인 후 완전 삭제한다.
- 등록 화면의 `Cancel`은 입력 변경 여부와 관계없이 확인창 없이 입력을 폐기하고 시작한 Board 또는 Items로 돌아간다.

### AI Task

- AI Generate에는 원본 title과 description이 모두 필요하다.
- Spring AI ChatClient와 Gemini Developer API를 사용한다. 기본 모델은 `gemini-2.5-flash`다.
- 호출당 timeout은 30초, temperature는 0.2, topP는 0.9, 최대 출력은 8,192 tokens다. 안전 설정은 별도로 override하지 않는다.
- 모델은 개수 제한 없이 문맥에 맞는 평면 Task 목록을 `{title, description, priority}`로 반환한다.
- priority는 1~5 정수이며 1이 가장 높다.
- AI 생성 성공 시 원본 title/description을 부모 또는 별도 Task로 추가 저장하지 않는다. 호출·검증 실패 때는 아래 fallback 규칙에 따라 원본 기반 Task 한 건만 저장한다.
- 백엔드는 각 저장 description을 정확히 `원본 title + " - " + AI description`으로 만든다.
- 모든 AI Task는 응답 순서대로 현재 첫 번째 Column 맨 아래에 한 트랜잭션으로 저장한다.
- AI Task와 fallback Task의 startDate와 endDate는 모두 null이다.
- 최초 구조 검증 실패 시 한 번 재요청한다. 호출 실패 또는 최종 검증 실패는 원본으로 일반 Task 하나를 조용히 저장한다.
- 유효 AI 결과의 DB 저장 실패는 전체 rollback 후 500이며 fallback하지 않는다.

### Items와 Board

- Frontend route는 로그인 `/login`, Project 목록 `/projects`, Board `/projects/:projectId/board`, Items `/projects/:projectId/items`, 회원정보 `/profile`이다.
- Task 등록은 별도 route가 아닌 시작 Board/Items 위 공통 modal/overlay이며 일반 Create와 AI Generate에 같은 Cancel 정책을 적용한다.
- Board는 Column과 카드의 저장 순서를 표시하며 priority 자동 정렬을 하지 않는다.
- 드래그는 동일 열 순서 또는 열과 위치를 함께 변경한다.
- Items는 BoardColumn 순서대로 그룹화하고 각 그룹은 Board 카드 순서를 따른다.
- Items Status 선택 이동은 대상 Column 맨 아래다.
- Items에서 각 Task의 startDate와 endDate를 직접 지정하거나 null로 비울 수 있다. 두 날짜는 서로 독립적이며 endDate가 startDate보다 앞서도 허용한다.
- title 부분 검색만 제공하며 대소문자를 구분하지 않고 빈 검색어는 전체를 표시한다. 검색 중에도 빈 Column 그룹을 유지한다.
- 현재 탭은 mutation 응답으로 즉시 갱신하고 다른 탭은 활성화될 때 재조회한다.
- 동시 변경은 잠금·버전 충돌 없이 마지막 저장 결과를 사용한다.

## 데이터 계약과 불변 규칙

- 주요 ID는 자동 증가 BIGINT다.
- User `1:N` Project, Project `1:N` BoardColumn, BoardColumn `1:N` Task다.
- Task의 project와 column의 project는 반드시 같다.
- Project에는 항상 최소 한 Column이 있다.
- Project name은 User 범위, Column name은 Project 범위, nickname은 전역 범위에서 대소문자를 무시하고 유일하다.
- Task priority는 생성 후 불변이며 1~5다.
- Task startDate와 endDate는 nullable DATE이며 두 값의 선후관계를 강제하지 않는다.
- Project/Column/Task 삭제는 soft delete 없이 확정된 범위 전체를 삭제한다.
- DDL은 사용자가 Workbench로 적용하고 JPA `ddl-auto=validate`로만 검사한다.

### 물리 필드 계약

| Entity | 필드 |
|---|---|
| User (`users`) | `id BIGINT PK AUTO_INCREMENT`; `google_id VARCHAR(255) NOT NULL UNIQUE`; `name VARCHAR(255) NOT NULL`; `email VARCHAR(320) NOT NULL UNIQUE`; `nickname VARCHAR(255) NOT NULL UNIQUE`; `created_at`, `updated_at DATETIME(6) NOT NULL` |
| Project (`projects`) | `id BIGINT PK`; `user_id BIGINT NOT NULL FK`; `name VARCHAR(100) NOT NULL`; `description TEXT NULL`; timestamps; `UNIQUE(user_id,name)` |
| BoardColumn (`board_columns`) | `id BIGINT PK`; `project_id BIGINT NOT NULL FK`; `name VARCHAR(50) NOT NULL`; `sort_order INT NOT NULL`; timestamps; `UNIQUE(project_id,name)` |
| Task (`tasks`) | `id BIGINT PK`; `project_id BIGINT NOT NULL`; `column_id BIGINT NOT NULL`; `title VARCHAR(200) NOT NULL`; `description TEXT NULL`; `start_date DATE NULL`; `end_date DATE NULL`; `priority TINYINT NOT NULL CHECK 1..5`; `sort_order BIGINT NOT NULL`; timestamps |

Task의 `(project_id,column_id)`는 동일 Project의 BoardColumn `(project_id,id)`를 참조한다. Project 삭제는 Column과 Task를, Column 삭제는 Task를 cascade한다. User 삭제 API는 없다. 문자열은 `utf8mb4`와 대소문자 무시 collation을 사용한다. 열 조회는 `sort_order,id`, 카드 조회는 `sort_order,id` 순이다.

### 입력 한도

Project name 1~100, description 0~2000, Column name 1~50, Task title 1~200, Task description 0~5000, 사용자가 바꾸는 nickname 1~100자를 MVP 고정 검증값으로 사용한다. 모든 문자열은 trim하고 필수 문자열의 빈 값과 제어 문자를 거부한다. 최초 nickname은 `trim(Google name) + "-" + 소문자 전체 UUID`이며 255자를 넘으면 이름 부분만 줄인다.

## API 계약

- Frontend/Backend는 REST로 통신한다.
- OAuth 리다이렉트 외 JSON 응답은 `ApiResponse<T>(success, code, message, data)`다.
- 오류 data는 항상 null이며 HTTP status를 의미에 맞게 사용한다.
- 타인 자원은 존재를 숨기기 위해 404로 처리한다.
- 서버 idempotency key는 없고 Frontend가 생성 요청 중 버튼만 비활성화한다.
- 정확한 경로·요청·응답 key·error code는 `docs/rest-api-spec.md`와 `docs/error-response-specification.md`를 따른다.

### 공통 응답

```text
ApiResponse<T> = { success: boolean, code: string, message: string, data: T|null }
```

실패는 `success=false`, `data=null`이다. 생성은 201, 조회·수정·삭제 성공은 body를 포함한 200, 검증 400, 미인증 401, CSRF 403, 없음/타인 자원 404, 중복·마지막 열 삭제 409, 내부 저장 실패 500, Redis 세션 저장소 장애 503이다. OAuth redirect/callback만 JSON 봉투의 예외다. `code`는 접두사 없는 `UPPER_SNAKE_CASE`다.

### 인증·User endpoint

| Method | Path | 요청/결과 |
|---|---|---|
| GET | `/oauth2/authorization/google` | Google 로그인 redirect |
| GET | `/login/oauth2/code/google` | OAuth callback |
| GET | `/api/v1/auth/csrf` | `{token,headerName}` |
| POST | `/api/v1/auth/logout` | 세션 종료 |
| GET | `/api/v1/users/me` | `{id,name,email,nickname,createdAt,updatedAt}` |
| PATCH | `/api/v1/users/me/nickname` | `{nickname}` |

OAuth callback에서 Redis 세션 저장이 실패하면 `/login?error=session-service-unavailable`로 리다이렉트한다.

### Project·Column endpoint

| Method | Path | 요청 key |
|---|---|---|
| GET/POST | `/api/v1/projects` | POST `{name,description}` |
| GET/PATCH/DELETE | `/api/v1/projects/{projectId}` | PATCH `{name,description}` |
| POST | `/api/v1/projects/{projectId}/columns` | `{name}` |
| PATCH/DELETE | `/api/v1/projects/{projectId}/columns/{columnId}` | PATCH `{name}` |
| PUT | `/api/v1/projects/{projectId}/columns/order` | `{orderedColumnIds:[...]}`; 현재 모든 열 ID를 중복·누락 없이 포함 |
| GET | `/api/v1/projects/{projectId}/board` | Column group과 Task 저장 순서 |
| GET | `/api/v1/projects/{projectId}/items?title=` | 같은 group 구조와 title 검색 |

### Task endpoint

| Method | Path | 요청 key |
|---|---|---|
| POST | `/api/v1/projects/{projectId}/columns/{columnId}/tasks` | `{title,description}`; priority 입력 금지 |
| POST | `/api/v1/projects/{projectId}/tasks/ai-generate` | `{title,description}` 둘 다 필수 |
| GET/PATCH/DELETE | `/api/v1/projects/{projectId}/tasks/{taskId}` | PATCH `{title,description}`만 허용 |
| PATCH | `/api/v1/projects/{projectId}/tasks/{taskId}/dates` | `{startDate,endDate}` 두 key 필수; 각 값은 `YYYY-MM-DD` 또는 null; 날짜 선후관계 제한 없음 |
| PATCH | `/api/v1/projects/{projectId}/tasks/{taskId}/status` | `{targetColumnId}`; 대상 열 맨 아래 |
| PATCH | `/api/v1/projects/{projectId}/tasks/{taskId}/position` | `{targetColumnId,beforeTaskId}`; null이면 맨 아래, 값이면 그 Task 바로 앞 |

Board/Items의 Task key는 `id,projectId,columnId,title,description,startDate,endDate,priority,sortOrder,createdAt,updatedAt`; Column key는 `id,projectId,name,sortOrder,taskCount`; Project key는 `id,name,description,createdAt,updatedAt`이다. Items 검색에서도 Column group을 유지한다.

### 성공 응답 `data` exact shape

| 작업 | `data` shape |
|---|---|
| CSRF 조회 | `{token:string, headerName:string}` |
| 로그아웃·Project/Column/Task 삭제 | `null` |
| 내 정보 조회·nickname 변경 | `{user: User}` |
| Project 목록 | `{projects: Project[]}` |
| Project 생성 | `{project: Project, columns: Column[]}` |
| Project 상세·수정 | `{project: Project}` |
| Column 생성·수정 | `{column: Column}` |
| Column 순서 변경 | `{columns: Column[]}` 전체 최신 순서 |
| Board/Items 조회 | `{project: Project, columnGroups: ColumnGroup[]}` |
| 일반 Task 생성·Task 상세·Task 수정·날짜 변경 | `{task: Task}` |
| AI 생성 또는 fallback | `{tasks: Task[]}` |
| Board 위치 이동·Items 상태 이동 | `{task: Task, affectedColumnGroups: ColumnGroup[]}` |

```text
User = {id,name,email,nickname,createdAt,updatedAt}
Project = {id,name,description,createdAt,updatedAt}
Column = {id,projectId,name,sortOrder,taskCount}
Task = {id,projectId,columnId,title,description,startDate,endDate,priority,sortOrder,createdAt,updatedAt}
ColumnGroup = {column:Column,tasks:Task[]}
```

`affectedColumnGroups`는 source와 target 열을 현재 BoardColumn 순서로 반환하고, 같은 열 안 재정렬이면 그 열 하나만 반환한다. 각 group의 tasks는 최신 전체 카드 순서다. 이 응답을 현재 탭의 기준으로 사용한다. 일반 생성 응답은 새 Task 하나로 해당 group 맨 아래를 갱신할 수 있고, AI 응답은 생성 Task 배열 순서로 추가한다. 삭제는 data null이므로 Frontend가 성공 대상 ID를 제거하고 필요하면 조회 API로 동기화한다.

### 성공 ApiCode exact 계약

| HTTP | code | message |
|---:|---|---|
| 200 | `CSRF_TOKEN_ISSUED` | 요청 보안 정보를 발급했습니다. |
| 200 | `LOGOUT_SUCCEEDED` | 로그아웃되었습니다. |
| 200 | `USER_READ` | 사용자 정보를 조회했습니다. |
| 200 | `USER_UPDATED` | 사용자 정보를 변경했습니다. |
| 200 | `PROJECT_LISTED` | 프로젝트 목록을 조회했습니다. |
| 201 | `PROJECT_CREATED` | 프로젝트가 생성되었습니다. |
| 200 | `PROJECT_READ` | 프로젝트를 조회했습니다. |
| 200 | `PROJECT_UPDATED` | 프로젝트가 수정되었습니다. |
| 200 | `PROJECT_DELETED` | 프로젝트가 삭제되었습니다. |
| 201 | `COLUMN_CREATED` | 보드 열이 생성되었습니다. |
| 200 | `COLUMN_UPDATED` | 보드 열이 수정되었습니다. |
| 200 | `COLUMNS_REORDERED` | 보드 열 순서가 변경되었습니다. |
| 200 | `COLUMN_DELETED` | 보드 열이 삭제되었습니다. |
| 200 | `BOARD_READ` | 보드를 조회했습니다. |
| 200 | `ITEMS_READ` | 작업 목록을 조회했습니다. |
| 201 | `TASK_CREATED` | 작업이 등록되었습니다. |
| 201 | `TASKS_CREATED` | 작업이 등록되었습니다. |
| 200 | `TASK_READ` | 작업을 조회했습니다. |
| 200 | `TASK_UPDATED` | 작업이 수정되었습니다. |
| 200 | `TASK_DATES_UPDATED` | 작업 날짜가 변경되었습니다. |
| 200 | `TASK_DELETED` | 작업이 삭제되었습니다. |
| 200 | `TASK_MOVED` | 작업이 이동되었습니다. |

AI success와 fallback은 모두 `TASKS_CREATED`를 사용하고 차이를 노출하지 않는다.

### 오류 ApiCode exact 계약

| HTTP | code | message |
|---:|---|---|
| 400 | `INVALID_REQUEST` | 요청 내용을 확인해 주세요. |
| 400 | `MALFORMED_JSON` | 요청 형식이 올바르지 않습니다. |
| 401 | `AUTHENTICATION_REQUIRED` | 로그인이 필요합니다. |
| 403 | `CSRF_TOKEN_INVALID` | 요청 보안 정보가 올바르지 않습니다. 다시 시도해 주세요. |
| 400 | `OAUTH_PROFILE_INVALID` | Google 계정 정보를 확인할 수 없습니다. |
| 400 | `OAUTH_EMAIL_INVALID` | Google 이메일 정보를 확인할 수 없습니다. |
| 409 | `DUPLICATE_GOOGLE_ID` | 이미 등록된 Google 계정입니다. |
| 409 | `DUPLICATE_EMAIL` | 이미 사용 중인 이메일입니다. |
| 400 | `INVALID_NICKNAME` | 닉네임을 확인해 주세요. |
| 409 | `DUPLICATE_NICKNAME` | 이미 사용 중인 닉네임입니다. |
| 500 | `NICKNAME_GENERATION_FAILED` | 닉네임을 생성하지 못했습니다. |
| 503 | `SESSION_SERVICE_UNAVAILABLE` | 로그인 서비스를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요. |
| 400 | `INVALID_PROJECT_NAME` | 프로젝트 이름을 확인해 주세요. |
| 400 | `INVALID_PROJECT_DESCRIPTION` | 프로젝트 설명을 확인해 주세요. |
| 409 | `DUPLICATE_PROJECT_NAME` | 같은 이름의 프로젝트가 이미 있습니다. |
| 404 | `PROJECT_NOT_FOUND` | 프로젝트를 찾을 수 없습니다. |
| 500 | `PROJECT_CREATE_FAILED` | 프로젝트를 생성하지 못했습니다. |
| 500 | `PROJECT_DELETE_FAILED` | 프로젝트를 삭제하지 못했습니다. |
| 400 | `INVALID_COLUMN_NAME` | 열 이름을 확인해 주세요. |
| 409 | `DUPLICATE_COLUMN_NAME` | 같은 이름의 열이 이미 있습니다. |
| 404 | `COLUMN_NOT_FOUND` | 보드 열을 찾을 수 없습니다. |
| 400 | `INVALID_COLUMN_ORDER` | 보드 열 순서가 올바르지 않습니다. |
| 409 | `LAST_COLUMN_DELETE_FORBIDDEN` | 프로젝트의 마지막 열은 삭제할 수 없습니다. |
| 500 | `COLUMN_DELETE_FAILED` | 보드 열을 삭제하지 못했습니다. |
| 400 | `INVALID_TASK_TITLE` | 작업 제목을 입력해 주세요. |
| 400 | `INVALID_TASK_DESCRIPTION` | 작업 설명을 확인해 주세요. |
| 400 | `INVALID_TASK_DATE` | 작업 날짜를 확인해 주세요. |
| 400 | `INVALID_TASK_PRIORITY` | 작업 우선순위가 올바르지 않습니다. |
| 400 | `INVALID_AI_DESCRIPTION` | AI 생성을 위한 설명을 입력해 주세요. |
| 400 | `READ_ONLY_FIELD` | 변경할 수 없는 항목이 포함되어 있습니다. |
| 404 | `TASK_NOT_FOUND` | 작업을 찾을 수 없습니다. |
| 400 | `INVALID_TASK_MOVE` | 작업을 이동할 위치가 올바르지 않습니다. |
| 400 | `INVALID_SEARCH_QUERY` | 검색어를 확인해 주세요. |
| 500 | `TASK_BATCH_SAVE_FAILED` | 생성된 작업을 저장하지 못했습니다. |
| 500 | `TASK_FALLBACK_SAVE_FAILED` | 작업을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요. |
| 500 | `TASK_DELETE_FAILED` | 작업을 삭제하지 못했습니다. |
| 404 | `RESOURCE_NOT_FOUND` | 요청한 대상을 찾을 수 없습니다. |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요. |

## AI 구조·검증 계약

모델의 구조화 출력은 다음 exact shape다. 추가 속성은 허용하지 않는다.

```json
{
  "tasks": [
    {"title": "string", "description": "string", "priority": 1}
  ]
}
```

- `tasks`는 필수이고 비어 있지 않아야 하며 개수 상한은 두지 않는다.
- 각 title·description은 trim 후 비어 있지 않아야 한다. AI title은 최대 200자다. 저장 Task description 전체 한도는 5,000자이므로 AI description은 `5000 - length(trim(originalTitle)) - length(" - ")`자 이하여야 한다.
- priority는 문자열·소수가 아닌 정수 1~5다.
- 하나라도 실패하면 전체 응답이 실패다.
- 구조 실패는 동일 입력으로 한 번 재요청하고 재실패 시 fallback한다. 연결·timeout·안전 차단처럼 결과가 없는 실패는 즉시 fallback한다.
- fallback Task는 `title=trim(originalTitle)`, `description=trim(originalDescription)`, `startDate=null`, `endDate=null`, `priority=1`, `column=요청 처리 시점의 첫 Column`, `sortOrder=해당 열의 다음 순서`다.
- AI 정상 결과도 `startDate=null`, `endDate=null`로 저장하며 모델 출력 schema에는 날짜를 포함하지 않는다.
- 저장 description은 정확히 `trim(originalTitle) + " - " + trim(aiDescription)`이다. 구분자 ` - `는 고정이며 최종 5,000자 이하여야 한다. 길이 초과는 해당 AI batch 전체의 구조·값 검증 실패로 처리한다.
- 유효 batch 저장은 전부 성공/rollback이다. DB 저장 오류는 `500 TASK_BATCH_SAVE_FAILED`이며 fallback하지 않는다.

### AI 호출 설정

- 호출 한 번의 timeout은 30초다. timeout은 즉시 fallback하며 호출 재시도를 하지 않는다.
- 구조·값 실패에만 두 번째 호출을 허용하므로 최악의 AI 대기 시간은 약 60초다.
- `temperature=0.2`, `topP=0.9`, 최대 출력 `8,192 tokens`다.
- 안전 설정, topK와 thinking 관련 옵션은 요청에서 덮어쓰지 않고 모델 기본값을 사용한다.
- 출력 token 한도는 기술 한도이며 Task 개수 비즈니스 제한이 아니다. 잘린 응답은 구조 실패로 처리한다.

## 세션·Redis 계약

- Spring Session Data Redis와 Redis Open Source 8.8을 사용한다. 기본 연결은 `localhost:6379`, namespace는 `ai-kanban:session`이다.
- Redis는 인증 세션만 저장하고 Project/Column/Task를 저장하지 않는다.
- 세션의 최대 미사용 시간은 24시간이다.
- `SESSION` 쿠키는 `HttpOnly=true`, `Path=/`, `SameSite=Lax`, `Max-Age=604800`초이며 로컬 HTTP에서는 `Secure=false`다.
- 쿠키에는 불투명 세션 ID만 저장한다. Redis 세션이 없으면 남은 쿠키만으로 인증하지 않는다.
- Backend 재시작 중 Redis가 유지되면 세션을 유지한다. Redis AOF/RDB 영속성은 요구하지 않아 Redis 재시작 후 세션 유지는 보장하지 않으며, 세션이 사라졌으면 재로그인한다.
- Redis 장애 중 메모리 세션으로 전환하지 않는다. 세션이 필요한 JSON API는 `503 SESSION_SERVICE_UNAVAILABLE`, OAuth callback은 `/login?error=session-service-unavailable`를 사용한다.
- CSRF token 조회도 세션 생성이 필요하면 503이다. logout 중 Redis가 실패하면 브라우저 쿠키는 만료시키되 503을 반환하며 성공으로 가장하지 않는다. Google 로그인 시작 또는 callback에서 Redis가 실패하면 `/login?error=session-service-unavailable`로 리다이렉트한다.

## 동시성·불변식 보장

- 일반 수정·정렬은 사용자에게 잠금이나 버전 충돌을 노출하지 않고 마지막 완료 변경을 최종 상태로 사용한다.
- Project의 최소 한 Column 규칙은 last-write-wins보다 우선한다. Column 삭제 트랜잭션은 Project 행을 짧게 DB write-lock한 뒤 현재 Column 수를 다시 계산한다. 동시에 두 열 삭제가 들어오면 직렬 처리되어 두 번째 요청은 마지막 열이면 409로 거부된다.
- 이 내부 트랜잭션 잠금은 사용자 편집 잠금이나 장기 lock UI가 아니며, 삭제 불변식을 지키는 동안에만 유지한다.

## 기술 결정

- Frontend: Vue.js 3, SFC, Vite, Composition API, Pinia, Vue Router, Axios, Element Plus
- Backend: Java 25, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA, Maven
- AI: Spring AI 2.0.x, Google GenAI starter, Gemini Developer API API key
- RDB: MySQL 8.0.46
- Session store: Redis Open Source 8.8
- 로컬 개발만 지원: Frontend 5173, Backend 8080, MySQL 3306, Redis 6379
- 비밀값은 환경에서 주입한다.
- 공식 화면 지원은 최소 너비 1280px의 최신 Chrome 정식 데스크톱 버전이다. 모바일·태블릿과 다른 브라우저 검증은 범위 밖이다.
- 키보드 포커스, 입력 이름 연결과 기본 색상 대비를 적용하되 접근성 인증 등급은 범위 밖이다.

## 범위 제외

- Project 공유·협업·초대·관리자 역할
- Task 계층, 담당자, 첨부, 카테고리, 저장소, 로드맵
- 예상 소요 시간·캘린더·자동 일정
- Markdown/Rich text
- 회원 탈퇴
- 운영 배포·HTTPS·백업·모니터링
- WebSocket/SSE/polling과 서버 idempotency
- 모바일·태블릿 반응형 UI, Chrome 외 브라우저 공식 지원, 접근성 인증

## Edge 규칙

- 마지막 Column 삭제 → 409, 데이터 변경 없음.
- Column 삭제 → 포함 Task도 삭제.
- Items 상태 이동 → 대상 Column 맨 아래.
- AI 일부 항목 검증 실패 → 전체 AI 결과 실패.
- AI 호출/최종 검증 실패 → 원본 일반 Task 한 건 성공 응답, 실패 사실 비노출.
- AI DB batch 실패 → 전체 rollback, 500, fallback 금지.
- 같은 POST가 실제로 여러 번 도착 → 중복 생성 가능.
- 타 User 자원 ID → 404.
- Redis 장애 → 메모리 fallback 없이 503.
- Task 등록 Cancel → 확인창 없이 입력 폐기 후 시작 화면 복귀.
- Task 날짜 변경 → 두 값 모두 null 가능, 유효한 달력 날짜만 허용, endDate가 startDate보다 앞서도 허용.

## 필수 검증

- Backend: 서비스 메서드 단위 테스트 `./mvnw test` 성공.
- Frontend: production build `npm run build` 성공.
- 테스트의 Google OAuth/Gemini는 실제 호출하지 않는다.
- Controller/security/DB integration/frontend unit/e2e 자동 테스트는 요구하지 않는다.
- DDL 수동 적용 후 JPA schema validation이 성공해야 한다.
- Redis 정상 세션, Backend 재시작 후 세션 유지, Redis 재시작 후 세션 유지 미보장·재로그인 허용, Redis 장애 503은 로컬에서 수동 확인한다.

## 확정 범위 경계

- 운영 배포·HTTPS·운영 CORS/쿠키, 백업·복구, 모니터링과 성능 SLA는 MVP 범위 밖이다.
- Frontend 자동 테스트와 Backend controller/security/DB integration test는 작성하지 않는다.
- 모바일·태블릿, Chrome 외 브라우저와 접근성 인증 등급은 범위 밖이다.

## 미확정 항목

없음.

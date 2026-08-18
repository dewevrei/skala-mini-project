# REST API Specification

문서 상태: 구현 계약 확정본  
Base URL: `http://localhost:8080/api/v1`  
Content-Type: `application/json`  
인증: Google OAuth 2.0 + 서버 세션 쿠키

세션 저장소: Spring Session Data Redis + Redis Open Source 8.8

## 공통 규칙

### JSON 응답 봉투

OAuth 로그인 리다이렉트·콜백을 제외한 모든 JSON API는 다음 구조를 사용한다.

```java
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data
) {
    public static <T> ApiResponse<T> success(ApiCode code) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), null);
    }

    public static <T> ApiResponse<T> success(ApiCode code, T data) {
        return new ApiResponse<>(true, code.getCode(), code.getMessage(), data);
    }

    public static <T> ApiResponse<T> error(ApiCode code) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), null);
    }
}
```

- 성공 응답: `success=true`
- 실패 응답: `success=false`, `data=null`
- HTTP 상태 코드는 결과 의미에 맞게 사용한다.
- 날짜·시간은 ISO-8601 문자열로 반환한다.
- 클라이언트 요청에 `userId`를 받지 않는다. 로그인 세션에서 사용자를 결정한다.
- 생성 API는 `201 Created`, 조회·수정·삭제는 body가 있으므로 `200 OK`를 사용한다.
- `code`는 접두사 없는 대문자 `UPPER_SNAKE_CASE`이며 이 문서와 오류 명세에 정의된 문자열을 안정적으로 유지한다.

### 대표 리소스 형태

```json
{
  "user": {
    "id": 1,
    "name": "Google Name",
    "email": "user@example.com",
    "nickname": "Google Name-550e8400-e29b-41d4-a716-446655440000",
    "createdAt": "2026-08-18T16:00:00+09:00",
    "updatedAt": "2026-08-18T16:00:00+09:00"
  },
  "project": {
    "id": 10,
    "name": "개인 프로젝트",
    "description": "선택 설명",
    "createdAt": "2026-08-18T16:00:00+09:00",
    "updatedAt": "2026-08-18T16:00:00+09:00"
  },
  "column": {
    "id": 100,
    "projectId": 10,
    "name": "Todo",
    "sortOrder": 1,
    "taskCount": 2
  },
  "task": {
    "id": 1000,
    "projectId": 10,
    "columnId": 100,
    "title": "회원 닉네임 변경",
    "description": "회원 변경 기능 - 회원 닉네임 변경 작업",
    "priority": 2,
    "sortOrder": 1,
    "createdAt": "2026-08-18T16:00:00+09:00",
    "updatedAt": "2026-08-18T16:00:00+09:00"
  }
}
```

## 인증·사용자 API

| Method | Path | 인증 | 요청 | 성공 |
|---|---|---|---|---|
| GET | `/oauth2/authorization/google` | 불필요 | 없음 | Google 인증 화면으로 리다이렉트; JSON 봉투 예외 |
| GET | `/login/oauth2/code/google` | Google callback | OAuth 파라미터 | 로그인 완료 후 Frontend로 리다이렉트; JSON 봉투 예외 |
| GET | `/api/v1/auth/csrf` | 선택 | 없음 | `200 CSRF_TOKEN_ISSUED`, CSRF 토큰 정보 |
| POST | `/api/v1/auth/logout` | 필요 | CSRF 헤더 | `200 LOGOUT_SUCCEEDED`, 세션 종료 |
| GET | `/api/v1/users/me` | 필요 | 없음 | `200 USER_READ`, 현재 사용자 |
| PATCH | `/api/v1/users/me/nickname` | 필요 | `{"nickname":"새 닉네임"}` | `200 USER_UPDATED`, 변경된 사용자 |

리다이렉트 계약:

- 로그인 성공: Frontend `/projects`
- OAuth 실패: Frontend `/login?error=oauth`
- 로그아웃 성공: 응답 처리 후 Frontend `/login`
- OAuth callback에서 Redis 세션 저장 불가: Frontend `/login?error=session-service-unavailable`

CSRF 응답 `data`:

```json
{
  "token": "opaque-token",
  "headerName": "X-CSRF-TOKEN"
}
```

## Project API

| Method | Path | 요청 | 성공 코드 | 동작 |
|---|---|---|---|---|
| GET | `/projects` | 없음 | `200 PROJECT_LISTED` | 로그인 사용자의 프로젝트 목록 |
| POST | `/projects` | `CreateProjectRequest` | `201 PROJECT_CREATED` | Project와 기본 열 3개 원자적 생성 |
| GET | `/projects/{projectId}` | 없음 | `200 PROJECT_READ` | 소유 Project 상세 |
| PATCH | `/projects/{projectId}` | `UpdateProjectRequest` | `200 PROJECT_UPDATED` | 이름·설명 수정 |
| DELETE | `/projects/{projectId}` | 없음 | `200 PROJECT_DELETED` | Project·열·Task 완전 삭제 |

```json
// CreateProjectRequest / UpdateProjectRequest
{
  "name": "프로젝트 이름",
  "description": "선택 설명 또는 null"
}
```

Project 생성 응답 `data`는 Project와 기본 열을 함께 반환한다.

Project 목록은 `createdAt DESC, id DESC`의 최근 생성순으로 반환한다.

```json
{
  "project": { "id": 10, "name": "프로젝트 이름", "description": null },
  "columns": [
    { "id": 100, "name": "Todo", "sortOrder": 1 },
    { "id": 101, "name": "In Progress", "sortOrder": 2 },
    { "id": 102, "name": "Done", "sortOrder": 3 }
  ]
}
```

## BoardColumn API

| Method | Path | 요청 | 성공 코드 | 동작 |
|---|---|---|---|---|
| POST | `/projects/{projectId}/columns` | `{"name":"Review"}` | `201 COLUMN_CREATED` | 사용자 열 생성 |
| PATCH | `/projects/{projectId}/columns/{columnId}` | `{"name":"검토"}` | `200 COLUMN_UPDATED` | 열 이름 변경 |
| PUT | `/projects/{projectId}/columns/order` | `ReorderColumnsRequest` | `200 COLUMNS_REORDERED` | 전체 열 순서 저장 |
| DELETE | `/projects/{projectId}/columns/{columnId}` | 없음 | `200 COLUMN_DELETED` | 열과 포함 Task 완전 삭제 |

```json
// ReorderColumnsRequest
{
  "orderedColumnIds": [101, 102, 100]
}
```

- `orderedColumnIds`는 Project의 현재 모든 열 ID를 중복·누락 없이 한 번씩 포함해야 한다.
- 새 Column은 현재 마지막 Column 뒤에 추가한다.
- 순서 변경 직후 배열 첫 번째 열이 AI 등록 대상이 된다.
- 마지막 열 삭제 요청은 `409 LAST_COLUMN_DELETE_FORBIDDEN`이다.

## 조회 API

| Method | Path | Query | 성공 코드 | 응답 |
|---|---|---|---|---|
| GET | `/projects/{projectId}/board` | 없음 | `200 BOARD_READ` | 열과 카드의 저장 순서 |
| GET | `/projects/{projectId}/items` | `title` 선택 | `200 ITEMS_READ` | Board와 같은 열 그룹·카드 순서; title 검색 |

두 응답의 `data`는 같은 핵심 구조를 사용한다.

```json
{
  "project": { "id": 10, "name": "개인 프로젝트", "description": null },
  "columnGroups": [
    {
      "column": { "id": 100, "name": "Todo", "sortOrder": 1 },
      "tasks": [
        {
          "id": 1000,
          "projectId": 10,
          "columnId": 100,
          "title": "작업",
          "description": null,
          "priority": 1,
          "sortOrder": 1
        }
      ]
    }
  ]
}
```

- Items `title` 검색은 trim 후 포함 검색이며 대소문자를 구분하지 않는다.
- 일치 Task가 없는 열 그룹도 표시해 그 열의 `Add item` 문맥을 유지한다.
- 페이지 나누기는 MVP에서 사용하지 않는다.

## Task API

| Method | Path | 요청 | 성공 코드 | 동작 |
|---|---|---|---|---|
| POST | `/projects/{projectId}/columns/{columnId}/tasks` | `CreateTaskRequest` | `201 TASK_CREATED` | 일반 Task를 열 맨 아래 생성 |
| POST | `/projects/{projectId}/tasks/ai-generate` | `GenerateAiTasksRequest` | `201 TASKS_CREATED` | AI Task 목록 또는 fallback Task 생성 |
| GET | `/projects/{projectId}/tasks/{taskId}` | 없음 | `200 TASK_READ` | Task 상세 |
| PATCH | `/projects/{projectId}/tasks/{taskId}` | `UpdateTaskRequest` | `200 TASK_UPDATED` | title·description 수정 |
| DELETE | `/projects/{projectId}/tasks/{taskId}` | 없음 | `200 TASK_DELETED` | Task 완전 삭제 |
| PATCH | `/projects/{projectId}/tasks/{taskId}/status` | `ChangeTaskStatusRequest` | `200 TASK_MOVED` | Items 상태 변경; 대상 열 맨 아래 |
| PATCH | `/projects/{projectId}/tasks/{taskId}/position` | `MoveTaskRequest` | `200 TASK_MOVED` | Board 드래그 위치 저장 |

```json
// CreateTaskRequest
{
  "title": "일반 작업",
  "description": "선택 설명"
}

// GenerateAiTasksRequest
{
  "title": "회원 변경 기능",
  "description": "닉네임과 비밀번호를 각각 변경할 수 있어야 한다."
}

// UpdateTaskRequest
{
  "title": "수정된 제목",
  "description": "수정된 설명 또는 null"
}

// ChangeTaskStatusRequest
{
  "targetColumnId": 101
}

// MoveTaskRequest
{
  "targetColumnId": 101,
  "beforeTaskId": 2005
}
```

`MoveTaskRequest` 규칙:

- `beforeTaskId`가 있으면 대상 Task 바로 앞에 배치한다.
- `beforeTaskId=null`이면 대상 열 맨 아래에 배치한다.
- 같은 열 재정렬과 다른 열 이동 모두 지원한다.
- `beforeTaskId`는 `targetColumnId`에 속해야 한다.
- 이동 후 관련 열의 `sortOrder`를 일관되게 다시 계산한다.

AI 생성 응답은 성공·fallback 모두 같은 형태다. fallback 여부를 별도 필드로 노출하지 않는다.

```json
{
  "success": true,
  "code": "TASKS_CREATED",
  "message": "작업이 등록되었습니다.",
  "data": {
    "tasks": [
      {
        "id": 1001,
        "title": "회원 닉네임 변경",
        "description": "회원 변경 기능 - 회원 닉네임 변경 작업",
        "priority": 2,
        "columnId": 100,
        "sortOrder": 3
      }
    ]
  }
}
```

## 요청 중복과 동시 수정

- 서버는 `Idempotency-Key`를 받거나 저장하지 않는다.
- 같은 POST가 여러 번 도착하면 요청 횟수만큼 생성될 수 있다.
- 프런트엔드는 요청 중 생성 버튼을 비활성화한다.
- 별도 버전 필드나 `If-Match`를 사용하지 않는다.
- 마지막으로 완료된 변경을 최종 상태로 사용한다.

## Frontend 전용 등록 취소

- Task 등록 화면의 `Cancel`은 API를 호출하지 않는다.
- 입력 변경 여부와 관계없이 확인창 없이 입력을 폐기한다.
- Board에서 시작했으면 같은 Project의 Board로, Items에서 시작했으면 같은 Project의 Items로 돌아간다.
- 일반 Create와 AI Generate에 공통으로 적용한다.

## Frontend route 계약

| 화면 | route |
|---|---|
| 로그인 | `/login` |
| Project 목록 | `/projects` |
| Board | `/projects/:projectId/board` |
| Items | `/projects/:projectId/items` |
| 회원정보 변경 | `/profile` |

Task 등록은 별도 route가 아니라 시작한 Board/Items 위의 공통 modal 또는 overlay로 제공한다. 따라서 `Cancel`은 route history를 추측하지 않고 modal을 닫아 현재 시작 화면을 그대로 보여준다.

## 세션 저장소 장애

- Redis 연결 실패 중 메모리 세션으로 전환하지 않는다.
- 세션 확인·생성이 필요한 JSON API는 `503 SESSION_SERVICE_UNAVAILABLE`을 반환한다.
- `/api/v1/auth/csrf`도 세션 생성이 필요하면 같은 503을 반환한다.
- `/api/v1/auth/logout` 처리 중 Redis가 실패하면 쿠키는 즉시 만료시키되 응답은 성공으로 가장하지 않고 같은 503을 반환한다.
- `/oauth2/authorization/google` 또는 OAuth callback에서 세션을 만들 수 없으면 `/login?error=session-service-unavailable`로 리다이렉트한다.
- Frontend는 `message`를 표시하고 로그인 화면에서 재시도를 안내한다.
- Redis가 복구되면 새 요청과 새 로그인을 허용한다. Redis 재시작으로 사라진 세션은 복원하지 않는다.

## 성공 응답 data 형태

| 작업 | `data` |
|---|---|
| CSRF | `{token, headerName}` |
| 로그아웃·삭제 | `null` |
| 내 정보·닉네임 | `{user}` |
| Project 목록 | `{projects}` |
| Project 생성 | `{project, columns}` |
| Project 상세·수정 | `{project}` |
| Column 생성·수정 | `{column}` |
| Column 순서 변경 | `{columns}` |
| Board·Items | `{project, columnGroups:[{column,tasks}]}` |
| Task 생성·상세·수정 | `{task}` |
| AI 생성·fallback | `{tasks}` |
| Task 위치·상태 이동 | `{task, affectedColumnGroups:[{column,tasks}]}` |

이동 응답은 source/target group의 최신 전체 카드 순서를 반환한다. 같은 열 재정렬은 한 group만 반환한다. 삭제 응답은 data가 null이다.

정확한 성공 `code`와 `message`는 `docs/error-response-specification.md`의 성공 ApiCode 계약을 사용한다.

## 미확정 항목

없음.

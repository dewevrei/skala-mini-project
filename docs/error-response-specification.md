# Error Response Specification

문서 상태: 구현 계약 확정본

## 공통 형태

```json
{
  "success": false,
  "code": "INVALID_TASK_TITLE",
  "message": "작업 제목을 입력해 주세요.",
  "data": null
}
```

규칙:

- 오류에서도 HTTP 상태 코드를 의미에 맞게 사용한다.
- `success`는 항상 `false`다.
- `code`는 프런트엔드 분기용 안정적인 식별자다.
- `message`는 사용자에게 표시 가능한 한국어 기본 문구다.
- 실패 응답의 `data`는 항상 `null`이다.
- 필드별 오류 배열은 제공하지 않는다.
- 예상하지 못한 예외의 내부 클래스명, SQL, stack trace, 외부 API key를 노출하지 않는다.

## HTTP 상태 사용

| 상태 | 의미 | 대표 상황 |
|---:|---|---|
| 400 | 요청 형식·검증 오류 | 필수값 누락, 잘못된 정렬 배열, 잘못된 이동 대상 |
| 401 | 로그인 필요 | 세션 없음·만료 |
| 403 | 인증은 됐지만 보안 검증 실패 | CSRF 오류 |
| 404 | 소유 범위에서 자원 없음 | 없는 자원 또는 타인 자원 |
| 409 | 현재 데이터 규칙과 충돌 | 이름 중복, 마지막 열 삭제 |
| 500 | 내부 처리 실패 | DB 저장 실패, 예기치 않은 서버 오류 |
| 502 | 외부 서비스 실패 | MVP AI 생성에서는 fallback으로 흡수하므로 일반적으로 직접 반환하지 않음 |
| 503 | 필수 세션 저장소 사용 불가 | Redis 연결 실패 또는 timeout |

## ApiCode 계약

### 인증·사용자

| HTTP | code | message |
|---:|---|---|
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

### Project

| HTTP | code | message |
|---:|---|---|
| 400 | `INVALID_PROJECT_NAME` | 프로젝트 이름을 확인해 주세요. |
| 400 | `INVALID_PROJECT_DESCRIPTION` | 프로젝트 설명을 확인해 주세요. |
| 409 | `DUPLICATE_PROJECT_NAME` | 같은 이름의 프로젝트가 이미 있습니다. |
| 404 | `PROJECT_NOT_FOUND` | 프로젝트를 찾을 수 없습니다. |
| 500 | `PROJECT_CREATE_FAILED` | 프로젝트를 생성하지 못했습니다. |
| 500 | `PROJECT_DELETE_FAILED` | 프로젝트를 삭제하지 못했습니다. |

### BoardColumn

| HTTP | code | message |
|---:|---|---|
| 400 | `INVALID_COLUMN_NAME` | 열 이름을 확인해 주세요. |
| 409 | `DUPLICATE_COLUMN_NAME` | 같은 이름의 열이 이미 있습니다. |
| 404 | `COLUMN_NOT_FOUND` | 보드 열을 찾을 수 없습니다. |
| 400 | `INVALID_COLUMN_ORDER` | 보드 열 순서가 올바르지 않습니다. |
| 409 | `LAST_COLUMN_DELETE_FORBIDDEN` | 프로젝트의 마지막 열은 삭제할 수 없습니다. |
| 500 | `COLUMN_DELETE_FAILED` | 보드 열을 삭제하지 못했습니다. |

### Task

| HTTP | code | message |
|---:|---|---|
| 400 | `INVALID_TASK_TITLE` | 작업 제목을 입력해 주세요. |
| 400 | `INVALID_TASK_DESCRIPTION` | 작업 설명을 확인해 주세요. |
| 400 | `INVALID_TASK_PRIORITY` | 작업 우선순위가 올바르지 않습니다. |
| 400 | `INVALID_AI_DESCRIPTION` | AI 생성을 위한 설명을 입력해 주세요. |
| 400 | `READ_ONLY_FIELD` | 변경할 수 없는 항목이 포함되어 있습니다. |
| 404 | `TASK_NOT_FOUND` | 작업을 찾을 수 없습니다. |
| 400 | `INVALID_TASK_MOVE` | 작업을 이동할 위치가 올바르지 않습니다. |
| 400 | `INVALID_SEARCH_QUERY` | 검색어를 확인해 주세요. |
| 500 | `TASK_BATCH_SAVE_FAILED` | 생성된 작업을 저장하지 못했습니다. |
| 500 | `TASK_FALLBACK_SAVE_FAILED` | 작업을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요. |
| 500 | `TASK_DELETE_FAILED` | 작업을 삭제하지 못했습니다. |

### 공통

| HTTP | code | message |
|---:|---|---|
| 400 | `INVALID_REQUEST` | 요청 내용을 확인해 주세요. |
| 400 | `MALFORMED_JSON` | 요청 형식이 올바르지 않습니다. |
| 404 | `RESOURCE_NOT_FOUND` | 요청한 대상을 찾을 수 없습니다. |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요. |

`SESSION_SERVICE_UNAVAILABLE`은 세션 없음·만료를 뜻하는 `401 AUTHENTICATION_REQUIRED`와 구분한다. Redis 장애 중 메모리 세션으로 전환하지 않는다. OAuth callback처럼 JSON 봉투를 사용할 수 없는 흐름은 `/login?error=session-service-unavailable`로 리다이렉트한다.

## AI 실패의 특례

- Gemini 호출 실패, 응답 형식 실패, 일부 항목 검증 실패는 오류 응답 대신 fallback Task 저장으로 처리한다.
- fallback 저장이 성공하면 `201 TASKS_CREATED` 성공을 반환하고 AI 실패를 알리는 별도 code나 field를 주지 않는다.
- 유효 AI 결과의 DB 일괄 저장 실패만 `500 TASK_BATCH_SAVE_FAILED`다.
- fallback Task 자체의 DB 저장이 실패하면 `500 TASK_FALLBACK_SAVE_FAILED`를 반환하며 작업은 남기지 않는다.

## 중복 DB 오류 매핑

DB 유일키 충돌은 SQL 메시지를 노출하지 않고 문맥별 `409 DUPLICATE_*`로 변환한다. 애플리케이션 사전 검증과 DB 최종 제약이 동일한 code를 사용해야 한다.

## 성공 ApiCode 계약

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
| 200 | `TASK_DELETED` | 작업이 삭제되었습니다. |
| 200 | `TASK_MOVED` | 작업이 이동되었습니다. |

## 로그 상관관계

응답 봉투에 trace ID 필드를 추가하지 않는다. 서버 로그 내부의 요청 상관관계 식별은 허용하되 사용자 응답 구조 네 필드를 변경하지 않는다.

## 미확정 항목

없음.

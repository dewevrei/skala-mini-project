# Validation Rules

문서 상태: 구현 계약 확정본  
원칙: 클라이언트 검증은 편의 기능이며 서버 검증이 최종 기준이다.

## 공통 문자열 규칙

- 입력 앞뒤 공백을 제거한 값을 검증·저장한다.
- 필수 문자열은 trim 후 비어 있으면 안 된다.
- NUL과 제어 문자를 거부한다. 일반적인 Unicode·한글·공백은 허용한다.
- 길이는 Unicode 문자 수 기준 애플리케이션 검증을 사용한다.
- 이름 유일성 및 title 검색은 대소문자를 구분하지 않는다.
- 아래 최대 길이는 MVP의 고정 검증값이다. 변경하려면 DDL·API·Frontend 검증 계약을 함께 변경해야 한다.

## 필드 규칙

| 대상 | 필드 | 필수 | 기본 길이 | 규칙 | 오류 코드 |
|---|---|---|---|---|---|
| User | googleId | 예 | 1~255 | Google claim에서만 설정; 클라이언트 입력 금지; 유일 | `OAUTH_PROFILE_INVALID` / `DUPLICATE_GOOGLE_ID` |
| User | name | 예 | 1~255 | 로그인마다 Google 값으로 갱신 | `OAUTH_PROFILE_INVALID` |
| User | email | 예 | 1~320 | 검증된 Google 이메일; 대소문자 무시 유일 | `OAUTH_EMAIL_INVALID` / `DUPLICATE_EMAIL` |
| User | nickname(자동) | 예 | 최대 255 | Google 이름을 길이에 맞게 줄인 뒤 `-`와 전체 UUID 결합 | `NICKNAME_GENERATION_FAILED` |
| User | nickname(수정) | 예 | 1~100 | trim; 대소문자 무시 유일 | `INVALID_NICKNAME` / `DUPLICATE_NICKNAME` |
| Project | name | 예 | 1~100 | 소유자 범위 대소문자 무시 유일 | `INVALID_PROJECT_NAME` / `DUPLICATE_PROJECT_NAME` |
| Project | description | 아니오 | 0~2000 | null 또는 빈 값 허용 | `INVALID_PROJECT_DESCRIPTION` |
| BoardColumn | name | 예 | 1~50 | Project 범위 대소문자 무시 유일 | `INVALID_COLUMN_NAME` / `DUPLICATE_COLUMN_NAME` |
| Task | title | 예 | 1~200 | 일반·AI·fallback 모두 동일 | `INVALID_TASK_TITLE` |
| Task | description(일반) | 아니오 | 0~5000 | plain text, null 허용 | `INVALID_TASK_DESCRIPTION` |
| Task | startDate | 아니오 | ISO `YYYY-MM-DD` | 유효한 달력 날짜 또는 null; endDate와 독립 | `INVALID_TASK_DATE` |
| Task | endDate | 아니오 | ISO `YYYY-MM-DD` | 유효한 달력 날짜 또는 null; startDate와 선후관계 검사 없음 | `INVALID_TASK_DATE` |
| AI 입력 | description | 예 | 1~5000 | Gemini 문맥으로만 사용 | `INVALID_AI_DESCRIPTION` |
| Task | priority | 예 | 1~5 | 일반/fallback=1; AI=모델 결과; 수정 API 입력 금지 | `INVALID_TASK_PRIORITY` |
| Search | title | 아니오 | 0~200 | 빈 값은 전체 조회; 포함 검색 | `INVALID_SEARCH_QUERY` |

## 유일성 규칙

| 규칙 | 비교 범위 | 비교 방식 | 충돌 결과 |
|---|---|---|---|
| `googleId` | 전체 User | 정확한 값 | `409 DUPLICATE_GOOGLE_ID` |
| `email` | 전체 User | 대소문자 무시 | `409 DUPLICATE_EMAIL` |
| `nickname` | 전체 User | 대소문자 무시 | `409 DUPLICATE_NICKNAME` |
| Project name | 한 User | trim + 대소문자 무시 | `409 DUPLICATE_PROJECT_NAME` |
| BoardColumn name | 한 Project | trim + 대소문자 무시 | `409 DUPLICATE_COLUMN_NAME` |

자기 자신을 수정하면서 이름이 실질적으로 바뀌지 않은 경우는 중복으로 보지 않는다.

## Project 규칙

1. 생성 시 기본 이름 `Todo`, `In Progress`, `Done`을 순서대로 만든다.
2. Project와 기본 열은 하나의 트랜잭션에서 생성한다.
3. Project 삭제는 인증된 소유자 요청에만 허용한다.
4. Project 이름 중복은 DB 유일키 오류도 동일한 `409`로 변환한다.

## BoardColumn 규칙

### 추가·수정

- 대상 Project는 로그인 사용자 소유여야 한다.
- 열 이름은 Project 안에서 유일해야 한다.
- 새 열은 현재 가장 큰 `sortOrder` 뒤에 추가하고 저장 후 전체 열 순서는 `sortOrder, id`로 결정한다.

### 전체 순서 변경

- 배열은 null이 아니고 비어 있지 않아야 한다.
- 현재 Project의 모든 열 ID를 정확히 한 번씩 포함해야 한다.
- 다른 Project 열, 누락, 중복, 알 수 없는 ID가 있으면 `400 INVALID_COLUMN_ORDER`다.
- 성공 후 배열 첫 번째가 AI 등록 대상이다.

### 삭제

- Project에 열이 두 개 이상일 때만 삭제 가능하다.
- 마지막 열 삭제는 `409 LAST_COLUMN_DELETE_FORBIDDEN`이다.
- 삭제 확인 모달은 Frontend 책임이고 서버는 확인 플래그를 별도로 신뢰하지 않는다.
- 삭제 시 포함 Task를 DB cascade로 완전 삭제한다.
- 동시에 여러 Column 삭제가 들어와도 최소 한 열을 보장하기 위해 Project 행을 삭제 트랜잭션 동안 write-lock하고 잠금 획득 후 열 수를 다시 검사한다.

## 일반 Task 생성 규칙

- `projectId`, `columnId`는 경로에서 받는다.
- BoardColumn은 Project에 속하고 Project는 로그인 사용자 소유여야 한다.
- `title` 필수, `description` 선택이다.
- 클라이언트가 priority를 보내더라도 무시하지 않고 `400 READ_ONLY_FIELD`로 거부한다.
- 서버가 priority `1`과 해당 열의 다음 `sortOrder`를 설정한다.
- 서버가 `startDate`, `endDate`를 모두 `null`로 설정하며 생성 요청에서 날짜 입력은 받지 않는다.

## AI Task 생성 규칙

### 입력

- `title`, `description` 모두 필수다.
- 현재 Project 첫 번째 BoardColumn이 존재해야 한다. Project 최소 열 규칙상 정상 상태에서는 항상 존재한다.

### 모델 응답 전체 검증

- 최상위 `tasks` 배열이 존재하고 비어 있지 않아야 한다.
- Task 수의 별도 최소·최대 비즈니스 제한은 두지 않는다.
- 모든 항목이 `title`, `description`, `priority`를 가져야 한다.
- 각 title·description은 trim 후 비어 있지 않아야 한다.
- priority는 소수나 문자열이 아닌 정수 `1~5`여야 한다.
- 하나라도 실패하면 전체 응답을 무효로 한다.
- 최초 구조 검증 실패 시 동일 입력으로 한 번만 재요청한다.

### 저장 전 변환

- 저장 description = `trim(originalTitle) + " - " + trim(aiDescription)`.
- 최종 저장 description이 5,000자를 넘으면 응답 검증 실패로 취급한다. 따라서 AI description 허용 길이는 `5000 - trim(originalTitle) 길이 - 구분자 길이`로 동적으로 계산한다.
- 응답 배열 순서를 유지해 첫 번째 열 맨 아래에 연속 `sortOrder`를 부여한다.
- 정상 AI 결과와 fallback Task의 `startDate`, `endDate`는 모두 `null`이다.

### 실패 분기

| 실패 위치 | 처리 |
|---|---|
| Gemini 연결·timeout·안전 필터 등 호출 실패 | 원본으로 fallback 일반 Task 1건 저장 |
| 최초 응답 구조 실패 | 동일 입력 1회 재요청 |
| 재요청 응답 구조 실패 | fallback 일반 Task 1건 저장 |
| 일부 항목만 유효 | 전체 응답 실패로 처리 후 위 규칙 적용 |
| 유효 응답 DB 저장 실패 | 전체 rollback, `500 TASK_BATCH_SAVE_FAILED`; fallback 금지 |

## Task 수정·이동·삭제 규칙

- 수정 API는 `title`, `description`만 허용한다.
- 날짜 수정 전용 API는 `startDate`, `endDate` 두 key를 모두 받으며 각 값은 유효한 ISO 달력 날짜 또는 `null`이어야 한다.
- 두 날짜는 서로 독립적이며 `endDate < startDate`도 거부하지 않는다.
- 일반 수정 API에서 `priority`, `projectId`, `columnId`, `sortOrder`, `startDate`, `endDate`, timestamp 수정 입력은 `400 READ_ONLY_FIELD`다.
- 상태 변경 대상 열은 같은 Project여야 하며 대상 열 맨 아래로 이동한다.
- 보드 위치 이동의 `beforeTaskId`는 대상 열에 있어야 한다.
- Task 삭제는 소유자만 가능하며 완전 삭제한다.

## 검색 규칙

- 현재 Project의 Task title만 검색한다.
- trim한 검색어로 대소문자를 구분하지 않는 부분 포함 검색을 한다.
- 검색어가 null 또는 빈 문자열이면 전체 Task를 반환한다.
- 결과는 BoardColumn 그룹과 저장된 카드 순서를 유지한다.
- 일치 항목이 없는 열 그룹도 유지한다.

## Frontend 입력 상태

- 필수값이 없으면 API 요청을 보내지 않고 메시지를 표시한다.
- 하나의 Task 등록 modal 하단에 `Cancel`, `Create`, `AI Generate`를 인접 배치하며 별도 AI 화면이나 모드 전환을 만들지 않는다.
- `Create`는 title만 필수이고 description은 선택이다. `AI Generate`는 같은 입력의 title과 description이 모두 필수다.
- 생성 요청 중 `Create`와 `AI Generate` 버튼을 모두 비활성화하고 요청 중인 버튼에 진행 상태를 표시한다.
- AI fallback은 정상 생성 응답처럼 처리하고 별도 경고를 표시하지 않는다.
- 서버 실패 응답은 `message`를 사용자에게 표시한다.
- Task 등록 `Cancel`은 dirty 여부를 검사하거나 확인창을 띄우지 않고 입력을 폐기한 뒤 시작한 Board/Items로 돌아간다.

## 세션 규칙

- Redis 세션의 최대 미사용 시간은 24시간이다.
- `SESSION` 쿠키의 최대 보관 기간은 7일이고 `HttpOnly`, `Path=/`, `SameSite=Lax`를 사용한다.
- 로컬 HTTP에서는 `Secure=false`이며 운영 HTTPS 설정은 MVP 범위 밖이다.
- 로그아웃은 Redis 세션 무효화와 쿠키 `Max-Age=0`을 함께 수행한다.
- Redis 장애를 `401`로 오인하지 않고 `503 SESSION_SERVICE_UNAVAILABLE`로 변환한다.

## 미확정 항목

없음.

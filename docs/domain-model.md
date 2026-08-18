# Domain Model

문서 상태: 구현 계약 확정본  
기준일: 2026-08-18

## 도메인 개요

핵심 모델은 `User → Project → BoardColumn → Task`의 소유·포함 관계다. Task는 계층을 갖지 않으며 일반 등록인지 AI 등록인지와 무관하게 동일한 작업으로 조회·관리된다. AI 생성은 영속 엔티티가 아니라 여러 Task를 만드는 애플리케이션 동작이다.

## 용어

| 용어 | 정의 |
|---|---|
| User | Google OAuth로 인증되어 독립된 개인 데이터를 소유하는 사용자 |
| Project | 한 사용자가 소유하는 작업 관리 단위이자 하나의 칸반보드 |
| BoardColumn | 프로젝트 보드에서 Task의 현재 상태와 표시 순서를 나타내는 열 |
| Task | 사용자가 직접 만들거나 AI가 생성한 독립 실행 작업 |
| Items View | BoardColumn 순서대로 그룹을 표시하는 표 형태의 작업 보기 |
| Board View | BoardColumn과 카드 형태 Task를 표시하는 칸반 보기 |
| AI Generation | 원본 제목·설명을 문맥으로 사용해 평면 Task 목록을 만드는 동작 |
| Login Session | Redis 8.8에 저장되는 인증 상태. RDB 도메인 엔티티가 아니며 불투명 ID만 쿠키로 전달됨 |

## 관계와 카디널리티

- User `1 : N` Project. Project는 정확히 한 User의 소유다.
- Project `1 : N` BoardColumn. Project는 항상 최소 한 개의 BoardColumn을 가진다.
- BoardColumn `1 : N` Task. Task는 정확히 한 BoardColumn에 속한다.
- Task의 Project는 BoardColumn의 Project와 항상 같아야 한다.
- User 간, Project 간 Task 이동은 허용하지 않는다.
- AI 생성 원본은 별도 부모 Task가 아니며 AI Task 간에도 부모·자식 관계가 없다.

## User

### 정체성과 속성

- `id`: 내부 자동 증가 식별자
- `googleId`: Google 계정의 안정적인 외부 식별자, 유일
- `name`: Google이 제공한 최신 이름
- `email`: Google이 제공한 최신 이메일, 유일
- `nickname`: 서비스 표시 이름, 유일(대소문자 무시)
- `createdAt`, `updatedAt`

### 생명주기

- 최초 Google 로그인 성공 시 생성된다.
- 재로그인 시 `name`, `email`, `updatedAt`이 갱신된다.
- 최초 `nickname`은 이름과 UUID를 결합해 충돌 없이 만든다.
- 사용자는 닉네임만 직접 변경할 수 있다.
- MVP에서는 회원 탈퇴나 User 삭제가 없다.

### 금지 규칙

- 다른 User의 Project, BoardColumn, Task를 조회·수정·삭제할 수 없다.
- `googleId`, `email`, `nickname` 중복을 허용하지 않는다.

## Project

### 정체성과 속성

- `id`: 내부 자동 증가 식별자
- `owner`: 소유 User
- `name`: 소유자 범위에서 유일한 필수 이름(대소문자 무시)
- `description`: 선택 설명
- `createdAt`, `updatedAt`

### 생명주기

- 생성 시 `Todo`, `In Progress`, `Done` BoardColumn을 순서대로 함께 만든다.
- 목록에서는 `createdAt DESC, id DESC`의 최근 생성순으로 표시한다.
- 이름과 설명을 수정할 수 있다.
- 사용자가 경고 모달에서 확인하면 Project와 모든 하위 데이터를 완전 삭제한다.

### 금지 규칙

- 하위 BoardColumn이 하나도 없는 상태로 존재할 수 없다.
- 소유자가 아닌 사용자는 접근할 수 없다.

## BoardColumn

### 정체성과 속성

- `id`: 내부 자동 증가 식별자
- `project`: 소속 Project
- `name`: Project 범위에서 유일한 이름(대소문자 무시)
- `sortOrder`: 보드와 Items 그룹의 표시 순서
- `createdAt`, `updatedAt`

### 의미

- Task의 상태는 별도 고정 값이 아니라 현재 BoardColumn이다.
- 현재 `sortOrder`가 가장 앞선 BoardColumn이 AI 작업의 등록 열이다.
- 별도 `default` 플래그나 고정된 `Todo` 상태는 없다.

### 생명주기

- Project 생성 시 기본 세 개가 만들어지거나 사용자가 추가한다.
- 사용자가 추가한 열은 현재 마지막 열 뒤에 생성된다.
- 기본 열과 사용자 열 모두 이름·순서를 바꿀 수 있다.
- 삭제 확인 시 포함 Task와 함께 완전 삭제된다.
- 마지막 남은 열은 삭제할 수 없다.

### 금지 규칙

- 같은 Project에 대소문자만 다른 중복 이름을 가질 수 없다.
- 다른 Project의 Task를 받을 수 없다.

## Task

### 정체성과 속성

- `id`: 내부 자동 증가 식별자
- `project`: 조회·소유권 경계를 위한 Project
- `column`: 현재 상태 BoardColumn
- `title`: 필수 제목
- `description`: 선택 설명. AI Task는 생성 시 원본 제목 접두부를 포함한다.
- `startDate`: 선택 시작일. 날짜만 저장하며 `null`일 수 있다.
- `endDate`: 선택 종료일. 날짜만 저장하며 `null`일 수 있다.
- `priority`: `1~5` 정수, `1`이 가장 높음
- `sortOrder`: 소속 열 안 카드 순서
- `createdAt`, `updatedAt`

### 생성 방식

#### 일반 Task

- BoardColumn 문맥이 있는 Board `+` 또는 Items `Add item`에서만 생성한다.
- 선택한 BoardColumn 맨 아래에 추가한다.
- `title`은 필수, `description`은 선택, `priority`는 항상 `1`이다.
- `startDate`, `endDate`는 모두 `null`로 생성한다.

#### AI Task

- 원본 `title`, `description`은 모두 필수다.
- AI 생성 성공 시 원본 입력은 부모 또는 별도 Task로 저장하지 않는다. AI 호출·검증 실패 때만 원본 기반 fallback Task 한 건을 저장한다.
- Gemini가 반환한 각 항목을 독립 Task로 저장한다.
- 모든 결과를 현재 첫 번째 BoardColumn 맨 아래에 응답 순서대로 추가한다.
- 저장 `description`은 정확히 `원본 title + " - " + AI description`이다.
- AI 결과와 fallback Task 모두 `startDate`, `endDate`를 `null`로 생성한다.

### 생명주기

- 제목과 설명은 수정 가능하다.
- Items에서 시작일과 종료일을 각각 지정하거나 다시 비울 수 있다.
- 시작일과 종료일의 선후관계는 제한하지 않으며 종료일이 시작일보다 앞서도 허용한다.
- 우선순위는 생성 이후 수정 불가다.
- 보드 드래그로 같은 열 안 순서를 바꾸거나 다른 열의 특정 위치로 이동할 수 있다.
- Items의 상태 선택으로 이동하면 대상 열 맨 아래로 간다.
- 삭제 확인 후 완전 삭제된다.

### 금지 규칙

- `priority`는 `1~5` 범위를 벗어날 수 없다.
- 다른 Project의 BoardColumn으로 이동할 수 없다.
- Task는 계층이나 하위 Task 컬렉션을 갖지 않는다.

## AI Generation 동작

### 입력

- 로그인 사용자 소유 Project
- 필수 원본 `title`
- 필수 원본 `description`

### 정상 결과

- 비어 있지 않은 Task 목록
- 각 항목: 비어 있지 않은 `title`, 비어 있지 않은 `description`, `1~5` 정수 `priority`
- 작업 수 비즈니스 제한 없음

### 실패와 종료

- 구조가 잘못되면 동일 입력으로 한 번 재요청한다.
- 호출 실패 또는 최종 검증 실패 시 원본 입력으로 일반 Task 하나를 첫 열에 저장한다.
- 유효 AI 목록 DB 저장 중 오류가 발생하면 전체 롤백하고 오류로 종료한다.
- AI 성공과 대체 저장 모두 원본 AI 요청 자체는 영속화하지 않는다.

## 주요 불변 규칙

1. Project는 정확히 한 User가 소유한다.
2. Project에는 항상 최소 한 BoardColumn이 있다.
3. Task의 Project와 BoardColumn의 Project는 같다.
4. 한 Project 안의 BoardColumn 이름은 대소문자를 무시하고 유일하다.
5. 한 User 안의 Project 이름은 대소문자를 무시하고 유일하다.
6. Task priority는 생성 후 변경되지 않는다.
7. AI 다중 Task 저장은 전부 성공하거나 전부 롤백한다.
8. 열 삭제는 그 열의 Task를 함께 완전 삭제한다.
9. 첫 번째 BoardColumn은 언제나 AI 등록 대상이다.
10. 보드와 Items는 같은 `sortOrder`를 공유한다.
11. Task의 `startDate`와 `endDate`는 서로 독립적으로 `null`일 수 있으며 두 날짜의 순서를 강제하지 않는다.

## 인증 세션 기술 모델

- Spring Session Data Redis와 Redis Open Source 8.8을 사용한다.
- Redis 세션은 마지막 요청 후 24시간 동안 활동이 없으면 만료된다.
- 브라우저의 `SESSION` 쿠키는 최대 7일간 유지되며 사용자 정보나 OAuth token을 포함하지 않는다.
- Backend 재시작은 세션을 없애지 않지만 Redis 재시작은 기존 세션을 없앨 수 있으며 재로그인을 허용한다.
- Redis 장애 중에는 메모리 저장소로 전환하지 않는다.

## 화면 흐름

- 로그인 성공은 `/projects`, OAuth 실패는 `/login?error=oauth`, 로그아웃 성공은 `/login`으로 이동한다.
- Task 등록 `Cancel`은 입력 변경 여부와 관계없이 확인창 없이 입력을 폐기하고 시작한 Board 또는 Items로 돌아간다.
- 공식 화면 범위는 최소 1280px의 최신 Chrome 데스크톱이다. 모바일·태블릿은 MVP 범위 밖이다.

## 미확정 항목

없음.

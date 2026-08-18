# Authorization Rules

문서 상태: 구현 계약 확정본  
인증 모델: Google OAuth 2.0 + Spring Security + Spring Session Data Redis

## 인증 흐름

1. Frontend가 Backend의 `/oauth2/authorization/google`로 이동한다.
2. Google 인증 성공 후 Backend callback이 `google_id`, `name`, `email`을 처리한다.
3. `google_id`로 User를 생성 또는 갱신한다.
4. Backend는 Redis 8.8에 서버 세션을 만들고 `HttpOnly` 세션 쿠키를 발급한다.
5. Frontend의 Axios는 `withCredentials=true`로 REST 요청을 전송한다.
6. 상태 변경 요청은 CSRF 토큰을 지정 헤더로 전송한다.

## 인증 상태별 규칙

| 요청 종류 | 익명 | 로그인 사용자 |
|---|---|---|
| Google 로그인 시작·callback | 허용 | 허용 |
| CSRF 토큰 조회 | 허용 또는 세션 생성 | 허용 |
| API 상태·정적 리소스 | 구현 기본 정책 | 구현 기본 정책 |
| `/api/v1/**` 업무 API | `401 AUTHENTICATION_REQUIRED` | 소유권 검사 후 허용 |
| 로그아웃 | `401` | 허용 + 세션 무효화 |

Redis 연결 실패로 세션을 확인하거나 만들 수 없는 경우에는 인증 없음으로 간주하지 않고 `503 SESSION_SERVICE_UNAVAILABLE`로 처리한다.

## 소유권 규칙

### User

- `/users/me`만 제공한다.
- 요청 경로나 body로 임의 User ID를 받지 않는다.
- 닉네임은 현재 세션 User만 변경할 수 있다.

### Project

- Project의 `user_id`가 현재 User ID와 같을 때만 조회·수정·삭제할 수 있다.
- Project 목록은 현재 User 소유 데이터로 항상 제한한다.
- 타인의 Project ID 요청은 존재 여부를 숨기기 위해 `404 PROJECT_NOT_FOUND`로 처리한다.

### BoardColumn

- Column의 Project 소유권을 통해 권한을 판정한다.
- Column 추가·이름 변경·정렬·삭제 모두 Project 소유자만 가능하다.
- 다른 Project Column ID를 순서 변경 배열에 넣거나 Task 이동 대상으로 사용할 수 없다.

### Task

- Task의 Project 소유권을 통해 권한을 판정한다.
- 조회·제목/설명 수정·날짜 수정·삭제·이동은 현재 User 소유 Project 안에서만 가능하다.
- Task를 다른 User 또는 다른 Project로 이동할 수 없다.

## 행위별 권한표

| 행위 | 로그인 필요 | 추가 조건 | 거부 결과 |
|---|---:|---|---|
| 내 정보 조회 | 예 | 현재 세션 User | `401` |
| 닉네임 변경 | 예 | 전역 닉네임 유일 | `401` 또는 `409` |
| Project 생성 | 예 | 내 소유로만 생성 | `401` |
| Project 조회·수정·삭제 | 예 | 소유자 | 타인 자원 `404` |
| Column CRUD·정렬 | 예 | Project 소유자 | 타인 자원 `404` |
| 일반 Task 생성 | 예 | Project와 Column 모두 내 소유이며 서로 일치 | `404` 또는 `400` |
| AI Task 생성 | 예 | Project 소유자 | 타인 자원 `404` |
| Task 조회·수정·삭제 | 예 | Project 소유자 | 타인 자원 `404` |
| Task 시작일·종료일 변경 | 예 | Project 소유자 | 타인 자원 `404` |
| Task 이동 | 예 | Task와 대상 Column이 같은 내 Project | `400`/`404` |

## CSRF와 CORS

- 세션 쿠키를 사용하는 모든 `POST`, `PUT`, `PATCH`, `DELETE`는 CSRF 검증 대상이다.
- CSRF 실패는 `403 CSRF_TOKEN_INVALID`다.
- 로컬 CORS 허용 원본은 `http://localhost:5173` 하나다.
- 허용 메서드는 실제 REST 계약에 필요한 메서드로 제한한다.
- 자격 증명 포함 요청을 허용하되 와일드카드 원본을 사용하지 않는다.

## 세션과 쿠키

- Redis Open Source 8.8과 Spring Session Data Redis를 사용하고 namespace는 `ai-kanban:session`이다.
- 세션은 해당 세션을 사용하는 마지막 요청 이후 24시간 동안 활동이 없으면 Redis에서 만료된다.
- 세션 식별 쿠키 이름은 `SESSION`, `HttpOnly=true`, `Path=/`, `SameSite=Lax`, `Max-Age=604800`초다.
- 로컬 HTTP MVP에서는 `Secure=false`다. 운영 HTTPS 쿠키 정책은 MVP 범위 밖이다.
- 쿠키에는 불투명한 세션 ID만 저장하며 User 정보, Google token, 권한 정보를 넣지 않는다.
- 브라우저 종료 후에도 쿠키를 유지한다. 쿠키가 남아 있어도 Redis 세션이 없으면 인증되지 않는다.
- 로그아웃 시 Redis 세션을 무효화하고 쿠키를 `Max-Age=0`으로 즉시 만료시킨다.
- Backend 재시작 중 Redis가 유지되면 세션도 유지한다.
- Redis AOF/RDB 영속성은 요구하지 않으며 Redis 재시작으로 세션이 사라지면 재로그인한다.
- Redis 장애 시 메모리 세션으로 대체하지 않고 JSON API는 `503 SESSION_SERVICE_UNAVAILABLE`을 반환한다.
- CSRF token 조회도 세션을 만들 수 없으면 503이다. logout 중 Redis가 실패하면 쿠키는 만료시키되 503을 반환한다.
- Google 로그인 시작 또는 OAuth callback에서 Redis 세션을 만들 수 없으면 `/login?error=session-service-unavailable`로 이동한다.

기본 로컬 연결은 `localhost:6379`이며 host, port, password는 실행 환경 설정으로 주입할 수 있다.

## 외부 식별자 보호

- Google `sub`를 `google_id`로 사용한다.
- 이메일만으로 User를 식별하지 않는다.
- 로그인 시 Google에서 검증된 이메일만 수용한다.
- OAuth client secret과 Gemini API key를 API 응답 또는 로그에 남기지 않는다.

## 범위 제외

- 관리자 역할, 역할 기반 권한, 조직, 팀
- Project 공유·초대·공개 링크
- JWT Access/Refresh Token
- 회원 탈퇴와 데이터 삭제 요청

## 인증 화면 이동

- 로그인 성공: `/projects`
- OAuth 실패: `/login?error=oauth`
- Redis 때문에 OAuth 세션 저장이 실패: `/login?error=session-service-unavailable`
- 로그아웃 성공: `/login`

## 미확정 항목

없음.

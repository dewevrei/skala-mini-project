# Implementation Plan

문서 상태: 구현 계약 확정본  
현재 단계: 설계 완료, 구현 미착수

## 실행 원칙

- 이 문서는 구현 순서와 검증 기준을 정의한다. 제품 동작은 Functional/Non-Functional Requirements와 각 계약 문서가 기준이다.
- 스키마 DDL은 애플리케이션이 적용하지 않는다. 사용자가 ERD 문서의 DDL을 MySQL Workbench로 실행한다.
- 프런트엔드는 프로덕션 빌드만 검증한다.
- 백엔드는 서비스 메서드 단위 테스트만 작성한다.
- 실제 Google OAuth와 Gemini 호출은 자동 테스트에서 사용하지 않는다.
- 구현 코드는 아직 작성하지 않는다.

## Phase 0 — 프로젝트 기반 준비

### 목표

고정 기술 스택으로 Frontend와 Backend를 분리 구성하고 로컬 실행 설정의 뼈대를 만든다.

### 작업

- `frontend/`: Vue 3 + Vite + SFC + Composition API + Pinia + Vue Router + Axios + Element Plus
- `backend/`: Java 25 + Spring Boot 4.1.0 + Spring Security 7 + Data JPA + Maven
- Spring AI 2.0.x BOM과 Google GenAI starter
- Spring Session Data Redis와 Redis Open Source 8.8 세션 저장소
- 로컬 포트, CORS, 세션, CSRF, MySQL·Redis 연결, 환경 변수 자리 구성
- 공통 `ApiResponse<T>`와 `ApiCode` 기반 마련
- `ddl-auto=validate`

### 검증

- Backend compile 성공
- Frontend production build 성공
- 비밀값이 저장소에 포함되지 않음

## Phase 1 — 데이터 모델과 수동 DDL 정합성

### 목표

User, Project, BoardColumn, Task 엔티티를 ERD와 동일하게 구현한다.

### 작업

- 자동 증가 BIGINT ID
- 유일키, 외래키, cascade, priority check 반영
- nullable `startDate`, `endDate`, `sortOrder`와 timestamp 매핑
- Project/Column/Task 소유권 조회용 repository query
- 사용자가 Workbench에서 DDL 적용 후 Backend 시작 검증

### 검증

- JPA validation 성공
- 의도적으로 다른 스키마에서는 애플리케이션 시작 실패
- 엔티티·DDL 이름과 nullability 수동 대조

## Phase 2 — Google 인증과 사용자

### 목표

Google 로그인으로 User를 생성·갱신하고 세션 기반 API 보안을 제공한다.

### 작업

- OAuth 2.0 login 시작/callback
- Spring Session Data Redis, 24시간 미사용 만료, `SESSION` 쿠키 7일 보관
- Redis namespace `ai-kanban:session`, 로컬 `localhost:6379`, 영속화 비요구
- Redis 장애 시 메모리 fallback 없이 `503 SESSION_SERVICE_UNAVAILABLE`
- Google `sub` → `google_id`
- 최초 닉네임 `name + UUID`
- 재로그인 name/email 동기화, nickname 유지
- `/users/me`, 닉네임 변경, 로그아웃, CSRF 토큰 API
- 허용 CORS 원본과 Axios 자격 증명 설정
- 로그인 성공 `/projects`, 실패 `/login?error=oauth`, 로그아웃 `/login` 이동

### 서비스 단위 테스트

- 신규/기존 User 처리
- 닉네임 생성과 대소문자 무시 중복 거부
- name/email 갱신 시 nickname 유지

## Phase 3 — Project와 BoardColumn

### 목표

사용자별 Project CRUD와 동적 BoardColumn 생명주기를 구현한다.

### 작업

- Project 생성과 기본 열 3개 원자적 생성
- Project 목록 `createdAt DESC, id DESC`
- Project 이름 중복, 수정, 완전 삭제
- Column 추가, 이름 변경, 전체 순서 변경
- 새 Column을 현재 마지막 열 뒤에 추가
- 현재 첫 번째 Column을 AI 대상 열로 조회
- 마지막 Column 삭제 거부
- 확인된 Column 삭제 시 포함 Task cascade

### 서비스 단위 테스트

- 기본 열 이름·순서
- Project/Column 이름 중복
- 전체 Column 순서 입력의 누락·중복·타 Project ID 거부
- 첫 열 변경 반영
- 마지막 열 삭제 거부
- Project/Column 삭제 규칙

## Phase 4 — Task 핵심과 정렬

### 목표

일반 Task CRUD, Board 이동, Items 상태 이동, 그룹 조회, 검색을 구현한다.

### 작업

- 선택 Column 맨 아래 일반 Task 생성; priority 1, startDate/endDate null
- Task title/description 수정, priority 읽기 전용
- Task 날짜 전용 API로 startDate/endDate 지정·해제; 날짜 선후관계 제한 없음
- Task 완전 삭제
- Board 드래그: `targetColumnId`, `beforeTaskId`
- Items 상태 변경: 대상 Column 맨 아래
- 관련 Column의 sortOrder 정규화
- Board/Items 동일 그룹·카드 순서
- title 대소문자 무시 포함 검색

### 서비스 단위 테스트

- 일반 Task 입력과 priority, 생성 날짜 null
- 날짜 지정·해제와 endDate가 startDate보다 앞선 값 허용
- 동일 열 재정렬, 열 간 이동, 맨 아래 이동
- 다른 Project 이동 거부
- Items/Board 순서 일치
- 검색 그룹 구조 유지

## Phase 5 — AI 생성

### 목표

Gemini 구조화 출력으로 평면 Task 목록을 만들고 확정된 실패 정책을 구현한다.

### 작업

- `ChatClient`와 `gemini-2.5-flash` 기본 설정
- 호출당 timeout 30초, temperature 0.2, topP 0.9, 최대 출력 8,192 tokens
- Gemini 안전 설정은 별도로 override하지 않음
- AI 구조 DTO와 schema validation
- 원본 title/description prompt
- 구조 검증 실패 1회 재요청
- 백엔드 description 접두부 조합
- 고정 description 구분자 ` - `
- 응답 순서대로 첫 Column 맨 아래 batch 저장
- AI·fallback Task startDate/endDate null
- 호출·검증 실패 fallback
- DB batch 저장 실패 rollback·500, fallback 금지

### 서비스 단위 테스트

- AI 계약 문서의 전체 테스트 계약 수행
- Gemini gateway는 테스트 대역 사용

## Phase 6 — Frontend 공통 흐름

### 목표

인증 상태, Router 보호, 공통 API·오류 처리, Project 선택 흐름을 구현한다.

### 작업

- OAuth login/logout UI
- CSRF 취득과 Axios interceptor
- Pinia 사용자/Project 상태
- 로그인 필요 route guard
- 공통 `ApiResponse` 성공·오류 처리
- 닉네임 변경 화면
- Project 생성·수정·삭제 경고 모달
- Element Plus 공통 컴포넌트와 GitHub Projects 유사 정보 구조·시각 밀도 적용
- Task 등록 Cancel은 확인창 없이 입력을 폐기하고 시작한 Board/Items로 복귀
- 탭 활성화 시 최신 Project 재조회
- 최소 1280px 최신 Chrome 데스크톱 UI와 기본 포커스·label·색 대비

### 검증

- `npm run build`
- 핵심 흐름 수동 확인

## Phase 7 — Items View

### 목표

열별 그룹 Items 화면과 일반·AI 작업 등록을 구현한다.

### 작업

- BoardColumn 순서의 그룹 표시
- 각 그룹 안 저장 카드 순서 표시
- 각 그룹 `Add item` → 해당 Column 일반 등록
- title 검색, 빈 그룹 유지
- Status 선택 → 대상 Column 맨 아래
- startDate/endDate 열에서 날짜를 직접 지정하거나 비우기
- Task 수정·삭제 모달
- 별도 AI Generate 진입 및 요청 중 버튼 비활성화

### 검증

- `npm run build`
- 생성·검색·상태 이동·날짜 지정·날짜 해제·역순 날짜·삭제 수동 확인

## Phase 8 — Board View

### 목표

동적 칸반 열과 카드 드래그를 구현한다.

### 작업

- Column 추가·수정·정렬·삭제
- 마지막 Column 삭제 UI 방지와 서버 오류 처리
- 각 Column `+` → 해당 Column 일반 등록
- 공통 등록 modal의 AI Generate → 현재 첫 Column에 AI Task 또는 fallback 생성
- Create·AI Generate 요청 중 버튼 비활성화와 Cancel 무경고 입력 폐기
- 카드 동일 열 정렬 및 열 간 이동
- Task 수정·삭제
- Project·Items와 상태 동기화

### 검증

- `npm run build`
- 열·카드 drag/drop과 새로고침 후 순서 유지 수동 확인

## Phase 9 — 통합 정리와 계약 검증

### 목표

요구사항 누락 없이 로컬 MVP를 실행 가능한 상태로 만든다.

### 작업

- 모든 Backend 서비스 단위 테스트 실행
- Frontend production build
- API 응답 code/status 일관성 검토
- 타인 자원 ID 직접 요청의 404 처리 수동 확인
- Google OAuth 로컬 callback 수동 확인
- 실제 Gemini 정상·실패·fallback 수동 확인
- Redis 정상 세션, Backend 재시작 후 유지, Redis 재시작 후 세션 유지 미보장·재로그인 허용, Redis 장애 503 수동 확인
- Workbench DDL과 JPA validation 최종 확인

### 필수 명령

```text
backend: ./mvnw test
frontend: npm run build
```

실제 명령은 scaffold가 만든 wrapper와 package scripts 이름을 최종 확인한다.

## 요구사항 추적표

| 구현 묶음 | Functional Requirements | Non-Functional Requirements |
|---|---|---|
| 인증·사용자 | REQ-FUNC-001~006, 039 | REQ-NFN-005~009, 014, 020, 023~025 |
| Project | REQ-FUNC-007~011 | REQ-NFN-008, 010, 012, 014 |
| BoardColumn | REQ-FUNC-012~016 | REQ-NFN-010~012, 020 |
| 일반 Task | REQ-FUNC-017~019, 034~035, 041 | REQ-NFN-008, 010~012, 014 |
| AI Task | REQ-FUNC-020~027 | REQ-NFN-009~014, 018, 021~022, 028 |
| Items/Board | REQ-FUNC-028~033, 037, 041 | REQ-NFN-011~012, 019~020 |
| 공통 UI/API | REQ-FUNC-036, 038, 040 | REQ-NFN-001~002, 006~007, 014~016, 026~027, 029 |
| 수동 DB·검증 | 전체 데이터 계약 | REQ-NFN-003~004, 016~018 |

## 공유 파일 충돌 방지

- Backend 공통 Security 설정, `ApiCode`, router/controller 등록은 기능 작업마다 동시에 수정하지 않고 통합 단계에서 한 번에 연결한다.
- Frontend Router, Pinia root 등록, Axios 공통 설정도 단일 통합 작업이 소유한다.
- 공통 DTO·응답 봉투를 먼저 확정하고 이후 기능이 소비한다.

## 확정 범위 경계

- 운영 배포·HTTPS·운영 CORS/쿠키, 백업·복구와 운영 모니터링은 MVP 범위 밖이다.
- Frontend 자동 테스트와 Backend controller/security/DB integration test는 작성하지 않는다.
- 성능·부하 SLA, 모바일·태블릿, Chrome 외 브라우저와 접근성 인증 등급은 범위 밖이다.
- 세션·Redis·AI 호출 설정은 각 Phase의 값으로 확정됐다.

## 미확정 항목

없음.

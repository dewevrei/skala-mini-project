# 실행 계획

## 사전 scaffold

Greenfield 초기화는 실행자가 본 작업 전에 인라인으로 수행한다. `frontend/`와 `backend/`, Maven wrapper, npm scripts, 공통 package/namespace marker를 먼저 만들어 병렬 작업 충돌을 막는다. scaffold 자체는 아래 실행 작업이 아니다.

## T1 — 데이터·공통 API 계약

목표: ERD와 ApiResponse 계약을 구현의 공통 기반으로 만든다.  
작업 대상: Backend entity/repository, 공통 API 응답·예외 계층, MySQL·Redis 환경 설정; 사용자 외부 상태인 Workbench DDL.  
행동 계약: 명세 물리 필드 표의 네 엔티티·타입·nullability·유일성·cascade를 동일하게 매핑하고 `ddl-auto=validate`만 사용한다. Column name은 Project 범위, Project name은 User 범위, nickname은 전역 범위에서 대소문자를 무시해 유일하다. 오류 응답은 네 필드 봉투와 의미 있는 HTTP status를 유지한다.  
검증: entity/DDL 수동 대조, Backend compile, schema validation 성공.  
주의: DDL 자동 실행·마이그레이션 코드를 추가하지 않는다.

## T2 — 인증·User 서비스

목표: Google OAuth·Redis 세션과 User 생명주기·닉네임을 구현한다.  
작업 대상: Backend security/auth/user, Frontend auth/profile state와 route.  
행동 계약: 모든 Google 계정 허용, google_id 식별, name/email 갱신, nickname 보존·유일성, CSRF, logout, 사용자별 데이터 기반을 제공한다. Spring Session Data Redis와 Redis 8.8을 사용하고 24시간 미사용 만료·7일 쿠키·Redis 영속성 보장 없음·장애 503·확정 인증 route를 적용한다.  
검증: User 서비스 단위 테스트와 Frontend build. 실제 Google 호출, Backend 재시작 세션 유지, Redis 재시작 후 세션 유지 미보장·재로그인 허용, Redis 장애 503은 수동 확인한다.  
공유 쓰기: 공통 Security 설정과 Frontend Router 최종 연결은 단일 작성자가 마무리한다.

## T3 — Project·BoardColumn 서비스

목표: Project CRUD와 동적 열 생명주기·정렬을 구현한다.  
작업 대상: Backend project/column service/API, 관련 서비스 테스트.  
행동 계약: 기본 열 원자적 생성, Project 최근 생성순 목록, 소유자별 Project name 유일성, Project별 Column name 유일성, 새 Column 맨 뒤 추가, 열 이름/순서, 첫 열 AI 대상, 마지막 열 삭제 금지, 확정 cascade 삭제를 구현한다. Column 삭제는 Project row를 트랜잭션 동안 짧게 잠그고 수를 재검사해 동시 삭제에도 최소 한 열을 보장한다.  
검증: 서비스 단위 테스트로 기본 열, 유일성, 순서 배열, 첫 열 변경, 삭제 edge를 확인한다.

## T4 — 일반 Task·조회·이동 서비스

목표: 일반 Task와 Board/Items의 공통 정렬·검색 계약을 구현한다.  
작업 대상: Backend task service/API/repository query, 서비스 테스트.  
행동 계약: 명시 Column 맨 아래 priority 1 생성, 수정 가능 필드 제한, Board drag 위치, Items 상태 이동 맨 아래, 그룹 조회와 title 검색을 구현한다.  
검증: 서비스 단위 테스트로 정렬·이동·검색·타 Project 거부·불변 priority를 확인한다.

## T5 — AI Task 서비스

목표: Gemini 구조화 출력과 두 종류 실패 분기를 구현한다.  
작업 대상: Backend AI gateway/prompt/DTO/service, 서비스 테스트, 환경 설정.  
행동 계약: `gemini-2.5-flash`, 호출당 timeout 30초, temperature 0.2, topP 0.9, 최대 출력 8,192 tokens, 안전 설정 미지정을 적용한다. exact `{tasks:[{title,description,priority}]}` schema(비어 있지 않음, 추가 속성 금지, 문자열 nonblank, priority 정수 1..5), 1회 검증 재시도, 고정 `원본 title + " - " + AI description` 조합, 첫 열 batch 저장, 호출/검증 fallback, DB rollback·500을 정확히 분리한다.  
검증: 테스트 대역으로 AI 계약의 모든 서비스 테스트를 통과시킨다.

## T6 — Frontend 공통 shell·Project 관리

목표: 로그인 이후 Project 선택과 공통 API 흐름을 완성한다.  
작업 대상: Vue Router, Pinia, Axios, login/profile/project views와 dialogs.  
행동 계약: credentials/CSRF, 명세의 성공 `data` exact shape와 ApiResponse 처리, Project CRUD, 확정 Frontend route, 공통 Task 등록 modal/overlay와 Cancel 무경고 폐기·원래 화면 복귀의 기반, 경고 모달, 탭 focus 재조회를 구현한다. 최소 1280px 최신 Chrome 데스크톱과 기본 포커스·label·색 대비만 공식 범위로 둔다.  
검증: `npm run build`와 수동 Project 흐름.

## T7 — Items View

목표: Column 그룹 Items 화면과 생성·검색·상태 변경을 구현한다.  
작업 대상: Items view/components/store 연결.  
행동 계약: 열·카드 저장 순서, 빈 검색 그룹 유지, 각 그룹 Add item, title 검색, 상태 변경 대상 맨 아래, Task 수정/삭제, AI Generate 요청 중 비활성화를 구현한다. Items에서 연 공통 등록 modal의 일반 Create·AI Generate Cancel이 입력을 폐기하고 Items를 그대로 보여주는지 검증한다.  
검증: Frontend build와 수동 Items 시나리오.

## T8 — Board View

목표: 동적 Column과 카드 drag/drop Board를 구현한다.  
작업 대상: Board view/components/store 연결.  
행동 계약: Column CRUD/정렬, 각 Column 일반 생성, AI Generate 요청·중복 클릭 방지, 카드 동일·교차열 이동, 삭제 경고, 서버 응답 기준 갱신을 구현한다. Board에서 연 공통 등록 modal의 일반 Create·AI Generate Cancel이 입력을 폐기하고 Board를 그대로 보여주는지 검증한다.  
검증: Frontend build와 새로고침 후 순서 유지 수동 확인.

## T9 — 계약 통합 검증

목표: 문서 계약과 통합 결과가 일치하는지 확인한다.  
작업 대상: 전체 프로젝트와 로컬 외부 설정.  
행동 계약: 소유권, error code/status, OAuth callback, Redis 세션 수명·재시작·장애, 실제 Gemini 정상/fallback·확정 호출 설정, DDL validation을 수동 확인하고 필수 자동 검증을 실행한다.  
검증: `backend ./mvnw test`, `frontend npm run build` exit 0; 수동 확인 기록.  
주의: 테스트 범위를 임의로 축소하지 않고 미요청 기능을 추가하지 않는다.

## 단계 설명

T1이 데이터와 응답 형태를 만든다. T2와 T3은 이를 소비해 인증/소유자와 Project/Column 기반을 만든다. T4는 두 기반을 모두 사용한다. T5는 Project 첫 열과 Task 저장을 소비한다. T6은 인증과 Project API가 준비된 뒤 Frontend shell을 연결한다. T7·T8은 Task/API와 shell을 소비해 병렬 구현할 수 있다. T9가 전체를 검증한다.

## 공유 파일 지침

- Backend Security config, global exception handler, `ApiCode`, route/controller 집합은 기능 작업이 중복 편집하지 않도록 공통 기반 또는 최종 통합 작성자가 소유한다.
- Frontend Router, root Pinia, Axios 공통 client는 T6이 소유하며 T7/T8은 feature module만 추가한다.
- 병렬 작업에서 동일 파일 수정이 필요하면 기능 구현은 별도 파일에 두고 등록을 한 작성자에게 미룬다.

## Execution Graph

```yaml
tasks:
  - id: T1
    depends: []
    risk: RISKY
  - id: T2
    depends: [T1]
    risk: RISKY
  - id: T3
    depends: [T1, T2]
    risk: RISKY
  - id: T4
    depends: [T1, T2, T3]
    risk: RISKY
  - id: T5
    depends: [T1, T2, T3, T4]
    risk: RISKY
  - id: T6
    depends: [T1, T2, T3]
    risk: MECHANICAL
  - id: T7
    depends: [T4, T5, T6]
    risk: RISKY
  - id: T8
    depends: [T3, T4, T5, T6]
    risk: RISKY
  - id: T9
    depends: [T5, T7, T8]
    risk: RISKY
regen_barriers: []
```

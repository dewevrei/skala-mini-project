# 실행 인계

## 문서 역할

| 문서 | 역할 | 충돌 시 처리 |
|---|---|---|
| `.dryforge/spec.md` | 구현할 제품 동작과 불변 규칙의 최종 기준 | 제품 동작 충돌 시 우선 |
| `.dryforge/plan.md` | 작업 순서, 작업 대상, 검증 방법 | 명세 안에서 수정 가능 |
| `docs/*.md` | 명세를 목적별로 펼친 요구사항·도메인·API·데이터·오류·AI 참고 문서 | 추가 권위를 만들지 않으며 명세와 충돌하면 명세만 따름 |
| 기존/생성 코드 | 구현 방식과 관례 | 요구사항을 변경할 권위 없음 |

명세 오류나 사용자 정책 변경은 사용자 승인 없이 수정하지 않는다.

## 파일 위치

- `.dryforge/spec.md`
- `.dryforge/plan.md`
- `.dryforge/handoff.md`
- `docs/functional-requirements.md`
- `docs/non-functional-requirements.md`
- `docs/domain-model.md`
- `docs/erd.md`
- `docs/rest-api-spec.md`
- `docs/validation-rules.md`
- `docs/authorization-rules.md`
- `docs/error-response-specification.md`
- `docs/ai-integration-contract.md`
- `docs/implementation-plan.md`

## 실행 형태

Greenfield 분리형 웹 애플리케이션이다. 실행자는 먼저 고정 스택을 scaffold한 뒤 데이터/공통 계약을 만들고, Redis 세션 인증·Project/Column·Task·AI를 구현한다. Frontend는 Backend 계약이 안정된 뒤 Items와 Board를 연결한다. DDL 적용은 사용자가 Workbench에서 수행하는 외부 단계이며 애플리케이션은 이를 자동 실행하지 않는다.

## 절대 조건

- 구현 전에 `.dryforge/spec.md`를 읽는다. `docs/`는 목적별 상세 확인과 추적에 사용하되 명세에 없는 새 동작을 만들지 않는다.
- 계층 Task, 협업, 캘린더·자동 일정 기능을 추가하지 않는다. Task의 nullable 시작일·종료일은 구현 범위다.
- 현재 첫 Column만 AI 생성 대상이며 일반 생성에는 항상 명시적 Column 문맥이 필요하다.
- Column name은 Project 안에서 대소문자를 무시하고 유일하다.
- AI 실패 fallback과 AI DB 실패 rollback을 혼동하지 않는다.
- 마지막 Column 불변식은 삭제 트랜잭션의 짧은 Project row lock으로 보장한다. 일반 수정의 last-write-wins를 장기 사용자 잠금으로 바꾸지 않는다.
- 모든 자원 접근에 사용자 소유권을 적용한다.
- 오류 응답은 제공된 네 필드 `ApiResponse<T>`를 유지하고 오류 data를 null로 둔다.
- DDL을 자동 적용하지 않고 `ddl-auto=validate`만 사용한다.
- 비밀값을 커밋하거나 응답·로그에 노출하지 않는다.
- Redis 8.8을 유일한 세션 저장소로 사용하고 장애 시 메모리 세션으로 바꾸지 않는다.
- Project 목록은 최근 생성순, 새 Column은 맨 뒤, AI 설명 구분자는 정확히 ` - `다.
- 로그인 성공·실패·로그아웃과 Task 등록 Cancel의 확정 이동 경로를 임의로 바꾸지 않는다.
- 공식 Frontend 범위는 최소 1280px 최신 Chrome 데스크톱이며 모바일 UI를 추가하지 않는다.
- Backend 검증 범위를 서비스 단위 테스트보다 임의로 줄이지 않는다. Frontend는 build가 반드시 성공해야 한다.

## 문서에 포착되지 않은 의도

- UI는 Element Plus를 사용해 제공된 GitHub Projects 참고 이미지의 구조감과 시각적 밀도를 따른다. 참고 이미지의 담당자·Estimate·저장소 등 미요청 기능과 모바일 전용 UI는 만들지 않되, 확정된 시작일·종료일은 Items에 제공한다.
- 단순 MVP를 우선하므로 실시간 동기화와 서버 멱등성을 넣지 않는다.
- priority는 정보이자 AI 판단 결과지만 자동 정렬 기준은 아니다.

## Project Foundation

비실행 프로젝트 문맥 — 실행 시 설계 배경으로 읽되 이 절 전체를 구현 범위로 확대하지 않는다.

### 1. 프로젝트 정체성

이 서비스는 여러 Google 사용자가 각자 격리된 개인 Project와 칸반보드로 일상·개인 프로젝트 Task를 관리하는 로컬 MVP다. 핵심 가치는 사용자가 쓴 큰 할 일을 Gemini가 실행 가능한 평면 Task로 나누는 것과 Board/Items 양쪽에서 동일 상태·순서를 관리하는 것이다. 협업 제품이나 자동 일정 서비스가 아니다.

### 2. 도메인 모델

User가 Project를 소유하고, Project가 순서 있는 BoardColumn을, BoardColumn이 순서 있는 Task를 포함한다. Task 상태는 고정 enum이 아니라 BoardColumn이다. Project는 최소 한 Column을 유지하고 현재 첫 Column이 AI 등록 대상이다. 일반 Task는 반드시 사용자가 선택한 Column 문맥에서 생성한다. AI 원본은 저장하지 않고 AI 결과 또는 실패 시 원본 기반 단일 fallback Task만 저장한다. Project·Column·Task 삭제는 확인 후 복구 없이 완전 삭제한다.

### 3. 기술 결정

Vue 3/Vite/Element Plus Frontend와 Java 25/Spring Boot 4.1 Backend를 REST로 분리한다. Spring Security Google OAuth, Spring Session Data Redis, Redis Open Source 8.8, CSRF와 사용자별 소유권 검사를 적용한다. 세션은 24시간 미사용 만료, 쿠키는 7일이며 Redis 장애는 503으로 처리한다. MySQL 8.0.46 DDL은 사용자가 Workbench로 적용하고 JPA가 validate한다. Spring AI 2.0.x ChatClient와 Gemini Developer API를 사용하며 호출 설정은 timeout 30초, temperature 0.2, topP 0.9, 최대 8,192 tokens다. 동시 수정은 마지막 저장 우선이며 다른 탭은 활성화 시 재조회한다. Backend 서비스 단위 테스트와 Frontend production build가 필수다.

### 4. 향후 범위

운영 배포, HTTPS, 백업, 관측, 성능 SLA, 모바일·태블릿 UI, Chrome 외 브라우저 검증, 접근성 인증, 실시간 동기화, 서버 멱등성, 협업, 계층 Task, 예상 소요 시간·캘린더·자동 일정, 첨부·담당자·카테고리·로드맵, 회원 탈퇴는 이번 구현 범위가 아니다. 향후 추가할 때 현재 User 소유권과 Project/Column/Task 불변 규칙을 의식적으로 재설계해야 한다.

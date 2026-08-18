# Non-Functional Requirements

문서 상태: 구현 계약 확정본  
기준일: 2026-08-18

## 요구사항

| 요구사항 ID | 요구사항명 | 상세 요구사항 내용 | 중요도 | 난이도 | 비고/제약사항 |
|---|---|---|---|---|---|
| REQ-NFN-001 | 기술 스택 준수 | Frontend는 Vue.js 3, SFC, Vite, Composition API, Pinia, Vue Router, Axios를 사용하고 Backend는 Java 25, Spring Boot 4.1.0, Spring Security 7, Spring Data JPA, Spring Session Data Redis, Maven을 사용한다. | 필수 | 중간 | 대체 스택 사용 금지 |
| REQ-NFN-002 | 분리형 구조 | 프런트엔드와 백엔드는 분리된 애플리케이션으로 구성하고 REST API로 통신한다. | 필수 | 중간 | 로컬 개발 포트 분리 |
| REQ-NFN-003 | 데이터베이스 고정 | MySQL 8.0.46을 사용한다. 스키마 DDL은 사용자가 MySQL Workbench로 직접 적용한다. | 필수 | 중간 | 자동 마이그레이션 도구 사용 안 함 |
| REQ-NFN-004 | 스키마 검증 | JPA `ddl-auto=validate`를 사용해 시작 시 엔티티와 실제 스키마 불일치를 탐지하고 불일치 시 실행을 중단한다. | 필수 | 낮음 | 자동 생성·수정 금지 |
| REQ-NFN-005 | 인증 보안 | Google OAuth 2.0 로그인 후 Redis 기반 서버 세션과 `HttpOnly` 세션 쿠키를 사용한다. JWT를 발급하지 않는다. | 필수 | 높음 | Spring Security 7 + Spring Session Data Redis |
| REQ-NFN-006 | CSRF 보호 | 상태 변경 REST 요청에는 CSRF 토큰을 요구하고 검증 실패 시 `403`을 반환한다. | 필수 | 높음 | Axios가 토큰 헤더 전송 |
| REQ-NFN-007 | CORS 최소 허용 | 로컬 개발에서는 `http://localhost:5173`만 자격 증명 포함 CORS 원본으로 허용한다. | 필수 | 중간 | 운영 CORS는 MVP 범위 밖 |
| REQ-NFN-008 | 자원 소유권 보호 | 서버의 모든 프로젝트·열·작업 접근에서 로그인 사용자 소유권을 검증한다. 클라이언트가 전달한 사용자 ID를 신뢰하지 않는다. | 필수 | 높음 | 타인 자원은 `404` 처리 |
| REQ-NFN-009 | 비밀정보 보호 | Google OAuth 비밀키, Gemini API 키와 Redis 비밀번호가 있는 경우 이를 환경 설정으로 주입하고 코드·Git·응답·일반 로그에 기록하지 않는다. | 필수 | 중간 | 로컬 환경 파일도 커밋 금지 |
| REQ-NFN-010 | 트랜잭션 원자성 | 프로젝트와 기본 열 생성, AI 다중 작업 저장, 열/프로젝트 연쇄 삭제는 각각 하나의 트랜잭션으로 처리한다. | 필수 | 높음 | AI DB 실패 시 전체 롤백 |
| REQ-NFN-011 | 동시 수정 정책 | 일반 변경은 별도 사용자 잠금이나 버전 충돌 응답 없이 마지막으로 서버에 반영된 결과를 사용한다. 최소 한 Column 불변식을 지키기 위한 삭제 트랜잭션의 짧은 DB 행 잠금은 허용한다. | 높음 | 중간 | 개인 보드·다중 탭 기준 |
| REQ-NFN-012 | 정렬 결정성 | 열은 `sortOrder, id`, 카드는 `sortOrder, id` 순으로 조회해 동률에도 결과가 결정적이어야 한다. | 필수 | 중간 | 이동 후 순번 정규화 가능 |
| REQ-NFN-013 | AI 장애 내성 | AI 호출·응답 검증 실패가 일반 작업 등록 기능 전체의 장애로 번지지 않도록 확정된 대체 저장을 수행한다. | 필수 | 높음 | DB 저장 실패는 대체 대상 아님 |
| REQ-NFN-014 | API 일관성 | OAuth 리다이렉트 외 JSON API는 동일한 `ApiResponse<T>` 봉투와 의미에 맞는 HTTP 상태 코드를 사용한다. | 필수 | 중간 | 오류 `data`는 항상 `null` |
| REQ-NFN-015 | 로컬 실행 환경 | MVP는 Frontend `localhost:5173`, Backend `localhost:8080`, MySQL `localhost:3306`, Redis `localhost:6379` 기본 구성을 대상으로 한다. | 필수 | 낮음 | 연결값은 환경 설정으로 주입 가능 |
| REQ-NFN-016 | 프런트엔드 검증 | 프런트엔드는 프로덕션 빌드가 성공해야 한다. 자동 단위·컴포넌트·브라우저 테스트는 작성하지 않는다. | 필수 | 낮음 | `npm run build` |
| REQ-NFN-017 | 백엔드 검증 | 백엔드는 서비스 메서드 단위 테스트만 작성하고 통과시켜야 한다. 컨트롤러·보안·DB 연동 자동 테스트는 범위 밖이다. | 필수 | 중간 | `./mvnw test` |
| REQ-NFN-018 | 외부 API 테스트 격리 | 서비스 단위 테스트에서 Google OAuth와 Gemini를 실제 호출하지 않고 테스트 대역을 사용한다. | 필수 | 중간 | 비용·불안정성 차단 |
| REQ-NFN-019 | 응답 최신화 | 변경 성공 시 서버 응답을 화면의 기준으로 사용하고, 브라우저 탭 활성화 시 최신 상태를 재조회한다. | 높음 | 중간 | WebSocket·SSE·polling 없음 |
| REQ-NFN-020 | 문자 처리 | 사용자 입력과 검색은 UTF-8/`utf8mb4`를 사용하고 이름 중복 및 제목 검색은 대소문자를 구분하지 않는다. | 필수 | 중간 | MySQL collation과 앱 검증 일치 |
| REQ-NFN-021 | 구조화 AI 출력 | AI 응답은 자유 텍스트가 아니라 명시된 작업 목록 구조로 변환·검증한 뒤에만 저장한다. | 필수 | 높음 | Spring AI structured output |
| REQ-NFN-022 | 의존성 호환성 | Spring AI 2.0.x BOM과 Google GenAI starter를 사용해 Spring Boot 4.1.x 호환성을 유지한다. | 필수 | 중간 | 정확한 패치 버전은 구현 시 최신 2.0.x 검증 |
| REQ-NFN-023 | Redis 세션 저장 | Redis Open Source 8.8을 Spring Session 저장소로 사용한다. Backend 재시작 시 Redis가 유지되면 로그인 세션도 유지한다. | 필수 | 중간 | Redis는 업무 데이터 저장에 사용하지 않음 |
| REQ-NFN-024 | 세션 만료와 쿠키 | Redis 세션은 마지막 요청 후 24시간 미사용 시 만료하고 `SESSION` 쿠키는 7일간 보관한다. 브라우저 종료 후에도 쿠키를 유지하며 로그아웃 시 둘 다 즉시 제거한다. | 필수 | 중간 | 쿠키에는 불투명 세션 ID만 저장 |
| REQ-NFN-025 | Redis 장애 정책 | Redis 장애 시 메모리 세션으로 대체하지 않고 세션이 필요한 JSON API는 `503 SESSION_SERVICE_UNAVAILABLE`을 반환한다. | 필수 | 높음 | Redis 복구 후 재요청; Redis 재시작 시 재로그인 허용 |
| REQ-NFN-026 | 데스크톱 화면 지원 | 최소 너비 1280px의 최신 Chrome 정식 데스크톱 버전을 공식 지원한다. 모바일·태블릿과 다른 브라우저의 공식 검증은 하지 않는다. | 높음 | 낮음 | 반응형 모바일 UI는 MVP 범위 밖 |
| REQ-NFN-027 | 기본 접근성 | 키보드 포커스 표시, 입력 항목의 이름 연결, 기본 색상 대비를 적용한다. | 높음 | 낮음 | 특정 접근성 인증 등급 준수는 범위 밖 |
| REQ-NFN-028 | AI 호출 설정 | Gemini 호출당 timeout 30초, temperature 0.2, topP 0.9, 최대 출력 8,192 tokens를 사용하며 안전 설정은 별도로 덮어쓰지 않는다. | 필수 | 중간 | topK 등 미지정 항목은 모델 기본값 |

## 품질 목표의 경계

- 성능 수치, 동시 사용자 수, 가용성, 복구 시간 목표는 MVP 범위 밖이다.
- 운영 모니터링, 중앙 로그, 백업·복구, HTTPS, 운영 CORS와 배포 자동화는 MVP 범위 밖이다.
- 모바일·태블릿, Chrome 외 브라우저 검증과 특정 접근성 인증 등급은 MVP 범위 밖이다.
- Frontend 자동 테스트와 Backend controller/security/DB integration test는 확정된 검증 범위 밖이다.

## 미확정 항목

없음. 로컬 MVP의 실행·보안·화면·AI 설정은 위 요구사항으로 확정했으며 운영 환경은 미확정이 아니라 명시적 범위 제외다.

## 참고한 공식 호환성 기준

- Spring Boot 4.1.0은 2026-06-10 정식 출시된 버전이다.
- Spring AI 2.0.x는 Spring Boot 4.0.x와 4.1.x를 지원한다.
- Google GenAI starter는 Gemini Developer API 키 방식과 `ChatClient` 구조화 출력을 지원한다.
- Spring Boot 4.1은 `spring-boot-starter-session-data-redis`로 Redis 기반 Spring Session 자동 구성을 지원한다.
- Redis Open Source 8.8 정식 버전을 사용한다.

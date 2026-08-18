# AI Integration Contract

문서 상태: 구현 계약 확정본  
Provider: Google Gemini Developer API  
Client: Spring AI `ChatClient`

## 고정 기술 계약

- Spring AI 2.0.x BOM을 사용한다.
- Google GenAI starter를 사용한다.
- 인증은 Google AI Studio에서 발급한 API key다.
- 기본 모델은 `gemini-2.5-flash`다.
- 모델 ID는 실행 환경 설정으로 교체할 수 있다.
- API key는 실행 환경에서 주입하고 소스·Git·응답·일반 로그에 넣지 않는다.
- streaming을 사용하지 않는 동기 `ChatClient` 호출이다.
- 호출 한 번의 timeout은 30초다.
- 생성 설정은 `temperature=0.2`, `topP=0.9`, 최대 출력 `8,192 tokens`다.
- topK와 thinking 관련 미지정 옵션은 모델 기본값을 사용한다.
- 안전 설정은 요청에서 별도로 지정하지 않고 Gemini 2.5 Flash 기본 정책을 사용한다.
- `8,192 tokens`는 호출 비용·응답 크기의 기술 한도이며 Task 개수에 대한 비즈니스 제한은 아니다. 토큰 한도로 응답이 잘리면 구조 검증 실패로 처리한다.

## 입력 계약

```json
{
  "title": "회원 변경 기능",
  "description": "회원이 닉네임과 비밀번호를 각각 변경할 수 있어야 한다."
}
```

- 두 필드는 trim 후 비어 있지 않아야 한다.
- `title`은 생성 Task들의 공통 상위 문맥이지만 별도 Task로 저장하지 않는다.
- `description`은 작업 분해 문맥으로만 사용하고 AI 성공 시 DB에 원문을 저장하지 않는다.
- Project 설명이나 기존 Task들은 prompt에 포함하지 않는다.

## 시스템 지시의 의미 계약

Gemini에 다음 행동을 요구해야 한다.

1. 입력 목표를 실제로 실행할 수 있는 독립 작업들로 나눈다.
2. 부모·자식 계층을 만들지 않는다.
3. 문맥상 적절한 작업 수를 스스로 정한다. 최소·최대 개수를 지시하지 않는다.
4. 각 작업에 명확한 title과 실행 내용을 설명하는 description을 작성한다.
5. 각 작업 priority를 `1~5` 정수로 정하며 `1`을 가장 높게 사용한다.
6. 지정된 구조 이외의 설명문·Markdown·코드블록을 반환하지 않는다.

## 구조화 출력

Provider structured output을 우선 사용하고 Spring AI entity mapping과 schema validation을 적용한다.

```json
{
  "tasks": [
    {
      "title": "회원 닉네임 변경",
      "description": "회원 닉네임 변경 기능을 구현한다.",
      "priority": 2
    },
    {
      "title": "회원 비밀번호 변경",
      "description": "회원 비밀번호 변경 기능을 구현한다.",
      "priority": 1
    }
  ]
}
```

내부 DTO 의미:

```text
AiTaskBatch
  tasks: List<AiTaskItem> (필수, 비어 있지 않음)

AiTaskItem
  title: String (필수, 비어 있지 않음)
  description: String (필수, 비어 있지 않음)
  priority: Integer (필수, 1..5)
```

## 검증과 재시도

1. 최초 모델 호출 결과를 전체 schema와 업무 규칙으로 검증한다.
2. 일부 항목만 실패해도 전체 batch를 실패로 본다.
3. 구조·값 검증 실패 시 검증 오류를 반영해 동일 원본 입력으로 한 번 재요청한다.
4. 두 번째 결과도 실패하면 fallback으로 전환한다.
5. 연결 실패, timeout, 안전 필터 차단처럼 구조 결과를 얻지 못한 호출 실패는 즉시 fallback한다.

재시도는 최대 한 번이다. Spring AI의 기본 검증 재시도 수가 더 크더라도 이 계약에 맞게 제한한다.

각 호출은 30초를 넘기면 실패다. 호출 자체의 timeout에는 재시도하지 않으며, 첫 응답을 받았지만 구조·값이 잘못된 경우에만 두 번째 호출을 수행하므로 최악의 AI 대기 시간은 약 60초다.

## 저장 변환

각 유효 AI 항목은 저장 전에 다음처럼 변환한다.

```text
stored.title       = trim(ai.title)
stored.description = trim(original.title) + " - " + trim(ai.description)
stored.priority    = ai.priority
stored.column      = 현재 sortOrder가 첫 번째인 BoardColumn
stored.sortOrder   = 대상 열의 마지막 순서 다음 값
```

- 구분자 ` - `는 고정 계약이다.
- 최종 저장 description은 5,000자 이하여야 한다. AI description의 허용 길이는 원본 title과 구분자 길이를 뺀 값으로 검증한다.
- Gemini 반환 배열 순서대로 Task 순서를 부여한다.
- AI 생성 성공 시 원본 title·description을 부모·별도 Task나 요청 이력으로 저장하지 않는다. fallback Task는 이 규칙의 명시적 예외다.
- AI 출처 필드를 Task에 추가하지 않는다.

## 트랜잭션 계약

### AI 결과 유효

- 전체 Task를 하나의 DB 트랜잭션에서 저장한다.
- 하나라도 저장 실패하면 전부 rollback한다.
- 이 DB 실패에서는 fallback을 시도하지 않는다.
- API는 `500 TASK_BATCH_SAVE_FAILED`를 반환한다.

### AI 호출·검증 실패

- `title=trim(originalTitle)`, `description=trim(originalDescription)`으로 일반 Task 하나를 만든다.
- priority는 `1`이다.
- 요청 처리 시점의 첫 번째 BoardColumn 맨 아래에 저장한다.
- 성공 API는 정상 AI 생성과 같은 `201 TASKS_CREATED` 형태를 사용한다.
- Frontend에 AI 실패 여부를 알리는 field·code·message를 제공하지 않는다.

## 예시

입력:

```json
{
  "title": "회원 변경 기능",
  "description": "xxx"
}
```

모델 항목:

```json
{
  "title": "회원 닉네임 변경",
  "description": "회원 닉네임 변경 작업",
  "priority": 2
}
```

DB Task:

```json
{
  "title": "회원 닉네임 변경",
  "description": "회원 변경 기능 - 회원 닉네임 변경 작업",
  "priority": 2
}
```

## 비용·중복 제약

- Frontend는 요청 중 `AI Generate` 버튼을 비활성화한다.
- 서버 멱등성 키는 없다.
- 동일 요청이 여러 번 실제 도착하면 Gemini 호출과 Task 저장이 중복될 수 있다.

## 테스트 계약

실제 Gemini를 호출하지 않고 AI gateway를 테스트 대역으로 교체해 서비스 메서드를 검증한다.

- 정상 다중 결과 저장과 순서
- 원본 title 접두부 조합
- priority 범위
- 최초 형식 실패 후 1회 재시도 성공
- 두 번 형식 실패 후 fallback
- 일부 잘못된 항목이 있을 때 전체 fallback
- 호출 실패 fallback
- AI batch DB 저장 실패 전체 rollback 및 fallback 금지

## 확정 호출 설정

| 항목 | 값 |
|---|---|
| 모델 | `gemini-2.5-flash` |
| 호출당 timeout | 30초 |
| temperature | `0.2` |
| topP | `0.9` |
| 최대 출력 | `8,192 tokens` |
| 안전 설정 | 별도 override 없음; 모델 기본 정책 |
| 설명 구분자 | 정확히 ` - ` |

이 값들은 MVP 구현 계약으로 확정됐다.

## 미확정 항목

없음.

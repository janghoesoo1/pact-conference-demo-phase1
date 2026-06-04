# Pact Conference Demo - 아키텍처 문서

## 1. 개요

### 1.1 프로젝트 목적

이 프로젝트는 "Mastering API Architecture" 책의 Consumer-Driven Contract (CDC) 테스트 개념을 실습하기 위한 데모입니다. 컨퍼런스 관리 도메인을 모델로 삼아 두 마이크로서비스(세션 서비스 / 참석자 서비스) 사이의 계약 테스트 전체 흐름을 구현합니다.

### 1.2 핵심 개념: Consumer-Driven Contract (CDC) 테스트

CDC 테스트는 서비스 간 API 호환성을 보장하는 테스트 전략입니다. 핵심 아이디어는 **API를 호출하는 쪽(Consumer)이 계약을 정의**하고, **API를 제공하는 쪽(Provider)이 그 계약을 검증**하는 것입니다.

- Consumer: `AttendeeService` - SessionService API를 호출하는 서비스
- Provider: `SessionService` - API를 실제로 제공하는 서비스

### 1.3 왜 계약 테스트가 필요한가

| 비교 항목 | E2E 테스트 | 계약 테스트 |
|----------|-----------|-----------|
| 실행 속도 | 느림 (전체 환경 필요) | 빠름 (MockServer/단위 수준) |
| 피드백 시점 | 통합 환경 구축 후 | 로컬 개발 단계에서 즉시 |
| 실패 원인 파악 | 어려움 (전체 스택 관여) | 명확함 (계약 위반 지점 특정) |
| 독립 배포 가능성 | 낮음 (전체 재배포 필요) | 높음 (can-i-deploy 게이트 활용) |
| 유지보수 비용 | 높음 | 낮음 |

계약 테스트는 E2E 테스트를 완전히 대체하지 않지만, 서비스 간 API 호환성 검증에서 훨씬 빠르고 신뢰성 높은 피드백을 제공합니다.

---

## 2. 시스템 아키텍처

### 2.1 전체 구조도

```
[AttendeeService:8080] --REST--> [SessionService:8081]
         |                              |
         +-------> [Pact Broker:9292] <-+
                        |
                   [PostgreSQL:5432]
```

- `AttendeeService`(포트 8080): 참석자 CRUD + SessionService 호출
- `SessionService`(포트 8081): 세션 CRUD, Pact Provider
- `Pact Broker`(포트 9292): 계약 파일 저장, 검증 결과 관리, can-i-deploy 판정
- `PostgreSQL`(포트 5432): Pact Broker의 영속성 저장소

### 2.2 모듈 구조

```
pact-conference-demo/
├── build.gradle.kts          # 루트 빌드 설정 (공통 플러그인/의존성)
├── settings.gradle.kts       # 멀티 모듈 선언
├── docker-compose.yml        # Pact Broker + PostgreSQL
├── common/                   # 공유 도메인 모델 라이브러리
│   └── src/main/kotlin/com/conference/common/
│       ├── model/
│       │   ├── Attendee.kt
│       │   ├── Session.kt
│       │   └── ApiResponse.kt
│       └── exception/
│           └── Exceptions.kt
├── session-service/          # Provider 서비스 (포트 8081)
│   └── src/
│       ├── main/kotlin/com/conference/session/
│       │   ├── SessionServiceApplication.kt
│       │   ├── store/SessionStore.kt
│       │   └── controller/SessionController.kt
│       └── test/kotlin/com/conference/session/
│           ├── controller/SessionControllerTest.kt
│           └── pact/SessionServiceProviderPactTest.kt
└── attendee-service/         # Consumer 서비스 (포트 8080)
    └── src/
        ├── main/kotlin/com/conference/attendee/
        │   ├── AttendeeServiceApplication.kt
        │   ├── config/RestClientConfig.kt
        │   ├── client/SessionClient.kt
        │   ├── store/AttendeeStore.kt
        │   └── controller/AttendeeController.kt
        ├── test/kotlin/com/conference/attendee/
        │   └── pact/SessionServiceConsumerPactTest.kt
        └── build/pacts/
            └── AttendeeService-SessionService.json  # 생성된 계약 파일
```

### 2.3 의존성 흐름

**런타임 의존성:**
```
AttendeeService --HTTP GET /sessions--> SessionService
AttendeeService --HTTP GET /sessions/{id}--> SessionService
```

**계약 정의 흐름:**
```
AttendeeService (Consumer 테스트 실행)
  -> 계약 파일 생성: build/pacts/AttendeeService-SessionService.json
  -> SessionService (Provider 테스트가 파일 로드 후 검증)
```

**빌드 의존성:**
- `session-service` -> `common`
- `attendee-service` -> `common`
- 모두 Kotlin 1.9.25 / Spring Boot 3.3.6 / JVM target 21

---

## 3. 도메인 모델

### 3.1 Attendee

`common/src/main/kotlin/com/conference/common/model/Attendee.kt`

```kotlin
data class Attendee(
    val id: Int? = null,
    val givenName: String,
    val surname: String,
    val email: String
)
```

| 필드 | 타입 | 필수 여부 | 설명 |
|------|------|----------|------|
| id | Int? | 선택 (자동 할당) | 참석자 고유 식별자. 생성 시 null, 저장 후 할당됨 |
| givenName | String | 필수 | 이름 |
| surname | String | 필수 | 성 |
| email | String | 필수 | 이메일 주소 |

초기 데이터 3건이 `AttendeeStore`에 인메모리로 적재됩니다 (Jim Gough, Matt Auburn, Daniel Bryant).

### 3.2 Session

`common/src/main/kotlin/com/conference/common/model/Session.kt`

```kotlin
data class Session(
    val id: Int? = null,
    val title: String,
    val speaker: String,
    val description: String? = null,
    val dateTime: LocalDateTime? = null
)
```

| 필드 | 타입 | 필수 여부 | 설명 |
|------|------|----------|------|
| id | Int? | 선택 (자동 할당) | 세션 고유 식별자 |
| title | String | 필수 | 세션 제목 |
| speaker | String | 필수 | 발표자 이름 |
| description | String? | 선택 | 세션 설명 |
| dateTime | LocalDateTime? | 선택 | 세션 일시 (ISO 8601 형식으로 직렬화: `2024-09-15T10:00:00`) |

초기 데이터 3건이 `SessionStore`에 인메모리로 적재됩니다:
- ID 1: "gRPC로 마이크로서비스 구축하기" (장호, 2024-09-15 10:00)
- ID 2: "API 게이트웨이 패턴" (김현수, 2024-09-15 14:00)
- ID 3: "계약 테스트 실전" (이서연, 2024-09-16 10:00)

### 3.3 ApiResponse\<T\>

`common/src/main/kotlin/com/conference/common/model/ApiResponse.kt`

```kotlin
data class ApiResponse<T>(
    val data: List<T>,
    val total: Int = data.size
)
```

목록 조회 엔드포인트의 공통 응답 래퍼입니다. `total`은 `data.size`로 자동 계산됩니다.

예시:
```json
{
  "data": [
    { "id": 1, "title": "gRPC로 마이크로서비스 구축하기", "speaker": "장호" }
  ],
  "total": 1
}
```

### 3.4 예외 클래스

`common/src/main/kotlin/com/conference/common/exception/Exceptions.kt`

```kotlin
class ResourceNotFoundException(message: String) : RuntimeException(message)
```

존재하지 않는 리소스 조회 시 발생합니다. 각 컨트롤러에서 이를 잡아 HTTP 404로 변환합니다.

---

## 4. API 명세

### 4.1 Session Service API (Provider, 포트 8081)

기본 경로: `/sessions`

#### GET /sessions

전체 세션 목록을 조회합니다.

- Response: `200 OK`
```json
{
  "data": [
    {
      "id": 1,
      "title": "gRPC로 마이크로서비스 구축하기",
      "speaker": "장호",
      "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
      "dateTime": "2024-09-15T10:00:00"
    }
  ],
  "total": 3
}
```

#### GET /sessions/{id}

특정 세션을 조회합니다.

- Response: `200 OK`
```json
{
  "id": 1,
  "title": "gRPC로 마이크로서비스 구축하기",
  "speaker": "장호",
  "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
  "dateTime": "2024-09-15T10:00:00"
}
```
- Response: `404 Not Found` (세션이 존재하지 않을 때)
```json
{
  "error": "Session with id 999 not found"
}
```

#### POST /sessions

새 세션을 생성합니다.

- Request Body:
```json
{
  "title": "새로운 세션",
  "speaker": "박지수",
  "description": "새로운 세션 설명"
}
```
- Response: `201 Created` + `Location` 헤더 (`/sessions/{id}`)
```json
{
  "id": 4,
  "title": "새로운 세션",
  "speaker": "박지수",
  "description": "새로운 세션 설명",
  "dateTime": null
}
```

#### PUT /sessions/{id}

세션 정보를 수정합니다.

- Request Body: Session 전체 필드
- Response: `204 No Content`
- Response: `404 Not Found` (세션이 존재하지 않을 때)

#### DELETE /sessions/{id}

세션을 삭제합니다.

- Response: `200 OK`
- Response: `404 Not Found` (세션이 존재하지 않을 때)

### 4.2 Attendee Service API (Consumer, 포트 8080)

기본 경로: `/attendees`

#### GET /attendees

전체 참석자 목록을 조회합니다.

- Response: `200 OK`
```json
{
  "data": [
    { "id": 1, "givenName": "Jim", "surname": "Gough", "email": "gough@mail.com" }
  ],
  "total": 3
}
```

#### GET /attendees/{id}

특정 참석자를 조회합니다.

- Response: `200 OK`
```json
{
  "id": 1,
  "givenName": "Jim",
  "surname": "Gough",
  "email": "gough@mail.com"
}
```
- Response: `404 Not Found`

#### GET /attendees/{id}/sessions

특정 참석자가 볼 수 있는 세션 목록을 조회합니다. 내부적으로 SessionService를 호출합니다.

- Response: `200 OK` (ApiResponse\<Session\> 형식)
- Response: `404 Not Found` (참석자가 존재하지 않을 때)

#### POST /attendees

새 참석자를 등록합니다.

- Request Body: `{ "givenName": "...", "surname": "...", "email": "..." }`
- Response: `201 Created`

#### PUT /attendees/{id}

참석자 정보를 수정합니다.

- Response: `204 No Content`
- Response: `404 Not Found`

#### DELETE /attendees/{id}

참석자를 삭제합니다.

- Response: `200 OK`
- Response: `404 Not Found`

### 4.3 서비스 간 통신

`attendee-service`는 `SessionClient`를 통해 `session-service`를 호출합니다.

**RestClient 설정** (`RestClientConfig.kt`):

```kotlin
@Bean
fun sessionRestClient(
    @Value("\${session-service.url:http://localhost:8081}") baseUrl: String
): RestClient = RestClient.builder()
    .baseUrl(baseUrl)
    .build()
```

기본 URL은 `http://localhost:8081`이며, `session-service.url` 프로퍼티로 재정의할 수 있습니다.

**SessionClient 에러 처리**:

```kotlin
fun getSession(id: Int): Session? {
    return try {
        sessionRestClient.get()
            .uri("/sessions/{id}", id)
            .retrieve()
            .body(Session::class.java)
    } catch (e: RestClientResponseException) {
        if (e.statusCode.value() == 404) null else throw e
    }
}
```

- 404 응답 시 `null` 반환 (리소스 없음으로 처리)
- 그 외 HTTP 오류는 예외를 그대로 전파

---

## 5. Pact 계약 테스트 상세

### 5.1 CDC 개념 설명

CDC 테스트의 핵심 원칙은 다음과 같습니다:

1. **Consumer가 계약을 정의한다**: API를 실제로 사용하는 쪽이 필요한 필드와 동작을 명세합니다. 필요하지 않은 필드는 계약에 포함하지 않아 Provider의 자유도를 높입니다.

2. **Provider State**: Provider가 특정 상태일 때 어떤 응답을 반환해야 하는지를 명시합니다. 예: "세션 ID 1이 존재함" 상태에서 `GET /sessions/1`을 요청하면 200을 반환해야 한다.

3. **Pact 파일**: Consumer 테스트가 통과하면 JSON 파일로 계약이 생성됩니다. Provider는 이 파일을 읽어 자신이 계약을 이행하는지 검증합니다.

**Pact 파일의 역할 흐름:**
```
Consumer 테스트 실행
  -> MockServer가 Consumer의 HTTP 호출을 검증
  -> 테스트 통과 시 계약 파일(JSON) 생성
  -> (선택) Pact Broker에 게시
  -> Provider 테스트가 계약 파일 로드
  -> 실제 Provider 서버에 계약대로 요청
  -> 응답이 계약과 일치하는지 검증
```

### 5.2 Consumer 테스트 (AttendeeService)

**파일 위치**: `attendee-service/src/test/kotlin/com/conference/attendee/pact/SessionServiceConsumerPactTest.kt`

**클래스 어노테이션:**
```kotlin
@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)
```

- `@ExtendWith(PactConsumerTestExt::class)`: JUnit 5에 Pact Consumer 확장 등록
- `@PactTestFor(providerName = "SessionService", ...)`: 이 Consumer가 상대하는 Provider 이름을 지정. Pact 파일에 `"provider": { "name": "SessionService" }` 로 기록됨
- `pactVersion = PactSpecVersion.V3`: Pact 명세 버전 3 사용

**계약 1: 세션 단건 조회 (200)**

```kotlin
@Pact(consumer = "AttendeeService")
fun getSessionPact(builder: PactDslWithProvider): RequestResponsePact {
    return builder
        .given("세션 ID 1이 존재함")
        .uponReceiving("세션 1 조회 요청")
        .path("/sessions/1")
        .method("GET")
        .headers("Accept", "application/json")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body(
            PactDslJsonBody()
                .integerType("id", 1L)
                .stringType("title", "gRPC로 마이크로서비스 구축하기")
                .stringType("speaker", "장호")
                .stringType("description", "...")
                .stringType("dateTime", "2024-09-15T10:00:00")
        )
        .toPact()
}
```

- `given("세션 ID 1이 존재함")`: Provider State 이름. Provider 검증 시 `@State` 핸들러와 매칭됨
- `integerType("id", ...)`: 값이 정확히 일치할 필요 없이 정수 타입이면 통과 (유연한 매칭)
- `stringType("title", ...)`: 문자열 타입이면 통과. 예시 값은 Pact 파일에 기록됨

**계약 2: 세션 목록 조회 (200)**

```kotlin
@Pact(consumer = "AttendeeService")
fun getSessionsPact(builder: PactDslWithProvider): RequestResponsePact {
    val responseBody = PactDslJsonBody()
        .integerType("total")
        .eachLike("data", 3)
            .integerType("id")
            .stringType("title", "gRPC로 마이크로서비스 구축하기")
            .stringType("speaker", "장호")
            .closeArray()!! as PactDslJsonBody
    // ...
}
```

- `eachLike("data", 3)`: `data` 배열의 각 원소가 이 구조를 따른다. 3은 예시 원소 개수
- 배열 내 필드도 타입 매칭 적용

**계약 3: 존재하지 않는 세션 (404)**

```kotlin
@Pact(consumer = "AttendeeService")
fun getSessionNotFoundPact(builder: PactDslWithProvider): RequestResponsePact {
    return builder
        .given("세션 ID 999가 존재하지 않음")
        .uponReceiving("존재하지 않는 세션 조회 요청")
        .path("/sessions/999")
        .method("GET")
        .headers("Accept", "application/json")
        .willRespondWith()
        .status(404)
        .toPact()
}
```

- 응답 바디 없이 상태 코드(404)만 계약에 포함

**MockServer 동작 원리:**

테스트 메서드에 `MockServer` 파라미터를 주입받아 실제 URL로 `RestClient`를 구성합니다:

```kotlin
@Test
@PactTestFor(pactMethod = "getSessionPact")
fun `세션 단건 조회 시 올바른 세션 정보를 반환한다`(mockServer: MockServer) {
    val restClient = RestClient.builder()
        .baseUrl(mockServer.getUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
    val client = SessionClient(restClient)

    val session = client.getSession(1)

    assertThat(session).isNotNull
    assertThat(session!!.id).isEqualTo(1)
    assertThat(session.title).isEqualTo("gRPC로 마이크로서비스 구축하기")
    assertThat(session.speaker).isEqualTo("장호")
}
```

Pact가 임시 MockServer를 실행하고, 해당 서버는 `@Pact` 메서드에 정의된 계약대로 응답합니다. Consumer가 실제로 그 URL을 올바르게 호출하는지 검증됩니다.

**Pact 파일 생성 위치**: `attendee-service/build/pacts/AttendeeService-SessionService.json`

### 5.3 Provider 검증 테스트 (SessionService)

**파일 위치**: `session-service/src/test/kotlin/com/conference/session/pact/SessionServiceProviderPactTest.kt`

**클래스 어노테이션:**
```kotlin
@Provider("SessionService")
@PactFolder("../attendee-service/build/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

- `@Provider("SessionService")`: 이 테스트가 검증하는 Provider 이름. Consumer 테스트의 `providerName`과 반드시 일치해야 함
- `@PactFolder("../attendee-service/build/pacts")`: Consumer가 생성한 Pact 파일을 로컬 경로에서 읽음
- `@SpringBootTest(...RANDOM_PORT)`: 실제 Spring Boot 서버를 임의 포트로 기동

**HttpTestTarget 설정:**
```kotlin
@BeforeEach
fun setUp(context: PactVerificationContext) {
    context.target = HttpTestTarget("localhost", port)
}
```

랜덤 포트로 기동된 실제 서버를 검증 대상으로 지정합니다.

**테스트 실행 방식:**
```kotlin
@TestTemplate
@ExtendWith(PactVerificationInvocationContextProvider::class)
fun pactVerificationTestTemplate(context: PactVerificationContext) {
    context.verifyInteraction()
}
```

`@TestTemplate`은 Pact 파일의 각 interaction마다 한 번씩 호출됩니다. 3개의 계약이 있으면 이 메서드가 3회 실행됩니다.

**@State 핸들러:**
```kotlin
@State("세션 ID 1이 존재함")
fun sessionWithId1Exists() {
    // SessionStore 초기 데이터에 ID 1이 이미 존재하므로 추가 설정 불필요
}

@State("세션 목록이 존재함")
fun sessionsExist() {
    // 초기 데이터 3건이 이미 로드되어 있으므로 추가 설정 불필요
}

@State("세션 ID 999가 존재하지 않음")
fun sessionWithId999DoesNotExist() {
    // 999번 세션은 존재하지 않으므로 추가 설정 불필요
}
```

Consumer가 정의한 각 Provider State 이름에 대응하는 메서드입니다. 필요하면 이 메서드에서 테스트 데이터를 준비합니다. 이 프로젝트에서는 인메모리 초기 데이터로 이미 충족되므로 별도 설정이 없습니다.

### 5.4 Pact 파일 분석

**파일 위치**: `attendee-service/build/pacts/AttendeeService-SessionService.json`

```json
{
  "consumer": { "name": "AttendeeService" },
  "provider": { "name": "SessionService" },
  "metadata": {
    "pact-jvm": { "version": "4.6.14" },
    "pactSpecification": { "version": "3.0.0" }
  },
  "interactions": [ ... ]
}
```

**Interaction 구조 (세션 단건 조회):**
```json
{
  "description": "세션 1 조회 요청",
  "providerStates": [{ "name": "세션 ID 1이 존재함" }],
  "request": {
    "method": "GET",
    "path": "/sessions/1",
    "headers": { "Accept": "application/json" }
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "body": {
      "id": 1,
      "title": "gRPC로 마이크로서비스 구축하기",
      "speaker": "장호",
      "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
      "dateTime": "2024-09-15T10:00:00"
    },
    "matchingRules": {
      "body": {
        "$.id":          { "matchers": [{ "match": "integer" }] },
        "$.title":       { "matchers": [{ "match": "type" }] },
        "$.speaker":     { "matchers": [{ "match": "type" }] },
        "$.description": { "matchers": [{ "match": "type" }] },
        "$.dateTime":    { "matchers": [{ "match": "type" }] }
      }
    }
  }
}
```

**matchingRules 해석:**
- `"match": "integer"`: 실제 값이 정수이면 통과 (값 자체는 무관)
- `"match": "type"`: 실제 값이 같은 타입(문자열)이면 통과

**Interaction 구조 (세션 목록 조회):**

목록 조회 계약은 `data` 배열에 `matchingRules`와 `generators`가 함께 포함됩니다:

```json
"generators": {
  "body": {
    "$.data[*].id": { "type": "RandomInt", "min": 0, "max": 2147483647 },
    "$.total":      { "type": "RandomInt", "min": 0, "max": 2147483647 }
  }
}
```

Provider 검증 시 `generators`는 무시되며 실제 서버의 응답값이 `matchingRules`에 의해 검증됩니다.

---

## 6. 인프라

### 6.1 Docker Compose

`docker-compose.yml`은 Pact Broker 운영에 필요한 두 서비스를 정의합니다.

**PostgreSQL:**
```yaml
postgres:
  image: postgres:17-alpine
  environment:
    POSTGRES_USER: pact
    POSTGRES_PASSWORD: pact
    POSTGRES_DB: pact_broker
  ports:
    - "5432:5432"
  volumes:
    - postgres-data:/var/lib/postgresql/data
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U pact"]
    interval: 5s
    timeout: 5s
    retries: 5
```

- Pact Broker의 계약 및 검증 결과를 영속적으로 저장
- `postgres-data` named volume으로 컨테이너 재시작 시에도 데이터 유지
- healthcheck 통과 후 Pact Broker가 기동됨 (depends_on 조건)

**Pact Broker:**
```yaml
pact-broker:
  image: pactfoundation/pact-broker:latest
  depends_on:
    postgres:
      condition: service_healthy
  environment:
    PACT_BROKER_DATABASE_URL: "postgres://pact:pact@postgres/pact_broker"
    PACT_BROKER_BASIC_AUTH_USERNAME: pact
    PACT_BROKER_BASIC_AUTH_PASSWORD: pact
    PACT_BROKER_LOG_LEVEL: INFO
  ports:
    - "9292:9292"
```

- PostgreSQL이 정상 상태일 때만 기동
- Basic Auth: `pact` / `pact`

### 6.2 Pact Broker 역할

| 역할 | 설명 |
|------|------|
| 계약 저장소 | Consumer가 게시한 Pact 파일을 버전별로 저장 |
| 검증 결과 관리 | Provider 검증 결과를 연결하여 호환성 매트릭스 생성 |
| can-i-deploy | 특정 Consumer/Provider 버전이 안전하게 배포 가능한지 판정 |
| 웹 UI | 계약 현황 시각화 |

**접속 정보:**
- URL: `http://localhost:9292`
- 사용자: `pact` / 비밀번호: `pact`

---

## 7. 사용법 (Step-by-Step)

### 7.1 로컬 개발 환경 구성

**사전 요구사항:**

```bash
# JDK 21 설치 확인
java -version
# 출력 예: openjdk version "21.0.x"

# Docker 설치 확인
docker --version
docker compose version
```

**프로젝트 클론:**
```bash
git clone <repository-url>
cd pact-conference-demo
```

### 7.2 빌드

```bash
./gradlew build
```

전체 빌드는 컴파일, 단위 테스트, Pact Consumer 테스트를 모두 실행합니다. 최초 빌드 시 Gradle 의존성을 다운로드하므로 시간이 걸릴 수 있습니다.

### 7.3 Pact 테스트 실행 (전체 흐름)

#### Step 1: Consumer 테스트 실행

```bash
./gradlew :attendee-service:test
```

**내부 동작:**
1. Pact 라이브러리가 임시 MockServer를 시작
2. `@Pact` 어노테이션이 붙은 메서드가 계약 명세를 정의
3. 각 `@Test` 메서드가 MockServer에 실제 HTTP 요청을 전송
4. MockServer가 계약 명세와 요청이 일치하는지 검증
5. 검증 성공 시 `attendee-service/build/pacts/` 아래에 JSON 파일 생성

**결과 확인:**
```bash
ls attendee-service/build/pacts/
# AttendeeService-SessionService.json
```

테스트 보고서: `attendee-service/build/reports/tests/test/index.html`

#### Step 2: Provider 검증 테스트 실행

Consumer 테스트가 먼저 실행되어 Pact 파일이 생성된 후에 실행해야 합니다.

```bash
./gradlew :session-service:test
```

**내부 동작:**
1. Spring Boot 서버가 랜덤 포트로 기동
2. `@PactFolder("../attendee-service/build/pacts")`에서 Pact 파일 로드
3. Pact 파일의 각 interaction에 대해 `@TestTemplate` 메서드가 한 번씩 실행
4. `@State` 핸들러로 Provider State 설정 (필요한 경우)
5. 실제 서버에 계약대로 HTTP 요청 전송
6. 응답이 `matchingRules`를 만족하는지 검증

**결과 확인:**

테스트 보고서: `session-service/build/reports/tests/test/index.html`

**순서 의존성 주의:** Provider 테스트(`session-service:test`)는 반드시 Consumer 테스트(`attendee-service:test`) 이후에 실행해야 합니다. Consumer 테스트가 생성하는 `build/pacts/` 파일이 없으면 Provider 테스트가 실패합니다.

#### Step 3: Pact Broker 사용 (선택)

로컬 파일 공유 대신 Broker를 통해 계약을 공유하는 워크플로우입니다.

```bash
# 1. Pact Broker 시작
docker compose up -d

# 2. Broker 상태 확인
curl http://localhost:9292/diagnostic/status/heartbeat

# 3. Consumer 테스트 실행 (Pact 파일 생성)
./gradlew :attendee-service:test

# 4. Pact 파일을 Broker에 게시
# pact-jvm-provider Gradle 태스크 또는 Pact CLI 사용
# 예: PACT_BROKER_BASE_URL=http://localhost:9292로 설정 후 pactPublish

# 5. Broker UI에서 계약 확인
open http://localhost:9292

# 6. Provider 검증 (Broker에서 Pact 파일을 읽도록 설정 필요)
./gradlew :session-service:test

# 7. Broker에서 검증 결과 확인
open http://localhost:9292
```

### 7.4 서비스 실행

**Session Service 단독 실행 (포트 8081):**
```bash
./gradlew :session-service:bootRun
```

기동 후 확인:
```bash
curl http://localhost:8081/sessions
# {"data":[...],"total":3}

curl http://localhost:8081/sessions/1
# {"id":1,"title":"gRPC로 마이크로서비스 구축하기","speaker":"장호",...}
```

**Attendee Service 단독 실행 (포트 8080):**
```bash
./gradlew :attendee-service:bootRun
```

기동 후 확인:
```bash
curl http://localhost:8080/attendees
# {"data":[...],"total":3}
```

**두 서비스 통합 실행:**

터미널 2개를 열어 각각 실행합니다.

```bash
# 터미널 1
./gradlew :session-service:bootRun

# 터미널 2
./gradlew :attendee-service:bootRun
```

통합 테스트:
```bash
# 참석자 1의 세션 목록 조회 (AttendeeService -> SessionService 호출)
curl http://localhost:8080/attendees/1/sessions
# {"data":[{"id":1,"title":"gRPC로 마이크로서비스 구축하기",...}],"total":3}

# 새 세션 생성
curl -X POST http://localhost:8081/sessions \
  -H "Content-Type: application/json" \
  -d '{"title":"새 세션","speaker":"김개발","description":"테스트 세션"}'
# {"id":4,"title":"새 세션","speaker":"김개발",...}

# 세션 수정
curl -X PUT http://localhost:8081/sessions/4 \
  -H "Content-Type: application/json" \
  -d '{"title":"수정된 세션","speaker":"김개발","description":"수정됨"}'
# 204 No Content

# 세션 삭제
curl -X DELETE http://localhost:8081/sessions/4
# 200 OK

# 참석자 추가
curl -X POST http://localhost:8080/attendees \
  -H "Content-Type: application/json" \
  -d '{"givenName":"Alice","surname":"Kim","email":"alice@example.com"}'
# {"id":4,"givenName":"Alice","surname":"Kim","email":"alice@example.com"}
```

### 7.5 새로운 계약 추가하기

**시나리오**: `GET /sessions/{id}` 응답에 `dateTime` 필드가 없는 경우도 Consumer가 처리해야 한다.

**Consumer 측 (AttendeeService):**

`SessionServiceConsumerPactTest.kt`에 새 `@Pact` 메서드와 테스트를 추가합니다:

```kotlin
@Pact(consumer = "AttendeeService")
fun getSessionWithoutDateTimePact(builder: PactDslWithProvider): RequestResponsePact {
    return builder
        .given("세션 ID 2에 dateTime이 없음")
        .uponReceiving("dateTime 없는 세션 조회 요청")
        .path("/sessions/2")
        .method("GET")
        .headers("Accept", "application/json")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body(
            PactDslJsonBody()
                .integerType("id", 2L)
                .stringType("title", "API 게이트웨이 패턴")
                .stringType("speaker", "김현수")
                // dateTime 필드를 계약에 포함하지 않음
        )
        .toPact()
}

@Test
@PactTestFor(pactMethod = "getSessionWithoutDateTimePact")
fun `dateTime 없는 세션 조회 시 나머지 필드를 정상 반환한다`(mockServer: MockServer) {
    val restClient = RestClient.builder()
        .baseUrl(mockServer.getUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
    val client = SessionClient(restClient)

    val session = client.getSession(2)

    assertThat(session).isNotNull
    assertThat(session!!.title).isEqualTo("API 게이트웨이 패턴")
}
```

**Provider 측 (SessionService):**

`SessionServiceProviderPactTest.kt`에 새 `@State` 핸들러를 추가합니다:

```kotlin
@State("세션 ID 2에 dateTime이 없음")
fun sessionWithId2WithoutDateTime() {
    // sessionStore의 ID 2 데이터를 dateTime null로 업데이트
    sessionStore.updateSession(2, Session(title = "API 게이트웨이 패턴", speaker = "김현수"))
}
```

**계약 변경 워크플로우:**

```
1. Consumer 팀: 새 @Pact 메서드 추가 + 테스트 작성
2. Consumer 테스트 실행: ./gradlew :attendee-service:test
3. 새 계약이 Pact 파일에 추가됨 확인
4. Provider 팀: 대응하는 @State 핸들러 추가
5. Provider 테스트 실행: ./gradlew :session-service:test
6. 검증 통과 확인
```

### 7.6 CI/CD 통합 가이드

Consumer와 Provider를 독립적으로 배포하려면 Pact Broker를 중심으로 파이프라인을 구성합니다.

**GitHub Actions 예시 파이프라인 (Consumer - AttendeeService):**

```yaml
name: Attendee Service CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  consumer-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Consumer Pact Tests
        run: ./gradlew :attendee-service:test

      - name: Publish Pact to Broker
        run: ./gradlew :attendee-service:pactPublish
        env:
          PACT_BROKER_BASE_URL: ${{ secrets.PACT_BROKER_URL }}
          PACT_BROKER_USERNAME: ${{ secrets.PACT_BROKER_USERNAME }}
          PACT_BROKER_PASSWORD: ${{ secrets.PACT_BROKER_PASSWORD }}
          PACT_CONSUMER_VERSION: ${{ github.sha }}

      - name: Can I Deploy?
        run: |
          pact-broker can-i-deploy \
            --pacticipant AttendeeService \
            --version ${{ github.sha }} \
            --to-environment production \
            --broker-base-url ${{ secrets.PACT_BROKER_URL }}
```

**GitHub Actions 예시 파이프라인 (Provider - SessionService):**

```yaml
name: Session Service CI

on:
  push:
    branches: [ main ]
  pull_request:

jobs:
  provider-verification:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run Provider Pact Verification
        run: ./gradlew :session-service:test
        env:
          PACT_BROKER_BASE_URL: ${{ secrets.PACT_BROKER_URL }}
          PACT_PROVIDER_VERSION: ${{ github.sha }}

      - name: Can I Deploy?
        run: |
          pact-broker can-i-deploy \
            --pacticipant SessionService \
            --version ${{ github.sha }} \
            --to-environment production \
            --broker-base-url ${{ secrets.PACT_BROKER_URL }}
```

**Consumer/Provider 빌드 순서:**

```
Consumer 빌드 (AttendeeService)
  1. 단위 테스트 + Consumer Pact 테스트
  2. Pact 파일 Broker 게시
  3. can-i-deploy 게이트 통과 시 배포

Provider 빌드 (SessionService) - 독립 파이프라인
  1. 단위 테스트
  2. Provider 검증 (Broker에서 최신 Pact 파일 수신)
  3. 검증 결과를 Broker에 기록
  4. can-i-deploy 게이트 통과 시 배포
```

`can-i-deploy` 게이트는 Broker에 기록된 검증 결과를 기반으로 해당 버전의 배포 가능 여부를 판정합니다.

---

## 8. 트러블슈팅

### 8.1 흔한 오류

**Consumer 테스트 실패: "Actual interactions do not match expected"**

원인: `SessionClient`가 실제로 전송하는 요청(헤더, path, method)이 `@Pact` 메서드에 정의된 계약과 다름.

확인 사항:
- `Accept` 헤더가 Consumer 테스트 MockServer 설정과 일치하는지 확인
- `SessionClient`의 URI 패턴(`/sessions/{id}`)이 계약의 `path`와 일치하는지 확인

**Provider 검증 실패: "No pact files found"**

원인: `@PactFolder("../attendee-service/build/pacts")`에 파일이 없음.

해결: Consumer 테스트를 먼저 실행합니다.
```bash
./gradlew :attendee-service:test
./gradlew :session-service:test
```

**Provider 검증 실패: "No @State handler found for 'X'"**

원인: Consumer 테스트의 `.given("상태 이름")`에 대응하는 `@State("상태 이름")` 메서드가 Provider 테스트에 없음.

해결: `SessionServiceProviderPactTest.kt`에 정확히 같은 문자열로 `@State` 메서드를 추가합니다.

**Pact 파일이 생성되지 않음**

원인: Consumer 테스트가 실패하면 파일이 생성되지 않습니다.

확인 사항:
```bash
./gradlew :attendee-service:test --info 2>&1 | grep -i pact
```

### 8.2 디버깅 팁

**Pact 로그 레벨 설정:**

`attendee-service/src/test/resources/logback-test.xml` (없으면 생성):
```xml
<configuration>
  <logger name="au.com.dius.pact" level="DEBUG"/>
</configuration>
```

**Pact 파일 직접 확인:**
```bash
cat attendee-service/build/pacts/AttendeeService-SessionService.json | python3 -m json.tool
```

**MockServer 포트 확인 (Consumer 테스트):**

테스트 실행 중 로그에서 `MockServer started on port XXXXX` 형태로 출력됩니다.

**Provider 테스트 단독 실행:**
```bash
./gradlew :session-service:test --tests "com.conference.session.pact.SessionServiceProviderPactTest" --info
```

---

## 9. 확장 가이드

### 9.1 새 서비스 추가

**시나리오: CFP(Call For Papers, 세션 제안 투표) 서비스 추가**

```
[CFPService:8082] --REST--> [SessionService:8081] (세션 목록 조회)
                 |
                 +-------> [AttendeeService:8080] (참석자 목록 조회)
```

추가 절차:
1. `settings.gradle.kts`에 `"cfp-service"` 모듈 추가
2. `cfp-service/build.gradle.kts`에 `common` 의존성 및 `pact.consumer` 의존성 추가
3. `CFPServiceConsumerPactTest`에서 SessionService, AttendeeService 각각에 대한 계약 정의
4. SessionService, AttendeeService의 Provider 테스트에 CFP 서비스용 `@State` 핸들러 추가

### 9.2 비동기 계약 테스트

현재 데모는 HTTP 요청-응답(동기) 방식의 계약 테스트만 포함합니다. 메시지 기반(비동기) 통신에는 **Message Pact**를 사용합니다.

Pact 4.x에서 Message Pact 의존성:
```kotlin
testImplementation("au.com.dius.pact.consumer:junit5:4.6.14")
```

Consumer 측에서 `@PactTestFor(providerType = ProviderType.ASYNCH)`로 메시지 계약을 정의하고, Provider 측에서 메시지 핸들러를 등록하여 검증합니다.

### 9.3 Provider Contract (Spring Cloud Contract)

CDC(Pact)와 Provider Contract(Spring Cloud Contract)는 반대 방향의 접근법입니다.

| 구분 | Pact (CDC) | Spring Cloud Contract |
|------|-----------|----------------------|
| 계약 소유자 | Consumer | Provider |
| 계약 위치 | Consumer 코드베이스 | Provider 코드베이스 |
| 주도권 | Consumer 팀 | Provider 팀 |
| 적합한 상황 | Consumer 팀이 여럿, 독립성 강조 | Provider가 API 스펙 주도 |

두 방식 모두 E2E 테스트보다 빠른 피드백을 제공하며, 조직 구조와 팀 간 협업 방식에 따라 선택합니다.

---

## 10. 참고자료

- **책**: "Mastering API Architecture" (James Gough, Daniel Bryant, Matthew Auburn) - O'Reilly Media
- **Pact 공식 문서**: https://docs.pact.io
- **Pact JVM**: https://github.com/pact-foundation/pact-jvm
- **Pact Broker**: https://docs.pact.io/pact_broker
- **Pact Foundation GitHub**: https://github.com/pact-foundation
- **Spring Boot Testing**: https://docs.spring.io/spring-boot/docs/current/reference/html/test-auto-configuration.html
- **can-i-deploy 문서**: https://docs.pact.io/pact_broker/can_i_deploy

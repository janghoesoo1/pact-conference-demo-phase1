# Pact Conference Demo 사용자 설명서

## 목차

1. [이 프로젝트는 무엇인가?](#1-이-프로젝트는-무엇인가)
2. [시작하기 전에](#2-시작하기-전에)
3. [프로젝트 구조 이해하기](#3-프로젝트-구조-이해하기)
4. [서비스 실행하기](#4-서비스-실행하기)
5. [Pact 계약 테스트 따라하기 (핵심!)](#5-pact-계약-테스트-따라하기-핵심)
6. [직접 계약 추가해보기 (실습 가이드)](#6-직접-계약-추가해보기-실습-가이드)
7. [Pact Broker 사용하기 (선택)](#7-pact-broker-사용하기-선택)
8. [OpenAPI 문서 확인](#8-openapi-문서-확인)
9. [자주 묻는 질문 (FAQ)](#9-자주-묻는-질문-faq)
10. [트러블슈팅](#10-트러블슈팅)
11. [다음 단계](#11-다음-단계)

---

## 1. 이 프로젝트는 무엇인가?

### 1.1 배경

이 프로젝트는 O'Reilly에서 출판된 **"Mastering API Architecture"** (한빛미디어 한국어판: "마스터링 API 아키텍처")의 내용을 기반으로 만든 학습용 데모 프로젝트입니다.

책에서는 컨퍼런스(학회/세미나) 시스템을 예시로 사용하여 API 아키텍처의 다양한 패턴을 설명합니다. 이 프로젝트는 그 중에서도 **계약 테스트(Contract Testing)** 개념을 직접 실행해볼 수 있도록 Kotlin + Spring Boot로 구현한 것입니다.

이 프로젝트를 만든 이유는 다음과 같습니다:

- 책의 이론을 코드로 직접 확인하고 싶었습니다
- Pact 프레임워크의 Consumer-Driven Contract 테스트를 JVM 환경에서 처음부터 끝까지 따라해볼 수 있는 예제가 필요했습니다
- 초보자도 계약 테스트의 전체 흐름을 단계별로 이해할 수 있는 참고 자료를 만들고 싶었습니다

### 1.2 핵심 개념: 계약 테스트 (Contract Testing)

#### 마이크로서비스 환경의 문제

마이크로서비스 아키텍처에서는 서비스 A가 서비스 B의 API를 HTTP로 호출합니다. 예를 들어, 참석자 서비스(Attendee Service)가 세션 서비스(Session Service)에게 "세션 목록을 달라"고 요청합니다.

이때 문제가 생길 수 있습니다:

- 세션 서비스가 응답 JSON의 필드명을 `title`에서 `name`으로 바꾸면?
- 참석자 서비스가 기대하는 필드가 사라지면?
- 양쪽 팀이 서로 모르는 상태에서 API를 변경하면?

이런 문제를 잡으려면 두 서비스를 함께 띄워서 E2E(End-to-End) 테스트를 돌려야 합니다. 하지만 E2E 테스트에는 심각한 한계가 있습니다:

| E2E 테스트의 한계 | 설명 |
|-------------------|------|
| 느립니다 | 모든 서비스를 함께 띄워야 하므로 시간이 오래 걸립니다 |
| 불안정합니다 | 네트워크, DB, 외부 서비스 등 실패 요인이 많습니다 |
| 비용이 큽니다 | CI/CD 환경에서 전체 인프라를 매번 구성하기 어렵습니다 |
| 원인 파악이 어렵습니다 | 실패 시 어떤 서비스가 문제인지 찾기 힘듭니다 |

#### 계약 테스트가 해결하는 것

계약 테스트는 **각 서비스를 독립적으로 테스트**하면서도, **서비스 간 API 호환성을 보장**합니다.

핵심 아이디어는 간단합니다: 서비스 사이에 "계약서"를 만들어두고, 양쪽이 각자 그 계약을 지키는지 확인합니다.

- **Consumer(소비자)**: API를 호출하는 쪽. 이 프로젝트에서는 `AttendeeService`가 Consumer입니다
- **Provider(제공자)**: API를 제공하는 쪽. 이 프로젝트에서는 `SessionService`가 Provider입니다

#### Consumer-Driven Contract (CDC)

이 프로젝트에서 사용하는 방식은 **Consumer-Driven Contract (CDC)**입니다. "소비자가 계약을 정의한다"는 뜻입니다.

왜 소비자가 계약을 정의할까요?

1. 소비자가 실제로 필요한 것만 계약에 포함합니다. Provider가 20개 필드를 반환하더라도, Consumer가 3개만 쓴다면 계약에는 3개만 들어갑니다
2. Provider가 나머지 17개 필드를 자유롭게 바꿀 수 있습니다. Consumer가 쓰지 않는 부분은 제약이 없습니다
3. 여러 Consumer가 같은 Provider를 쓸 때, 각 Consumer마다 서로 다른 계약을 만들 수 있습니다

흐름을 정리하면:

```
1. Consumer가 "나는 이런 요청을 보내고, 이런 응답을 기대해" 라는 계약을 만든다
2. 이 계약이 JSON 파일로 저장된다
3. Provider가 이 JSON 파일을 읽고, "정말로 이 계약을 지킬 수 있는가?" 를 검증한다
4. 양쪽 모두 통과하면, 독립적으로 배포해도 안전하다
```

### 1.3 이 프로젝트의 구성

이 프로젝트는 **Gradle 멀티모듈 프로젝트**로, 3개의 모듈로 구성됩니다:

| 모듈 | 역할 |
|------|------|
| `common` | 공통 도메인 모델 (Session, Attendee, ApiResponse, 예외 클래스) |
| `session-service` | 세션(발표) 정보를 관리하는 REST API 서버 (Provider) |
| `attendee-service` | 참석자 정보를 관리하고, 세션 서비스를 호출하는 REST API 서버 (Consumer) |

서비스 간 관계는 다음과 같습니다:

```
AttendeeService (Consumer)              SessionService (Provider)
    포트 8080                                포트 8081
       |                                        |
       |   GET /sessions                        |
       |   GET /sessions/{id}                   |
       | -------------------------------------->|
       |<---------------------------------------|
       |                                        |
   [참석자 CRUD]                          [세션 CRUD]
   /attendees                             /sessions
   /attendees/{id}                        /sessions/{id}
   /attendees/{id}/sessions               (POST, PUT, DELETE)
```

- `AttendeeService`는 `/attendees/{id}/sessions` 엔드포인트에서 `SessionClient`를 통해 `SessionService`의 `/sessions` API를 HTTP로 호출합니다
- `SessionService`는 독립적으로 세션 데이터를 관리하며, 누가 호출하는지 신경쓰지 않습니다
- 두 서비스 사이의 "약속"이 바로 Pact 계약(Contract)입니다

---

## 2. 시작하기 전에

### 2.1 필수 설치 프로그램

이 프로젝트를 실행하려면 아래 프로그램들이 설치되어 있어야 합니다.

#### JDK 21 이상

이 프로젝트는 `build.gradle.kts`에서 `jvmTarget = "21"`로 설정되어 있으므로 JDK 21 이상이 필요합니다.

설치 확인:
```bash
java -version
```

예상 출력 (버전 21 이상이면 됩니다):
```
openjdk version "21.0.x" ...
```

설치 방법:
- macOS: `brew install openjdk@21`
- Windows: [Adoptium](https://adoptium.net/)에서 JDK 21 다운로드
- Linux: `sudo apt install openjdk-21-jdk` (Ubuntu/Debian)

#### Docker & Docker Compose (Pact Broker 사용 시에만 필요)

Pact Broker를 로컬에서 실행하려면 Docker가 필요합니다. Pact 테스트 자체는 Docker 없이도 동작합니다.

설치 확인:
```bash
docker --version
docker compose version
```

예상 출력:
```
Docker version 24.x.x ...
Docker Compose version v2.x.x
```

설치 방법:
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치 (macOS, Windows)

#### Git

설치 확인:
```bash
git --version
```

#### (선택) IDE: IntelliJ IDEA

Kotlin 프로젝트이므로 IntelliJ IDEA를 강력히 추천합니다. Community Edition(무료)으로 충분합니다.

- [IntelliJ IDEA 다운로드](https://www.jetbrains.com/idea/)

### 2.2 프로젝트 클론

```bash
git clone https://github.com/janghoesoo1/pact-conference-demo.git
cd pact-conference-demo
```

### 2.3 빌드 확인

프로젝트 루트에서 다음 명령어를 실행합니다:

```bash
./gradlew build
```

Windows의 경우:
```bash
gradlew.bat build
```

예상 결과:
```
BUILD SUCCESSFUL in Xs
```

만약 빌드가 실패한다면 다음을 확인하세요:

| 체크 항목 | 확인 방법 |
|-----------|-----------|
| JDK 버전이 21 이상인가? | `java -version` |
| JAVA_HOME이 설정되어 있는가? | `echo $JAVA_HOME` |
| 인터넷 연결이 정상인가? | Gradle이 의존성을 다운로드해야 합니다 |
| gradlew에 실행 권한이 있는가? | `chmod +x gradlew` (macOS/Linux) |

---

## 3. 프로젝트 구조 이해하기

### 3.1 디렉토리 구조

```
pact-conference-demo/
├── build.gradle.kts                     # 루트 빌드 스크립트 (공통 설정)
├── settings.gradle.kts                  # 모듈 포함 설정
├── gradle.properties                    # Gradle 속성 (parallel=true)
├── gradlew                              # Gradle Wrapper (macOS/Linux)
├── gradlew.bat                          # Gradle Wrapper (Windows)
├── docker-compose.yml                   # Pact Broker + PostgreSQL 실행용
├── common/                              # 공통 모듈
│   └── src/main/kotlin/com/conference/common/
│       ├── model/
│       │   ├── Session.kt               # 세션 데이터 클래스
│       │   ├── Attendee.kt              # 참석자 데이터 클래스
│       │   └── ApiResponse.kt           # API 응답 래퍼
│       └── exception/
│           └── Exceptions.kt            # ResourceNotFoundException
├── session-service/                     # 세션 서비스 (Provider)
│   ├── build.gradle.kts                 # 세션 서비스 의존성
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/conference/session/
│       │   │   ├── SessionServiceApplication.kt  # Spring Boot 메인 클래스
│       │   │   ├── controller/
│       │   │   │   └── SessionController.kt      # REST 컨트롤러 (CRUD)
│       │   │   └── store/
│       │   │       └── SessionStore.kt            # 인메모리 저장소
│       │   └── resources/
│       │       └── application.yml                # 서버 설정 (포트 8081)
│       └── test/kotlin/com/conference/session/
│           ├── controller/
│           │   └── SessionControllerTest.kt       # 컨트롤러 단위 테스트
│           └── pact/
│               └── SessionServiceProviderPactTest.kt  # Provider 계약 검증
├── attendee-service/                    # 참석자 서비스 (Consumer)
│   ├── build.gradle.kts                 # 참석자 서비스 의존성
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/conference/attendee/
│       │   │   ├── AttendeeServiceApplication.kt  # Spring Boot 메인 클래스
│       │   │   ├── client/
│       │   │   │   └── SessionClient.kt           # SessionService 호출 클라이언트
│       │   │   ├── config/
│       │   │   │   └── RestClientConfig.kt        # RestClient Bean 설정
│       │   │   ├── controller/
│       │   │   │   └── AttendeeController.kt      # REST 컨트롤러 (CRUD + 세션 조회)
│       │   │   └── store/
│       │   │       └── AttendeeStore.kt           # 인메모리 저장소
│       │   └── resources/
│       │       └── application.yml                # 서버 설정 (포트 8080)
│       └── test/kotlin/com/conference/attendee/pact/
│           └── SessionServiceConsumerPactTest.kt  # Consumer 계약 정의
└── docs/                                # 문서
```

### 3.2 도메인 모델

이 프로젝트에서 사용하는 도메인 모델은 `common` 모듈에 정의되어 있으며, 두 서비스가 공유합니다.

#### Session (세션)

```kotlin
// common/src/main/kotlin/com/conference/common/model/Session.kt
package com.conference.common.model

import java.time.LocalDateTime

data class Session(
    val id: Int? = null,
    val title: String,
    val speaker: String,
    val description: String? = null,
    val dateTime: LocalDateTime? = null
)
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Int?` | 아니오 | 세션 고유 식별자. 생성 시 자동 부여됩니다 |
| `title` | `String` | 예 | 세션(발표) 제목 |
| `speaker` | `String` | 예 | 발표자 이름 |
| `description` | `String?` | 아니오 | 세션 설명 |
| `dateTime` | `LocalDateTime?` | 아니오 | 발표 일시 |

#### Attendee (참석자)

```kotlin
// common/src/main/kotlin/com/conference/common/model/Attendee.kt
package com.conference.common.model

data class Attendee(
    val id: Int? = null,
    val givenName: String,
    val surname: String,
    val email: String
)
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | `Int?` | 아니오 | 참석자 고유 식별자. 생성 시 자동 부여됩니다 |
| `givenName` | `String` | 예 | 이름 (first name) |
| `surname` | `String` | 예 | 성 (last name) |
| `email` | `String` | 예 | 이메일 주소 |

#### ApiResponse (API 응답 래퍼)

```kotlin
// common/src/main/kotlin/com/conference/common/model/ApiResponse.kt
package com.conference.common.model

data class ApiResponse<T>(
    val data: List<T>,
    val total: Int = data.size
)
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `data` | `List<T>` | 응답 데이터 목록 |
| `total` | `Int` | 총 개수. 기본값은 `data.size`로 자동 계산됩니다 |

목록 조회 API(`GET /sessions`, `GET /attendees`)의 응답을 감싸는 래퍼입니다. 이 형태 덕분에 응답 JSON이 항상 `{"data": [...], "total": N}` 구조를 갖습니다.

#### ResourceNotFoundException (예외)

```kotlin
// common/src/main/kotlin/com/conference/common/exception/Exceptions.kt
package com.conference.common.exception

class ResourceNotFoundException(message: String) : RuntimeException(message)
```

존재하지 않는 리소스를 조회할 때 발생하는 예외입니다. `SessionController`와 `AttendeeController`에서 이 예외를 HTTP 404 응답으로 변환합니다.

#### 초기 데이터

두 서비스 모두 인메모리 저장소를 사용하며, 서버 시작 시 아래 데이터가 미리 들어 있습니다.

**세션 초기 데이터 (SessionStore)**:

| id | title | speaker | description | dateTime |
|----|-------|---------|-------------|----------|
| 1 | gRPC로 마이크로서비스 구축하기 | 장호 | gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다. | 2024-09-15T10:00 |
| 2 | API 게이트웨이 패턴 | 김현수 | API 게이트웨이의 다양한 패턴과 구현 전략을 살펴봅니다. | 2024-09-15T14:00 |
| 3 | 계약 테스트 실전 | 이서연 | Pact 프레임워크를 사용한 CDC 테스트 실전 사례를 공유합니다. | 2024-09-16T10:00 |

**참석자 초기 데이터 (AttendeeStore)**:

| id | givenName | surname | email |
|----|-----------|---------|-------|
| 1 | Jim | Gough | gough@mail.com |
| 2 | Matt | Auburn | auburn@mail.com |
| 3 | Daniel | Bryant | bryant@mail.com |

참고: Jim Gough, Matt Auburn, Daniel Bryant는 "Mastering API Architecture" 책의 저자입니다.

### 3.3 의존성 구조

```
common (공통 모듈)
   ^               ^
   |               |
   | (컴파일 의존)   | (컴파일 의존)
   |               |
attendee-service   session-service
   |
   | (런타임 HTTP 호출: SessionClient -> http://localhost:8081)
   |
   +---> session-service
   |
   | (테스트 시 Pact 파일 전달)
   |     attendee-service/build/pacts/AttendeeService-SessionService.json
   |
   +---> session-service (Provider 검증에서 읽음)
```

빌드 의존성(`build.gradle.kts`):
- `attendee-service`와 `session-service` 모두 `implementation(project(":common"))`으로 common 모듈에 의존합니다
- `session-service`의 테스트는 `dependsOn(":attendee-service:test")`로 설정되어 있어, session-service 테스트 실행 전에 attendee-service 테스트가 먼저 실행됩니다. 이는 Consumer 테스트가 Pact JSON 파일을 생성해야 Provider 검증에서 읽을 수 있기 때문입니다

주요 라이브러리 버전:
- Kotlin: 1.9.25
- Spring Boot: 3.3.6
- Pact JVM: 4.6.14
- springdoc-openapi: 2.3.0
- MockK: 1.13.13
- SpringMockK: 4.0.2

---

## 4. 서비스 실행하기

### 4.1 Session Service 실행

터미널에서 프로젝트 루트 디렉토리로 이동한 뒤, 다음 명령어를 실행합니다:

```bash
./gradlew :session-service:bootRun
```

어떤 일이 일어나는가:

1. Gradle이 common 모듈과 session-service 모듈을 컴파일합니다
2. Spring Boot 내장 서버(Tomcat)가 시작됩니다
3. `application.yml`에 설정된 포트 **8081**에서 HTTP 요청을 받기 시작합니다
4. `SessionStore`에 3개의 초기 세션 데이터가 로드됩니다

로그에서 아래와 같은 내용을 확인할 수 있습니다:
```
Started SessionServiceApplicationKt in X.XXX seconds
Tomcat started on port 8081
```

동작 확인:
```bash
curl -s http://localhost:8081/sessions | jq .
```

정상이라면 세션 목록 JSON이 출력됩니다.

### 4.2 Attendee Service 실행

**새 터미널 창을 열고** (Session Service를 종료하지 마세요), 다음 명령어를 실행합니다:

```bash
./gradlew :attendee-service:bootRun
```

주의사항:

- Session Service가 먼저 실행되어 있어야 합니다
- `RestClientConfig.kt`에서 `@Value("\${session-service.url:http://localhost:8081}")`로 설정된 URL을 사용하여 `SessionClient`가 Session Service에 연결합니다
- `application.yml`에 `session-service.url: http://localhost:8081`이 명시되어 있습니다
- Session Service가 실행 중이지 않으면 `/attendees/{id}/sessions` 엔드포인트 호출 시 연결 오류가 발생합니다

동작 확인:
```bash
curl -s http://localhost:8080/attendees | jq .
```

### 4.3 두 서비스 통합 확인

두 서비스가 모두 실행된 상태에서, 아래 curl 명령어를 통해 모든 API를 테스트할 수 있습니다.

참고: `jq`는 JSON을 보기 좋게 포맷해주는 도구입니다. 설치되어 있지 않다면 `| jq .` 부분을 제거해도 됩니다.

#### 세션 API (Session Service - 포트 8081)

**세션 목록 조회**:
```bash
curl -s http://localhost:8081/sessions | jq .
```

기대 응답:
```json
{
  "data": [
    {
      "id": 1,
      "title": "gRPC로 마이크로서비스 구축하기",
      "speaker": "장호",
      "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
      "dateTime": "2024-09-15T10:00:00"
    },
    {
      "id": 2,
      "title": "API 게이트웨이 패턴",
      "speaker": "김현수",
      "description": "API 게이트웨이의 다양한 패턴과 구현 전략을 살펴봅니다.",
      "dateTime": "2024-09-15T14:00:00"
    },
    {
      "id": 3,
      "title": "계약 테스트 실전",
      "speaker": "이서연",
      "description": "Pact 프레임워크를 사용한 CDC 테스트 실전 사례를 공유합니다.",
      "dateTime": "2024-09-16T10:00:00"
    }
  ],
  "total": 3
}
```

**세션 단건 조회**:
```bash
curl -s http://localhost:8081/sessions/1 | jq .
```

기대 응답:
```json
{
  "id": 1,
  "title": "gRPC로 마이크로서비스 구축하기",
  "speaker": "장호",
  "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
  "dateTime": "2024-09-15T10:00:00"
}
```

**세션 생성**:
```bash
curl -s -X POST http://localhost:8081/sessions \
  -H "Content-Type: application/json" \
  -d '{"title":"새 세션","speaker":"홍길동","description":"설명"}' | jq .
```

기대 응답 (HTTP 201 Created):
```json
{
  "id": 4,
  "title": "새 세션",
  "speaker": "홍길동",
  "description": "설명",
  "dateTime": null
}
```

참고: 응답 헤더에 `Location: http://localhost:8081/sessions/4`가 포함됩니다. `SessionController.createSession()`에서 `ServletUriComponentsBuilder`를 사용하여 생성된 리소스의 URI를 Location 헤더로 반환합니다.

**세션 수정**:
```bash
curl -s -X PUT http://localhost:8081/sessions/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"수정된 세션","speaker":"장호","description":"수정됨"}'
```

기대 응답: HTTP 204 No Content (응답 본문 없음)

수정이 반영되었는지 확인:
```bash
curl -s http://localhost:8081/sessions/1 | jq .
```

**세션 삭제**:
```bash
curl -s -X DELETE http://localhost:8081/sessions/1
```

기대 응답: HTTP 200 OK (응답 본문 없음)

삭제 후 조회하면 404가 반환됩니다:
```bash
curl -s http://localhost:8081/sessions/1 | jq .
```

**존재하지 않는 세션 조회 (404)**:
```bash
curl -s http://localhost:8081/sessions/999 | jq .
```

기대 응답 (HTTP 404 Not Found):
```json
{
  "error": "Session with id 999 not found"
}
```

이 응답은 `SessionController`의 `@ExceptionHandler(ResourceNotFoundException::class)` 메서드가 `ResourceNotFoundException`을 잡아서 `{"error": "..."}`형태의 JSON으로 변환한 결과입니다.

#### 참석자 API (Attendee Service - 포트 8080)

**참석자 목록 조회**:
```bash
curl -s http://localhost:8080/attendees | jq .
```

기대 응답:
```json
{
  "data": [
    {
      "id": 1,
      "givenName": "Jim",
      "surname": "Gough",
      "email": "gough@mail.com"
    },
    {
      "id": 2,
      "givenName": "Matt",
      "surname": "Auburn",
      "email": "auburn@mail.com"
    },
    {
      "id": 3,
      "givenName": "Daniel",
      "surname": "Bryant",
      "email": "bryant@mail.com"
    }
  ],
  "total": 3
}
```

**참석자 단건 조회**:
```bash
curl -s http://localhost:8080/attendees/1 | jq .
```

기대 응답:
```json
{
  "id": 1,
  "givenName": "Jim",
  "surname": "Gough",
  "email": "gough@mail.com"
}
```

**참석자의 세션 목록 조회 (서비스 간 통신!)**:
```bash
curl -s http://localhost:8080/attendees/1/sessions | jq .
```

이 요청이 특별한 이유: `AttendeeController.getAttendeeSessions()` 메서드가 내부적으로 `SessionClient.getSessions()`를 호출하고, 이것이 `http://localhost:8081/sessions`로 실제 HTTP 요청을 보냅니다. 즉, 두 서비스 사이에 실제 네트워크 통신이 발생합니다.

기대 응답:
```json
{
  "data": [
    {
      "id": 1,
      "title": "gRPC로 마이크로서비스 구축하기",
      "speaker": "장호",
      "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
      "dateTime": "2024-09-15T10:00:00"
    },
    {
      "id": 2,
      "title": "API 게이트웨이 패턴",
      "speaker": "김현수",
      "description": "API 게이트웨이의 다양한 패턴과 구현 전략을 살펴봅니다.",
      "dateTime": "2024-09-15T14:00:00"
    },
    {
      "id": 3,
      "title": "계약 테스트 실전",
      "speaker": "이서연",
      "description": "Pact 프레임워크를 사용한 CDC 테스트 실전 사례를 공유합니다.",
      "dateTime": "2024-09-16T10:00:00"
    }
  ],
  "total": 3
}
```

존재하지 않는 참석자의 세션을 조회하면 404가 반환됩니다:
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/attendees/999/sessions
# 출력: 404
```

**참석자 등록**:
```bash
curl -s -X POST http://localhost:8080/attendees \
  -H "Content-Type: application/json" \
  -d '{"givenName":"길동","surname":"홍","email":"hong@test.com"}' | jq .
```

기대 응답 (HTTP 201 Created):
```json
{
  "id": 4,
  "givenName": "길동",
  "surname": "홍",
  "email": "hong@test.com"
}
```

**참석자 수정**:
```bash
curl -s -X PUT http://localhost:8080/attendees/1 \
  -H "Content-Type: application/json" \
  -d '{"givenName":"James","surname":"Gough","email":"james@mail.com"}'
```

기대 응답: HTTP 204 No Content

**참석자 삭제**:
```bash
curl -s -X DELETE http://localhost:8080/attendees/1
```

기대 응답: HTTP 200 OK

---

## 5. Pact 계약 테스트 따라하기 (핵심!)

이 장은 이 프로젝트의 가장 중요한 부분입니다. Pact를 사용한 Consumer-Driven Contract 테스트의 전체 흐름을 단계별로 설명합니다.

### 5.1 전체 흐름 이해

```
+--------------------------------------------------------------+
|                    개발자의 로컬 환경                          |
|                                                              |
|  Step 1: Consumer 테스트 실행                                 |
|  +---------------------------------------+                   |
|  |   SessionServiceConsumerPactTest      |                   |
|  |                                       |                   |
|  |  @Pact 메서드가 계약 정의              |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  Pact 라이브러리가 MockServer 시작     |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  @Test 메서드가 SessionClient로        |                   |
|  |  MockServer에 실제 HTTP 요청           |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  요청/응답이 계약과 일치하면 통과       |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  Pact JSON 파일 생성                   |                   |
|  +----------|----------------------------+                   |
|             |                                                |
|             v (파일 시스템으로 전달)                           |
|    attendee-service/build/pacts/                             |
|    AttendeeService-SessionService.json                       |
|             |                                                |
|             v                                                |
|  Step 2: Provider 검증 실행                                   |
|  +---------------------------------------+                   |
|  |  SessionServiceProviderPactTest       |                   |
|  |                                       |                   |
|  |  Pact JSON 파일 로드                   |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  Spring Boot 서버 시작 (랜덤 포트)     |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  각 계약(interaction)마다:             |                   |
|  |    1. @State 핸들러 실행 (데이터 준비) |                   |
|  |    2. 실제 서버에 HTTP 요청            |                   |
|  |    3. 응답이 matchingRules 충족?       |                   |
|  |       |                               |                   |
|  |       v                               |                   |
|  |  모두 통과하면 계약 검증 완료!         |                   |
|  +---------------------------------------+                   |
+--------------------------------------------------------------+
```

### 5.2 Step 1: Consumer 테스트 실행

```bash
./gradlew :attendee-service:test
```

#### 무엇이 일어나는가?

JUnit이 `SessionServiceConsumerPactTest` 클래스를 발견하고 실행합니다. 이 클래스는 `attendee-service/src/test/kotlin/com/conference/attendee/pact/` 디렉토리에 있습니다.

클래스 선언부를 보겠습니다:

```kotlin
@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)
class SessionServiceConsumerPactTest {
```

- `@ExtendWith(PactConsumerTestExt::class)`: JUnit 5에 Pact Consumer 테스트 확장을 등록합니다. 이 확장이 MockServer 생성, Pact 파일 저장 등을 담당합니다
- `@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)`: Provider 이름을 "SessionService"로, Pact 스펙 버전을 V3로 지정합니다

이 클래스에는 **3개의 @Pact 메서드**(계약 정의)와 **3개의 @Test 메서드**(실제 검증)가 있습니다.

---

#### 계약 1: 세션 단건 조회 (`getSessionPact`)

계약 정의:

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
                .stringType("description", "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.")
                .stringType("dateTime", "2024-09-15T10:00:00")
        )
        .toPact()
}
```

이 코드를 한 줄씩 해석합니다:

| 코드 | 의미 |
|------|------|
| `.given("세션 ID 1이 존재함")` | 전제 조건: Provider 쪽에서 "세션 ID 1이 존재함" 상태를 만들어줘야 함 |
| `.uponReceiving("세션 1 조회 요청")` | 이 계약의 이름 (로그/리포트에 표시됨) |
| `.path("/sessions/1")` | 요청 경로 |
| `.method("GET")` | HTTP 메서드 |
| `.headers("Accept", "application/json")` | 요청 헤더 |
| `.willRespondWith()` | 여기서부터 기대하는 응답 정의 |
| `.status(200)` | HTTP 200 OK |
| `.integerType("id", 1L)` | id 필드는 **정수형**이면 OK. 1은 예시값일 뿐 |
| `.stringType("title", "...")` | title 필드는 **문자열**이면 OK. 값은 예시 |

테스트 메서드:

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

핵심 포인트:
- `@PactTestFor(pactMethod = "getSessionPact")`: 위에서 정의한 `getSessionPact` 계약을 사용합니다
- `mockServer: MockServer`: Pact가 자동으로 MockServer를 만들어 파라미터로 전달합니다
- `mockServer.getUrl()`: MockServer의 URL (예: `http://localhost:54321`)을 가져옵니다
- `SessionClient(restClient)`: 실제 운영 코드인 `SessionClient`를 사용합니다. 다만 대상 URL이 실제 서버가 아닌 MockServer입니다
- `client.getSession(1)`: SessionClient가 MockServer에 `GET /sessions/1` 요청을 보냅니다
- MockServer는 계약에 정의된 대로 응답을 반환하고, 요청이 계약과 일치하는지도 검증합니다

---

#### 계약 2: 전체 세션 목록 조회 (`getSessionsPact`)

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

    return builder
        .given("세션 목록이 존재함")
        .uponReceiving("전체 세션 목록 조회 요청")
        .path("/sessions")
        .method("GET")
        .headers("Accept", "application/json")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body(responseBody)
        .toPact()
}
```

새로운 메서드 설명:

| 코드 | 의미 |
|------|------|
| `.integerType("total")` | total 필드가 정수형이면 OK (예시값은 자동 생성) |
| `.eachLike("data", 3)` | data 필드는 배열이며, 3개의 예시 요소를 가짐. 실제 Provider가 1개든 100개든 상관없음 |
| `.closeArray()` | 배열 정의 종료 |

테스트 메서드:

```kotlin
@Test
@PactTestFor(pactMethod = "getSessionsPact")
fun `세션 목록 조회 시 세션 리스트를 반환한다`(mockServer: MockServer) {
    val restClient = RestClient.builder()
        .baseUrl(mockServer.getUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
    val client = SessionClient(restClient)

    val sessions = client.getSessions()

    assertThat(sessions).isNotEmpty
    assertThat(sessions).hasSize(3)
}
```

`SessionClient.getSessions()`는 다음과 같이 구현되어 있습니다:

```kotlin
fun getSessions(): List<Session> {
    val response = sessionRestClient.get()
        .uri("/sessions")
        .retrieve()
        .body(object : ParameterizedTypeReference<ApiResponse<Session>>() {})
    return response?.data ?: emptyList()
}
```

`ParameterizedTypeReference<ApiResponse<Session>>()`를 사용하여 `{"data": [...], "total": N}` 형태의 JSON을 `ApiResponse<Session>` 객체로 역직렬화하고, 그 안의 `data` 필드(세션 리스트)를 반환합니다.

---

#### 계약 3: 존재하지 않는 세션 조회 (`getSessionNotFoundPact`)

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

이 계약은 **응답 본문(body)을 정의하지 않습니다**. HTTP 404 상태 코드만 확인하면 됩니다.

테스트 메서드:

```kotlin
@Test
@PactTestFor(pactMethod = "getSessionNotFoundPact")
fun `존재하지 않는 세션 조회 시 null을 반환한다`(mockServer: MockServer) {
    val restClient = RestClient.builder()
        .baseUrl(mockServer.getUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
    val client = SessionClient(restClient)

    val session = client.getSession(999)

    assertThat(session).isNull()
}
```

`SessionClient.getSession()`의 에러 처리를 보면:

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

404를 받으면 `null`을 반환하고, 다른 에러는 그대로 던집니다. 이 테스트는 이 동작이 올바른지 검증합니다.

---

#### Consumer 테스트 실행 결과

모든 테스트가 통과하면, `attendee-service/build/pacts/` 디렉토리에 Pact JSON 파일이 생성됩니다.

```bash
cat attendee-service/build/pacts/AttendeeService-SessionService.json
```

파일명은 `{Consumer이름}-{Provider이름}.json` 형식입니다. 이 경우 `AttendeeService-SessionService.json`입니다.

#### 생성된 Pact 파일의 구조

```json
{
  "consumer": {
    "name": "AttendeeService"
  },
  "provider": {
    "name": "SessionService"
  },
  "interactions": [
    {
      "description": "세션 1 조회 요청",
      "providerStates": [
        {
          "name": "세션 ID 1이 존재함"
        }
      ],
      "request": {
        "method": "GET",
        "path": "/sessions/1",
        "headers": {
          "Accept": "application/json"
        }
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": "application/json"
        },
        "body": {
          "id": 1,
          "title": "gRPC로 마이크로서비스 구축하기",
          "speaker": "장호",
          "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
          "dateTime": "2024-09-15T10:00:00"
        },
        "matchingRules": {
          "body": {
            "$.id": {
              "matchers": [{ "match": "integer" }]
            },
            "$.title": {
              "matchers": [{ "match": "type" }]
            },
            "$.speaker": {
              "matchers": [{ "match": "type" }]
            },
            "$.description": {
              "matchers": [{ "match": "type" }]
            },
            "$.dateTime": {
              "matchers": [{ "match": "type" }]
            }
          }
        }
      }
    }
  ],
  "metadata": {
    "pactSpecification": {
      "version": "3.0.0"
    },
    "pact-jvm": {
      "version": "4.6.14"
    }
  }
}
```

Pact 파일의 핵심 구조를 이해합시다:

| 섹션 | 설명 |
|------|------|
| `consumer.name` | Consumer 서비스 이름: "AttendeeService" |
| `provider.name` | Provider 서비스 이름: "SessionService" |
| `interactions` | 계약 목록. 이 프로젝트에서는 3개 |
| `interactions[].description` | 각 계약의 설명 (로그에 표시됨) |
| `interactions[].providerStates` | Provider가 준비해야 할 상태 |
| `interactions[].request` | Consumer가 보내는 요청 |
| `interactions[].response` | 기대하는 응답 |
| `interactions[].response.matchingRules` | 응답 검증 규칙 |
| `metadata` | Pact 스펙 버전(3.0.0)과 pact-jvm 버전(4.6.14) |

`matchingRules` 이해하기:

- `"match": "integer"` -- `integerType()`으로 정의한 것. 해당 필드가 정수형이면 통과. 값이 1이든 42이든 상관없습니다
- `"match": "type"` -- `stringType()`으로 정의한 것. 해당 필드가 같은 타입(문자열)이면 통과. 값이 달라도 됩니다

`body`에 들어있는 값(예: "장호", 1)은 **예시값**일 뿐입니다. 실제 Provider 검증 시에는 `matchingRules`만 확인합니다.

### 5.3 Step 2: Provider 검증 실행

```bash
./gradlew :session-service:test
```

참고: `session-service`의 `build.gradle.kts`에 다음 설정이 있습니다:

```kotlin
tasks.test {
    dependsOn(":attendee-service:test")
}
```

따라서 위 명령어를 실행하면 `attendee-service:test`가 먼저 실행되어 Pact 파일이 생성된 후, `session-service:test`가 실행됩니다. 전체 테스트를 한 번에 실행하려면:

```bash
./gradlew test
```

이것만으로도 순서가 보장됩니다.

#### 무엇이 일어나는가?

JUnit이 `SessionServiceProviderPactTest` 클래스를 실행합니다. 이 클래스는 `session-service/src/test/kotlin/com/conference/session/pact/` 디렉토리에 있습니다.

```kotlin
@Provider("SessionService")
@PactFolder("../attendee-service/build/pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionServiceProviderPactTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sessionStore: SessionStore

    @BeforeEach
    fun setUp(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext) {
        context.verifyInteraction()
    }
```

각 어노테이션/메서드의 역할:

| 코드 | 의미 |
|------|------|
| `@Provider("SessionService")` | 이 테스트가 "SessionService" Provider를 검증합니다 |
| `@PactFolder("../attendee-service/build/pacts")` | Pact JSON 파일을 이 경로에서 읽습니다 |
| `@SpringBootTest(webEnvironment = RANDOM_PORT)` | 실제 Spring Boot 서버를 랜덤 포트로 시작합니다 |
| `@LocalServerPort private var port` | 랜덤으로 할당된 포트 번호를 가져옵니다 |
| `context.target = HttpTestTarget("localhost", port)` | Pact가 이 포트로 HTTP 요청을 보내도록 설정합니다 |
| `pactVerificationTestTemplate()` | Pact 파일의 각 interaction마다 한 번씩 호출됩니다 |

핵심: `@TestTemplate`과 `PactVerificationInvocationContextProvider`가 Pact 파일의 3개 interaction을 순회하면서, 각각에 대해 `context.verifyInteraction()`을 호출합니다. 이것이 실제 HTTP 요청을 보내고 응답을 검증합니다.

---

#### Interaction 1 검증: "세션 1 조회 요청"

Pact 프레임워크가 하는 일:

1. `providerStates`에서 `"세션 ID 1이 존재함"`을 발견합니다
2. 아래 `@State` 메서드를 찾아 실행합니다:

```kotlin
@State("세션 ID 1이 존재함")
fun sessionWithId1Exists() {
    sessionStore.clear()
    sessionStore.addSession(Session(
        title = "gRPC로 마이크로서비스 구축하기",
        speaker = "장호",
        description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
        dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
    ))
}
```

이 메서드가 하는 일:
- `sessionStore.clear()`: 기존 데이터를 모두 지웁니다 (counter도 0으로 리셋)
- `sessionStore.addSession(...)`: 세션 하나를 추가합니다. `addSession()`은 `counter.incrementAndGet()`으로 id=1을 부여합니다

3. Pact가 `GET /sessions/1` 요청을 Spring Boot 서버에 보냅니다
4. `SessionController.getSession(1)`이 호출되고, `SessionStore`에서 id=1 세션을 찾아 반환합니다
5. 응답 JSON:

```json
{
  "id": 1,
  "title": "gRPC로 마이크로서비스 구축하기",
  "speaker": "장호",
  "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
  "dateTime": "2024-09-15T10:00:00"
}
```

6. `matchingRules` 확인:
   - `$.id`가 정수? 1은 정수. 통과!
   - `$.title`이 문자열? "gRPC로 마이크로서비스 구축하기"는 문자열. 통과!
   - `$.speaker`가 문자열? "장호"는 문자열. 통과!
   - `$.description`이 문자열? 통과!
   - `$.dateTime`이 문자열? "2024-09-15T10:00:00"은 문자열. 통과!

---

#### Interaction 2 검증: "전체 세션 목록 조회 요청"

1. `@State("세션 목록이 존재함")` 실행:

```kotlin
@State("세션 목록이 존재함")
fun sessionsExist() {
    sessionStore.clear()
    sessionStore.addSession(Session(
        title = "gRPC로 마이크로서비스 구축하기",
        speaker = "장호",
        description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
        dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
    ))
    sessionStore.addSession(Session(
        title = "API 게이트웨이 패턴",
        speaker = "김현수",
        description = "API 게이트웨이의 다양한 패턴과 구현 전략을 살펴봅니다.",
        dateTime = LocalDateTime.of(2024, 9, 15, 14, 0)
    ))
    sessionStore.addSession(Session(
        title = "계약 테스트 실전",
        speaker = "이서연",
        description = "Pact 프레임워크를 사용한 CDC 테스트 실전 사례를 공유합니다.",
        dateTime = LocalDateTime.of(2024, 9, 16, 10, 0)
    ))
}
```

3개의 세션을 추가합니다.

2. Pact가 `GET /sessions` 요청을 보냅니다
3. `SessionController.getSessions()`가 `ApiResponse(data = sessions)`를 반환합니다
4. 응답 JSON:

```json
{
  "data": [
    { "id": 1, "title": "gRPC로 마이크로서비스 구축하기", "speaker": "장호", ... },
    { "id": 2, "title": "API 게이트웨이 패턴", "speaker": "김현수", ... },
    { "id": 3, "title": "계약 테스트 실전", "speaker": "이서연", ... }
  ],
  "total": 3
}
```

5. `matchingRules` 확인:
   - `$.data`가 배열? 통과!
   - `$.data[*].id`가 정수? 1, 2, 3 모두 정수. 통과!
   - `$.data[*].title`이 문자열? 통과!
   - `$.data[*].speaker`가 문자열? 통과!
   - `$.total`이 정수? 3은 정수. 통과!

---

#### Interaction 3 검증: "존재하지 않는 세션 조회 요청"

1. `@State("세션 ID 999가 존재하지 않음")` 실행:

```kotlin
@State("세션 ID 999가 존재하지 않음")
fun sessionWithId999DoesNotExist() {
    sessionStore.clear()
}
```

저장소를 완전히 비웁니다. 어떤 세션도 존재하지 않습니다.

2. Pact가 `GET /sessions/999` 요청을 보냅니다
3. `SessionController.getSession(999)`가 호출되고, `SessionStore`에서 id=999를 찾지 못해 `ResourceNotFoundException("Session with id 999 not found")`를 던집니다
4. `SessionController`의 `@ExceptionHandler`가 이를 잡아 HTTP 404를 반환합니다
5. 계약에서 기대하는 것: `"status": 404`. 실제 응답: 404. 통과!

---

### 5.4 테스트 결과 확인

모든 테스트가 통과한 후, 상세 리포트를 확인할 수 있습니다:

```bash
# macOS에서 브라우저로 열기
open attendee-service/build/reports/tests/test/index.html
open session-service/build/reports/tests/test/index.html
```

리포트에서 확인할 수 있는 것:
- 실행된 테스트 수
- 각 테스트의 성공/실패 여부
- 실행 시간

---

## 6. 직접 계약 추가해보기 (실습 가이드)

이 장에서는 새로운 계약을 직접 추가하는 과정을 단계별로 안내합니다. 실습을 통해 CDC 테스트의 전체 워크플로우를 체험할 수 있습니다.

### 6.1 시나리오: "세션 생성 API" 계약 추가

시나리오: "AttendeeService에서 SessionService에 새 세션을 생성하는 API를 호출해야 한다"

현재 `SessionClient`에는 `getSessions()`와 `getSession(id)` 두 메서드만 있습니다. 여기에 세션 생성 기능을 추가합니다.

#### Step 1: SessionClient에 메서드 추가

파일: `attendee-service/src/main/kotlin/com/conference/attendee/client/SessionClient.kt`

기존 코드에 `createSession()` 메서드를 추가합니다:

```kotlin
package com.conference.attendee.client

import com.conference.common.model.ApiResponse
import com.conference.common.model.Session
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class SessionClient(private val sessionRestClient: RestClient) {

    fun getSessions(): List<Session> {
        val response = sessionRestClient.get()
            .uri("/sessions")
            .retrieve()
            .body(object : ParameterizedTypeReference<ApiResponse<Session>>() {})
        return response?.data ?: emptyList()
    }

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

    // 새로 추가하는 메서드
    fun createSession(session: Session): Session? {
        return try {
            sessionRestClient.post()
                .uri("/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(session)
                .retrieve()
                .body(Session::class.java)
        } catch (e: RestClientResponseException) {
            throw e
        }
    }
}
```

#### Step 2: Consumer Pact 테스트에 계약 추가

파일: `attendee-service/src/test/kotlin/com/conference/attendee/pact/SessionServiceConsumerPactTest.kt`

기존 3개의 계약 아래에 새 계약을 추가합니다:

```kotlin
@Pact(consumer = "AttendeeService")
fun createSessionPact(builder: PactDslWithProvider): RequestResponsePact {
    return builder
        .given("세션 생성이 가능한 상태")
        .uponReceiving("새 세션 생성 요청")
        .path("/sessions")
        .method("POST")
        .headers("Content-Type", "application/json")
        .body(PactDslJsonBody()
            .stringType("title", "새로운 세션")
            .stringType("speaker", "홍길동")
            .stringType("description", "세션 설명"))
        .willRespondWith()
        .status(201)
        .headers(mapOf("Content-Type" to "application/json"))
        .body(PactDslJsonBody()
            .integerType("id")
            .stringType("title", "새로운 세션")
            .stringType("speaker", "홍길동"))
        .toPact()
}

@Test
@PactTestFor(pactMethod = "createSessionPact")
fun `새 세션을 생성한다`(mockServer: MockServer) {
    val restClient = RestClient.builder()
        .baseUrl(mockServer.getUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
    val client = SessionClient(restClient)

    val newSession = Session(
        title = "새로운 세션",
        speaker = "홍길동",
        description = "세션 설명"
    )
    val created = client.createSession(newSession)

    assertThat(created).isNotNull
    assertThat(created!!.title).isEqualTo("새로운 세션")
}
```

이 계약의 의미:
- Consumer는 `POST /sessions`에 세션 정보를 보냅니다
- Provider는 HTTP 201(Created)과 함께, id가 부여된 세션을 반환해야 합니다
- `integerType("id")`로 id 필드가 정수형이기만 하면 됩니다 (값은 상관없음)

#### Step 3: Consumer 테스트 실행

```bash
./gradlew :attendee-service:test
```

통과하면 `attendee-service/build/pacts/AttendeeService-SessionService.json`에 새 interaction이 추가됩니다.

#### Step 4: Provider 쪽에 @State 핸들러 추가

파일: `session-service/src/test/kotlin/com/conference/session/pact/SessionServiceProviderPactTest.kt`

기존 3개의 `@State` 메서드 아래에 추가합니다:

```kotlin
@State("세션 생성이 가능한 상태")
fun readyForSessionCreation() {
    sessionStore.clear()
}
```

`@State`의 문자열이 Consumer 계약의 `.given("세션 생성이 가능한 상태")`와 **정확히 일치**해야 합니다. 한 글자라도 다르면 매칭되지 않아 테스트가 실패합니다.

#### Step 5: Provider 검증 실행

```bash
./gradlew :session-service:test
```

Pact가 `POST /sessions` 요청을 실제 Spring Boot 서버에 보내고, `SessionController.createSession()`이 호출됩니다. 이 메서드는 `SessionStore.addSession()`으로 세션을 저장하고, HTTP 201과 함께 생성된 세션을 반환합니다.

반환된 응답이 계약의 `matchingRules`를 충족하면 통과합니다.

#### Step 6: 전체 테스트 실행

```bash
./gradlew test
```

모든 테스트(기존 3개 + 새 1개)가 통과하면 성공입니다.

### 6.2 계약 변경 워크플로우

Provider가 API를 변경했을 때 계약이 어떻게 보호해주는지 시나리오로 설명합니다.

#### 시나리오: Provider가 응답 필드명을 변경

SessionService 팀에서 `Session` 클래스의 `title` 필드명을 `name`으로 바꾸기로 결정했다고 가정합니다.

1. SessionService 팀이 `Session.kt`에서 `title`을 `name`으로 변경합니다
2. `./gradlew :session-service:test` 실행
3. Provider Pact 검증 단계에서 **즉시 실패**합니다:

```
Response body mismatch:
  $.title - Expected a string but was missing
```

계약에는 "응답에 `title` 필드(문자열)가 있어야 한다"고 명시되어 있는데, Provider가 `title` 대신 `name`을 반환하기 때문입니다.

4. 이 실패를 본 SessionService 팀은 두 가지 선택이 있습니다:
   - (a) 변경을 철회하고 `title`을 유지합니다
   - (b) AttendeeService 팀과 협의하여, Consumer 테스트의 계약도 함께 수정합니다

이것이 계약 테스트의 핵심 가치입니다: **배포 전에, 코드 수준에서 호환성 문제를 발견**합니다.

---

## 7. Pact Broker 사용하기 (선택)

지금까지는 Pact 파일을 **파일 시스템**으로 직접 전달했습니다 (`@PactFolder`). 이 방식은 한 개발자의 로컬 환경에서는 충분하지만, 여러 팀이 독립적으로 개발/배포할 때는 한계가 있습니다.

**Pact Broker**는 계약의 중앙 저장소로, 다음 기능을 제공합니다:
- 모든 Consumer-Provider 관계를 한 곳에서 관리
- 버전별 계약 이력 추적
- `can-i-deploy`: 특정 버전을 배포해도 안전한지 자동 확인
- 웹 UI에서 계약 현황을 시각적으로 확인

### 7.1 Pact Broker 실행

이 프로젝트의 `docker-compose.yml`에 Pact Broker와 PostgreSQL이 정의되어 있습니다.

```bash
docker compose up -d
```

이 명령어가 시작하는 컨테이너:

| 서비스 | 이미지 | 포트 | 역할 |
|--------|--------|------|------|
| `postgres` | postgres:17-alpine | 5432 | Pact Broker의 데이터 저장소 |
| `pact-broker` | pactfoundation/pact-broker:latest | 9292 | Pact Broker 서버 |

`docker-compose.yml`의 설정값:

| 환경변수 | 기본값 | 설명 |
|----------|--------|------|
| `POSTGRES_USER` | pact | DB 사용자명 |
| `POSTGRES_PASSWORD` | pact | DB 비밀번호 |
| `POSTGRES_DB` | pact_broker | DB 이름 |
| `PACT_BROKER_BASIC_AUTH_USERNAME` | pact | Broker 로그인 사용자명 |
| `PACT_BROKER_BASIC_AUTH_PASSWORD` | pact | Broker 로그인 비밀번호 |

시작 확인:
```bash
# 컨테이너 상태 확인
docker compose ps

# Broker 헬스체크
curl -s http://localhost:9292/diagnostic/status/heartbeat
```

브라우저에서 접속: http://localhost:9292
- 사용자명: `pact`
- 비밀번호: `pact`

### 7.2 Pact Broker UI

Pact Broker 웹 UI에서 확인할 수 있는 정보:

```
+-----------------------------------------------------+
|  Pact Broker                                        |
|-----------------------------------------------------|
|  Pacts                                              |
|-----------------------------------------------------|
|  Consumer        | Provider        | Last verified  |
|  AttendeeService | SessionService  | (timestamp)    |
|-----------------------------------------------------|
|  [클릭하면 계약 상세 보기]                            |
|    - interactions 목록                               |
|    - 각 interaction의 request/response               |
|    - 검증 상태                                       |
+-----------------------------------------------------+
```

### 7.3 Pact 파일 Broker에 게시

Consumer 테스트를 실행하여 Pact 파일을 생성한 후, Broker에 게시합니다:

```bash
docker run --rm --net=host \
  -v $(pwd)/attendee-service/build/pacts:/pacts \
  pactfoundation/pact-cli:latest \
  publish /pacts \
  --broker-base-url=http://localhost:9292 \
  --broker-username=pact \
  --broker-password=pact \
  --consumer-app-version=1.0.0
```

명령어 설명:

| 옵션 | 설명 |
|------|------|
| `--rm` | 컨테이너 실행 후 자동 삭제 |
| `--net=host` | 호스트 네트워크 사용 (localhost:9292 접근을 위해) |
| `-v $(pwd)/attendee-service/build/pacts:/pacts` | Pact 파일 디렉토리를 컨테이너에 마운트 |
| `publish /pacts` | /pacts 디렉토리의 Pact 파일을 게시 |
| `--broker-base-url` | Broker URL |
| `--broker-username/password` | 인증 정보 |
| `--consumer-app-version` | Consumer 버전 태그 |

게시 후 http://localhost:9292 에서 계약을 확인할 수 있습니다.

### 7.4 can-i-deploy 확인

`can-i-deploy`는 "이 버전의 서비스를 배포해도 안전한가?"를 확인하는 명령어입니다.

```bash
docker run --rm --net=host \
  pactfoundation/pact-cli:latest \
  broker can-i-deploy \
  --pacticipant=AttendeeService \
  --version=1.0.0 \
  --to-environment=production \
  --broker-base-url=http://localhost:9292 \
  --broker-username=pact \
  --broker-password=pact
```

이 명령어의 의미: "AttendeeService 1.0.0 버전을 production 환경에 배포해도 괜찮은가?"

Pact Broker는 다음을 확인합니다:
1. 이 버전의 Pact 파일이 존재하는가?
2. Provider가 이 계약을 검증했는가?
3. 검증 결과가 성공인가?

모두 통과하면 배포 가능, 하나라도 실패하면 배포 불가로 판정합니다.

### 7.5 Broker 종료

```bash
docker compose down
```

데이터를 포함하여 완전 초기화:
```bash
docker compose down -v
```

`-v` 플래그는 `postgres-data` 볼륨도 삭제하여, 다음에 시작할 때 깨끗한 상태로 시작합니다.

---

## 8. OpenAPI 문서 확인

두 서비스 모두 `springdoc-openapi-starter-webmvc-ui:2.3.0` 의존성을 포함하고 있어, 서버 실행 시 Swagger UI가 자동으로 생성됩니다.

### 8.1 Swagger UI

서비스가 실행 중인 상태에서 브라우저로 접속합니다:

```
# Session Service (포트 8081)
http://localhost:8081/swagger-ui.html

# Attendee Service (포트 8080)
http://localhost:8080/swagger-ui.html
```

Swagger UI에서 할 수 있는 것:
- 모든 API 엔드포인트 목록 확인
- 각 엔드포인트의 요청/응답 스키마 확인
- "Try it out" 버튼으로 브라우저에서 직접 API 호출 테스트
- curl 명령어 자동 생성

### 8.2 OpenAPI JSON 스펙

JSON 형태의 OpenAPI 스펙을 직접 받을 수도 있습니다:

```bash
# Session Service
curl -s http://localhost:8081/v3/api-docs | jq .

# Attendee Service
curl -s http://localhost:8080/v3/api-docs | jq .
```

---

## 9. 자주 묻는 질문 (FAQ)

### Q1: Consumer 테스트 없이 Provider 테스트만 실행할 수 있나요?

안됩니다. Provider 테스트(`SessionServiceProviderPactTest`)는 `@PactFolder("../attendee-service/build/pacts")`에서 Pact JSON 파일을 읽어야 합니다. 이 파일은 Consumer 테스트(`SessionServiceConsumerPactTest`)가 실행될 때 생성됩니다.

파일이 없으면 Provider 테스트가 실패합니다. 이 순서는 `session-service/build.gradle.kts`의 `dependsOn(":attendee-service:test")` 설정으로 자동 보장됩니다.

### Q2: 테스트에서 실제 네트워크 요청이 발생하나요?

**Consumer 테스트**: Pact 내장 MockServer에만 요청합니다. MockServer는 localhost의 임시 포트에서 실행됩니다. 외부 서비스에 대한 네트워크 요청은 발생하지 않습니다.

**Provider 테스트**: `@SpringBootTest(webEnvironment = RANDOM_PORT)`로 시작된 Spring Boot 테스트 서버에 요청합니다. 이 역시 localhost의 랜덤 포트에서 실행되며, 외부 서비스 연결은 없습니다.

### Q3: Pact 파일은 어디에 생성되나요?

```
attendee-service/build/pacts/AttendeeService-SessionService.json
```

파일명 규칙: `{Consumer이름}-{Provider이름}.json`

Consumer 이름은 `@Pact(consumer = "AttendeeService")`에서, Provider 이름은 `@PactTestFor(providerName = "SessionService")`에서 결정됩니다.

### Q4: Provider State(@State)는 무엇인가요?

Provider State는 Consumer가 정의한 **전제 조건**입니다.

Consumer 쪽에서 `.given("세션 ID 1이 존재함")`이라고 계약에 적으면, Provider 쪽에서는 `@State("세션 ID 1이 존재함")` 메서드를 만들어 그 상태를 실제로 준비해야 합니다.

이 프로젝트에서 사용하는 Provider State 3개:

| Consumer의 `.given()` | Provider의 `@State()` 메서드 | 하는 일 |
|------------------------|------------------------------|---------|
| "세션 ID 1이 존재함" | `sessionWithId1Exists()` | store 초기화 + 세션 1개 추가 |
| "세션 목록이 존재함" | `sessionsExist()` | store 초기화 + 세션 3개 추가 |
| "세션 ID 999가 존재하지 않음" | `sessionWithId999DoesNotExist()` | store 비우기 |

문자열이 **정확히 일치**해야 합니다. 한 글자라도 다르면 테스트가 실패합니다.

### Q5: integerType, stringType은 무엇인가요?

Pact의 **타입 매칭 규칙**입니다. "값이 정확히 일치하는가?"가 아니라 "값의 타입이 맞는가?"를 확인합니다.

| 메서드 | 의미 | 예시 | 통과하는 값 |
|--------|------|------|-------------|
| `integerType("id", 1L)` | id 필드가 정수형이면 OK | 예시값: 1 | 1, 42, 999 모두 통과 |
| `stringType("title", "gRPC로...")` | title 필드가 문자열이면 OK | 예시값: "gRPC로..." | 아무 문자열이나 통과 |
| `eachLike("data", 3)` | data 필드가 배열이면 OK | 3개 예시 | 1개든 100개든 통과 |

예시값은 MockServer가 Consumer 테스트에서 반환하는 값이자, Pact JSON 파일의 `body`에 기록되는 값입니다. Provider 검증 시에는 `matchingRules`만 적용됩니다.

### Q6: 새 필드를 Provider 응답에 추가하면 계약이 깨지나요?

아닙니다. Pact는 기본적으로 **"Consumer가 필요한 필드가 있으면 OK"** 방식으로 동작합니다. 이를 "Postel's Law" 또는 "Robustness Principle"이라고 합니다.

예를 들어, Consumer 계약에 `id`, `title`, `speaker`만 정의되어 있을 때:
- Provider가 `id`, `title`, `speaker`, `room`, `capacity`를 반환하면? **통과**합니다
- Provider가 `id`, `speaker`만 반환하면? **실패**합니다 (`title` 누락)

### Q7: Docker 없이도 사용할 수 있나요?

네. Docker는 **Pact Broker 실행용**입니다. 로컬 Pact 테스트는 Docker 없이 완벽하게 동작합니다.

- `./gradlew test`: Docker 필요 없음. Pact 파일이 파일 시스템(`attendee-service/build/pacts/`)을 통해 전달됩니다
- `docker compose up`: Docker 필요. Pact Broker를 로컬에서 실행합니다

### Q8: 인메모리 저장소를 실제 DB로 교체하려면?

`SessionStore`와 `AttendeeStore`는 `ConcurrentHashMap`을 사용하는 인메모리 저장소입니다. 실제 DB로 교체하려면:

1. `SessionStore`를 Spring Data JPA의 `JpaRepository` 인터페이스로 교체합니다
2. `Session` 데이터 클래스에 `@Entity`, `@Id` 등 JPA 어노테이션을 추가합니다
3. `build.gradle.kts`에 `spring-boot-starter-data-jpa`와 DB 드라이버 의존성을 추가합니다

중요한 점: **Controller와 Pact 테스트는 변경할 필요가 없습니다.** Controller는 Store/Repository를 주입받아 사용하므로, 저장소 구현이 바뀌어도 API의 입출력은 동일합니다. Pact 계약은 API의 입출력만 검증하므로, 내부 구현 변경과 무관합니다.

다만 Provider Pact 테스트의 `@State` 메서드에서 데이터를 준비하는 방식은 바뀔 수 있습니다 (예: 테스트용 DB에 데이터를 insert하는 방식으로).

---

## 10. 트러블슈팅

### 10.1 빌드 실패

| 증상 | 원인 | 해결 |
|------|------|------|
| `Could not find ... kotlin` 관련 오류 | JDK 버전 불일치 | `java -version`으로 JDK 21 이상 확인 |
| `Port already in use 8080` | 이미 같은 포트에서 프로세스 실행 중 | `lsof -i :8080` (macOS/Linux) 또는 `netstat -ano | findstr :8080` (Windows)으로 확인 후 해당 프로세스 종료 |
| `Port already in use 8081` | session-service가 이미 실행 중 | `lsof -i :8081`로 확인 후 종료 |
| `BUILD FAILED` (테스트 실패) | 테스트 오류 | 아래 테스트 실패 항목 참조 |
| `Unsupported class file major version 65` | JDK 21 코드를 낮은 버전 JDK로 실행 | JDK 21 이상 설치 및 JAVA_HOME 설정 |
| `Permission denied: ./gradlew` | 실행 권한 없음 | `chmod +x gradlew` |

### 10.2 Consumer 테스트 실패

| 증상 | 원인 | 해결 |
|------|------|------|
| `Unexpected request received` | `@Pact` 메서드에서 정의한 요청과 `@Test` 메서드에서 실제로 보내는 요청이 불일치 | path, method, headers가 계약 정의와 정확히 일치하는지 확인 |
| `No interaction found for ...` | `@PactTestFor(pactMethod = "...")` 이름이 실제 `@Pact` 메서드 이름과 불일치 | pactMethod 값의 오타 확인 |
| `MockServer returned 500` | 계약 응답 정의(PactDslJsonBody)에 오류 | body 구조 확인. 특히 `closeArray()` 누락, 타입 불일치 등 |
| `Connection refused` | MockServer가 시작되지 않음 | `@ExtendWith(PactConsumerTestExt::class)` 확인 |

### 10.3 Provider 검증 실패

| 증상 | 원인 | 해결 |
|------|------|------|
| `No pacts found in directory` | Pact JSON 파일이 없음 | Consumer 테스트를 먼저 실행하세요: `./gradlew :attendee-service:test` |
| `Missing Provider State "..."` | `@State` 메서드가 없거나 이름이 다름 | Consumer의 `.given("...")` 문자열과 Provider의 `@State("...")` 문자열이 정확히 일치하는지 확인 |
| `Response body mismatch` | Provider 응답이 계약의 matchingRules를 충족하지 못함 | Controller가 반환하는 JSON 구조 확인. 필드명, 타입, 누락 여부 점검 |
| `Response status mismatch` | HTTP 상태 코드 불일치 | 예: 계약에서 200을 기대하는데 500이 반환됨. 서버 에러 로그 확인 |
| `Spring context failed to start` | Spring Boot 설정 오류 | application.yml, Bean 설정 확인 |

### 10.4 Docker 관련

```bash
# Pact Broker가 시작되지 않을 때
docker compose logs pact-broker
docker compose logs postgres

# PostgreSQL 연결 문제 (흔한 원인)
# postgres 컨테이너가 healthy 상태가 될 때까지 기다려야 합니다
docker compose ps
# postgres 상태가 "healthy"인지 확인

# 완전 초기화 (데이터 포함)
docker compose down -v
docker compose up -d

# 포트 5432가 이미 사용 중일 때 (로컬 PostgreSQL이 실행 중인 경우)
lsof -i :5432
# 로컬 PostgreSQL을 종료하거나, docker-compose.yml에서 포트를 변경
```

### 10.5 Gradle 관련

```bash
# 캐시 초기화
./gradlew clean

# 의존성 새로 다운로드
./gradlew build --refresh-dependencies

# 상세 로그 보기
./gradlew test --info

# 특정 테스트만 실행
./gradlew :session-service:test --tests "*.SessionServiceProviderPactTest"
./gradlew :attendee-service:test --tests "*.SessionServiceConsumerPactTest"
```

---

## 11. 다음 단계

### 11.1 학습 추천 경로

1. **이 프로젝트의 Consumer 테스트 코드를 직접 수정해보기**
   - `PactDslJsonBody`에서 `stringType` 대신 `stringMatcher`(정규식)를 사용해보세요
   - `eachLike`의 요소 수를 바꿔보세요

2. **새 계약 추가하기 (6장 실습 따라하기)**
   - 세션 생성, 수정, 삭제 API에 대한 계약을 추가해보세요
   - 에러 케이스(잘못된 입력, 서버 에러 등)에 대한 계약도 만들어보세요

3. **Pact Broker 연동하기 (7장)**
   - `@PactFolder` 대신 `@PactBroker`를 사용하여 Broker에서 계약을 읽어오도록 변경해보세요
   - Webhook을 설정하여 계약 변경 시 자동으로 Provider 검증이 실행되게 해보세요

4. **CI/CD 파이프라인에 통합하기**
   - GitHub Actions에서 Consumer 테스트 -> Broker 게시 -> Provider 검증 -> can-i-deploy 흐름을 자동화해보세요

5. **Message Pact 학습**
   - HTTP API 외에도, 메시지 큐(Kafka, RabbitMQ 등)를 통한 비동기 통신에도 계약 테스트를 적용할 수 있습니다
   - Pact JVM의 `au.com.dius.pact.consumer:junit5` 패키지에서 `MessagePactBuilder`를 제공합니다

### 11.2 참고 자료

| 자료 | 링크 |
|------|------|
| 책: Mastering API Architecture (원서/한빛미디어) | O'Reilly / 한빛미디어 |
| Pact 공식 문서 | https://docs.pact.io/ |
| Pact JVM (Java/Kotlin용) | https://github.com/pact-foundation/pact-jvm |
| Pact 공식 5분 가이드 | https://docs.pact.io/5-minute-getting-started-guide |
| Mastering API Architecture GitHub | https://github.com/masteringapi |
| Spring Boot 공식 문서 | https://docs.spring.io/spring-boot/docs/current/reference/html/ |
| Pact Broker 문서 | https://docs.pact.io/pact_broker |

### 11.3 관련 프레임워크 비교

| 도구 | 방식 | 장점 | 단점 |
|------|------|------|------|
| Pact | CDC (Consumer-Driven) | 소비자 중심 설계, 언어 무관 (JVM, JS, Python, Go 등), 활발한 커뮤니티 | Broker 운영 필요 (대규모), 학습 곡선 |
| Spring Cloud Contract | Provider-Driven | Spring 생태계와 긴밀한 통합, Groovy/YAML DSL, Stub 자동 생성 | Spring 전용, 다른 언어와 호환 어려움 |
| Dredd | API 명세 기반 | OpenAPI 스펙을 직접 활용, 별도 테스트 코드 불필요 | 유연성 부족, 복잡한 시나리오 표현 어려움 |
| Schemathesis | API 명세 기반 | 속성 기반 테스트(Property-based), 자동 엣지 케이스 탐지 | OpenAPI 스펙 필수, 계약 테스트와는 성격이 다름 |

이 프로젝트에서 Pact를 선택한 이유:
- Consumer가 실제로 사용하는 부분만 계약에 포함하는 CDC 방식이 마이크로서비스에 적합합니다
- JVM(Kotlin/Java)뿐 아니라 다른 언어로 작성된 서비스와도 계약을 공유할 수 있습니다
- "Mastering API Architecture" 책에서 Pact를 중심으로 설명하고 있습니다

# Pact Conference Demo - 사용자 가이드

> **"Mastering API Architecture"** 책 기반 계약 테스트 실습 프로젝트
> Kotlin + Spring Boot + Pact로 구현한 마이크로서비스 계약 테스트 전체 흐름 학습

---

## 목차

1. [이 프로젝트는 무엇인가?](#1-이-프로젝트는-무엇인가)
2. [시작하기 전에 (Prerequisites)](#2-시작하기-전에-prerequisites)
3. [프로젝트 구조 이해하기](#3-프로젝트-구조-이해하기)
4. [빠른 시작 (Quick Start)](#4-빠른-시작-quick-start)
5. [API 사용 가이드](#5-api-사용-가이드)
6. [Pact 계약 테스트 따라하기 (핵심!)](#6-pact-계약-테스트-따라하기-핵심)
7. [테스트 실행 가이드](#7-테스트-실행-가이드)
8. [Feature Flag 사용법](#8-feature-flag-사용법)
9. [JPA 모드로 실행하기](#9-jpa-모드로-실행하기)
10. [모니터링 & 대시보드](#10-모니터링--대시보드)
11. [Docker & Kubernetes 배포](#11-docker--kubernetes-배포)
12. [성능 테스트](#12-성능-테스트)
13. [직접 계약 추가해보기 (실습 가이드)](#13-직접-계약-추가해보기-실습-가이드)
14. [자주 묻는 질문 (FAQ)](#14-자주-묻는-질문-faq)
15. [트러블슈팅](#15-트러블슈팅)
16. [프로젝트 학습 로드맵](#16-프로젝트-학습-로드맵)

---

## 1. 이 프로젝트는 무엇인가?

### 1.1 배경: "Mastering API Architecture"

이 프로젝트는 O'Reilly에서 출판된 **"Mastering API Architecture"** (한빛미디어 한국어판: "마스터링 API 아키텍처")의 내용을 기반으로 만든 학습용 데모 프로젝트입니다.

책에서는 컨퍼런스(학회/세미나) 시스템을 예시로 사용하여 API 아키텍처의 다양한 패턴을 설명합니다. 이 프로젝트는 그 중에서도 **계약 테스트(Contract Testing)**와 **API 설계 패턴**을 직접 실행해볼 수 있도록 Kotlin + Spring Boot로 구현한 것입니다.

### 1.2 프로젝트 목적과 학습 목표

이 프로젝트를 통해 다음을 배울 수 있습니다:

| 학습 주제 | 구현된 내용 |
|-----------|------------|
| Consumer-Driven Contract 테스트 | Pact 프레임워크로 서비스 간 계약 검증 |
| API 게이트웨이 패턴 | Spring Cloud Gateway MVC로 라우팅 및 JWT 인증 |
| API 버전 관리 | URI 경로 버전 관리 (v1/v2) |
| RFC 7807 에러 응답 | ProblemDetail로 표준화된 오류 형식 |
| Branch by Abstraction | In-memory / JPA 저장소 전환 패턴 |
| Feature Flags | 런타임 기능 전환 (새 투표 알고리즘) |
| 모니터링 | Prometheus + Grafana 메트릭 수집 |

### 1.3 4개 Phase 구현 요약

| Phase | 핵심 주제 | 구현 내용 |
|-------|-----------|----------|
| **Phase 1** | 기초 계약 테스트 | 3개 서비스 + Gateway, Pact Consumer/Provider 테스트 기본 |
| **Phase 2** | 고급 계약 패턴 | API 버전 관리, Provider 상태 관리, Pact Broker 연동 |
| **Phase 3** | 운영 수준 테스트 | Feature Flags, 가중 투표 알고리즘, 성능 테스트, 모니터링 |
| **Phase 4** | 배포 및 인프라 | Docker, Kubernetes, CI/CD 파이프라인, can-i-deploy |

### 1.4 누구를 위한 가이드인가

- 마이크로서비스 계약 테스트를 처음 배우는 개발자
- Pact 프레임워크를 JVM 환경에서 사용해보고 싶은 개발자
- "Mastering API Architecture" 책의 예제 코드를 직접 실행해보고 싶은 독자
- Spring Boot + Kotlin 기반 API 설계 패턴을 학습하고 싶은 개발자

---

## 2. 시작하기 전에 (Prerequisites)

### 2.1 필수 설치 항목

#### JDK 21 (필수)

이 프로젝트는 **JDK 21**이 필요합니다. Eclipse Temurin을 권장합니다.

```bash
# macOS (Homebrew)
brew install --cask temurin@21

# 버전 확인
java -version
# 출력 예시: openjdk version "21.0.x" 2024-xx-xx
```

#### Gradle (선택 - wrapper 포함)

프로젝트에 Gradle Wrapper(`gradlew`)가 포함되어 있으므로 별도 설치는 불필요합니다. 단, 직접 설치하려면 8.10 이상을 사용하세요.

```bash
# wrapper 사용 (권장)
./gradlew --version
```

#### Docker & Docker Compose (선택)

Pact Broker, Prometheus, Grafana 실행에 필요합니다.

```bash
# 설치 확인
docker --version
docker compose version
```

#### Git

```bash
git --version
```

### 2.2 추천 IDE

**IntelliJ IDEA** (Community 또는 Ultimate)를 권장합니다.

- `File > Open` 에서 프로젝트 루트 디렉토리 선택
- Gradle 프로젝트로 자동 인식됩니다
- Kotlin 플러그인이 기본 내장되어 있습니다

### 2.3 최소 시스템 요구사항

| 항목 | 최소 사양 |
|------|----------|
| RAM | 4 GB (4개 서비스 동시 실행 시 8 GB 권장) |
| 디스크 | 2 GB 여유 공간 |
| OS | macOS 12+, Linux (Ubuntu 20.04+), Windows 10+ |
| CPU | 2코어 이상 |

---

## 3. 프로젝트 구조 이해하기

### 3.1 디렉토리 구조

```
pact-conference-demo-phase1/
├── common/                          # 공통 모듈 (도메인 모델, 보안, 예외)
│   └── src/main/kotlin/com/conference/common/
│       ├── model/                   # Session, Attendee, ApiResponse
│       ├── exception/               # GlobalExceptionHandler, ProblemDetail
│       └── security/                # JWT, RoleRequired, RoleCheckInterceptor
│
├── gateway/                         # API Gateway (포트 8080)
│   └── src/main/kotlin/com/conference/gateway/
│       ├── controller/AuthController.kt   # JWT 토큰 발급
│       ├── filter/JwtAuthFilter.kt        # JWT 검증 필터
│       └── config/                        # CORS, RateLimit 설정
│   └── src/main/resources/application.yml # 라우팅 규칙
│
├── attendee-service/                # 참석자 서비스 (포트 8081, Consumer)
│   └── src/main/kotlin/com/conference/attendee/
│       ├── controller/AttendeeController.kt
│       ├── client/SessionClient.kt        # SessionService 호출
│       └── dto/AttendeeDto.kt
│   └── src/test/kotlin/...pact/
│       ├── SessionServiceConsumerPactTest.kt   # Pact Consumer 테스트
│       └── AttendeeServiceProviderPactTest.kt  # Pact Provider 테스트
│
├── session-service/                 # 세션 서비스 (포트 8082, Provider)
│   └── src/main/kotlin/com/conference/session/
│       ├── controller/SessionController.kt     # CRUD API
│       ├── controller/SessionV1Controller.kt   # v1 API
│       ├── controller/SessionV2Controller.kt   # v2 API (확장 필드)
│       ├── store/SessionStore.kt               # In-memory 저장소
│       └── store/JpaSessionStore.kt            # JPA 저장소 (jpa profile)
│   └── src/test/kotlin/...pact/
│       └── SessionServiceProviderPactTest.kt   # Pact Provider 테스트
│
├── cfp-service/                     # CFP 서비스 (포트 8083, Consumer)
│   └── src/main/kotlin/com/conference/cfp/
│       ├── controller/CfpController.kt         # 제안서/투표 API
│       ├── client/SessionClient.kt             # SessionService 호출
│       ├── client/AttendeeClient.kt            # AttendeeService 호출
│       └── config/FeatureFlags.kt              # Feature Flag 설정
│   └── src/test/kotlin/...pact/
│       ├── SessionServiceConsumerPactTest.kt
│       └── AttendeeServiceConsumerPactTest.kt
│
├── docker-compose.yml               # Pact Broker + PostgreSQL
├── docker-compose.monitoring.yml    # Prometheus + Grafana
├── start-all.sh                     # 모든 서비스 한 번에 시작
├── stop-all.sh                      # 모든 서비스 종료
├── k8s/                             # Kubernetes 매니페스트
├── performance-test/                # 성능 테스트 스크립트
│   ├── load-test.sh                 # Apache Bench 기반
│   └── k6-load-test.js             # k6 기반
├── monitoring/                      # Prometheus, Grafana 설정
└── dashboard/index.html             # HTML 대시보드
```

### 3.2 5개 모듈의 역할과 의존관계

```
[클라이언트]
     |
     v
[Gateway :8080]  ────────────────────────────────────────────────
     |                    |                    |                  |
     | /api/attendees/**  | /api/sessions/**   | /api/proposals/**| /api/v1,v2/sessions/**
     v                    v                    v                  v
[AttendeeService :8081] [SessionService :8082] [CfpService :8083]
     |                                             |        |
     | GET /sessions                               |        |
     +--------------------------------------------->        |
                                                   | GET /sessions
                                                   +-------->
                                                   | GET /attendees/{id}
                                                   +-----> [AttendeeService :8081]

[Common] ─── 모든 서비스가 의존 (Session, Attendee 모델, JWT, 예외 처리)
```

### 3.3 포트 할당

| 서비스 | 포트 | 역할 |
|--------|------|------|
| Gateway | **8080** | 단일 진입점, JWT 검증, 라우팅 |
| Attendee Service | **8081** | 참석자 CRUD, Pact Consumer |
| Session Service | **8082** | 세션 CRUD, Pact Provider |
| CFP Service | **8083** | 제안서/투표, Pact Consumer |
| Pact Broker | **9292** | 계약 파일 저장/공유 (Docker) |
| Prometheus | **9090** | 메트릭 수집 (Docker) |
| Grafana | **3000** | 모니터링 대시보드 (Docker) |

---

## 4. 빠른 시작 (Quick Start)

### 방법 1: start-all.sh 사용 (권장)

모든 서비스를 한 번에 빌드하고 시작합니다.

```bash
# 프로젝트 루트에서 실행
cd pact-conference-demo-phase1

# 실행 권한 부여 (최초 1회)
chmod +x start-all.sh stop-all.sh

# 모든 서비스 시작
./start-all.sh
```

스크립트가 수행하는 작업:
1. JDK 설치 여부 확인
2. 전체 모듈 빌드 (`./gradlew clean bootJar -x test --parallel`)
3. 순서대로 서비스 시작 (session → attendee → cfp → gateway)
4. 20초 대기 후 헬스 체크

정상 시작 시 출력 예시:
```
================================================================
 Services
================================================================
  Gateway:          http://localhost:8080
  Attendee Service: http://localhost:8081
  Session Service:  http://localhost:8082
  CFP Service:      http://localhost:8083

================================================================
 Swagger UI
================================================================
  Attendee: http://localhost:8081/swagger-ui.html
  Session:  http://localhost:8082/swagger-ui.html
  CFP:      http://localhost:8083/swagger-ui.html

================================================================
 Monitoring
================================================================
  Health:     http://localhost:8080/actuator/health
  Prometheus: http://localhost:8082/actuator/prometheus
  Pact Broker: http://localhost:9292 (pact/pact)
```

### 방법 2: 개별 서비스 수동 실행

각 서비스를 별도 터미널에서 실행합니다.

```bash
# 터미널 1: Session Service (포트 8082)
./gradlew :session-service:bootRun

# 터미널 2: Attendee Service (포트 8081)
./gradlew :attendee-service:bootRun

# 터미널 3: CFP Service (포트 8083)
./gradlew :cfp-service:bootRun

# 터미널 4: Gateway (포트 8080)
./gradlew :gateway:bootRun
```

### 서비스 종료

```bash
# start-all.sh로 시작한 경우
./stop-all.sh

# 포트 번호로 직접 종료 (macOS/Linux)
lsof -ti:8080 | xargs kill -9
lsof -ti:8081 | xargs kill -9
lsof -ti:8082 | xargs kill -9
lsof -ti:8083 | xargs kill -9
```

### 헬스 체크 확인

```bash
# Gateway를 통한 헬스 체크
curl http://localhost:8080/actuator/health

# 각 서비스 직접 헬스 체크
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

응답 예시:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

---

## 5. API 사용 가이드

### 5.1 JWT 토큰 발급

쓰기 작업(POST, PUT, DELETE)은 Gateway를 통해 호출할 때 JWT 토큰이 필요합니다. GET 요청은 토큰 없이도 사용할 수 있습니다.

**엔드포인트**: `POST /api/auth/token`

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "roles": ["ADMIN"]
  }'
```

**응답 예시**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZXMiOlsiQURNSU4iXSwiaWF0IjoxNzE3NTUwMDAwLCJleHAiOjE3MTc1NTM2MDB9.XXXXX",
  "expiresIn": 3600
}
```

**토큰 구조 설명**:
- `sub`: userId (사용자 ID)
- `roles`: 역할 목록 (`ADMIN`, `ATTENDEE` 등)
- `iat`: 발급 시각 (Unix timestamp)
- `exp`: 만료 시각 (발급 후 1시간)

**토큰 저장 (편의용 환경변수)**:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "roles": ["ADMIN"]}' | python3 -m json.tool | grep '"token"' | cut -d'"' -f4)
echo $TOKEN
```

**인증 방식 요약**:

| 요청 유형 | Gateway 직접 | 서비스 직접 |
|-----------|-------------|------------|
| GET | 인증 불필요 | 인증 불필요 |
| POST / PUT / DELETE | Bearer 토큰 필요 | 인증 없음 |

> **참고**: 직접 서비스 포트(8081~8083)로 요청하면 인증 없이 모든 작업이 가능합니다. Gateway(8080)를 통해야만 JWT 검증이 작동합니다.

---

### 5.2 세션 관리 (Session Service)

- **Gateway 경유**: `http://localhost:8080/api/sessions`
- **직접 접근**: `http://localhost:8082/sessions`

#### 전체 세션 목록 조회

```bash
GET /api/sessions
```

```bash
curl http://localhost:8080/api/sessions
```

**응답 예시**:
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

#### 세션 단건 조회

```bash
GET /api/sessions/{id}
```

```bash
curl http://localhost:8080/api/sessions/1
```

**응답 예시**:
```json
{
  "id": 1,
  "title": "gRPC로 마이크로서비스 구축하기",
  "speaker": "장호",
  "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
  "dateTime": "2024-09-15T10:00:00"
}
```

#### 세션 생성

```bash
POST /api/sessions
Authorization: Bearer {token}
```

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Kotlin Coroutines 심화",
    "speaker": "박민준",
    "description": "코루틴을 활용한 비동기 프로그래밍 패턴을 알아봅니다.",
    "dateTime": "2024-09-16T14:00:00"
  }'
```

**응답 예시** (HTTP 201 Created):
```json
{
  "id": 4,
  "title": "Kotlin Coroutines 심화",
  "speaker": "박민준",
  "description": "코루틴을 활용한 비동기 프로그래밍 패턴을 알아봅니다.",
  "dateTime": "2024-09-16T14:00:00"
}
```

**필드 설명**:
- `title` (필수): 세션 제목 (빈 문자열 불가)
- `speaker` (필수): 발표자 이름 (빈 문자열 불가)
- `description` (선택): 세션 설명 (최대 500자)
- `dateTime` (선택): 발표 일시 (ISO 8601 형식: `"2024-09-16T14:00:00"`)

#### 세션 수정

```bash
PUT /api/sessions/{id}
Authorization: Bearer {token}
```

```bash
curl -X PUT http://localhost:8080/api/sessions/4 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Kotlin Coroutines 실전 활용",
    "speaker": "박민준",
    "description": "실제 프로덕션에서 코루틴을 활용한 사례를 공유합니다.",
    "dateTime": "2024-09-16T15:00:00"
  }'
```

**응답**: HTTP 204 No Content (본문 없음)

#### 세션 삭제

```bash
DELETE /api/sessions/{id}
Authorization: Bearer {token}
```

```bash
curl -X DELETE http://localhost:8080/api/sessions/4 \
  -H "Authorization: Bearer $TOKEN"
```

**응답**: HTTP 200 OK (본문 없음)

---

### 5.3 참석자 관리 (Attendee Service)

- **Gateway 경유**: `http://localhost:8080/api/attendees`
- **직접 접근**: `http://localhost:8081/attendees`

#### 전체 참석자 목록 조회

```bash
curl http://localhost:8080/api/attendees
```

**응답 예시**:
```json
{
  "data": [
    {
      "id": 1,
      "givenName": "Jim",
      "surname": "Gough",
      "email": "jim@conference.com"
    }
  ],
  "total": 1
}
```

#### 참석자 단건 조회

```bash
curl http://localhost:8080/api/attendees/1
```

**응답 예시**:
```json
{
  "id": 1,
  "givenName": "Jim",
  "surname": "Gough",
  "email": "jim@conference.com"
}
```

#### 참석자 등록

```bash
curl -X POST http://localhost:8080/api/attendees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "givenName": "길동",
    "surname": "홍",
    "email": "gildong@example.com"
  }'
```

**응답 예시** (HTTP 201 Created):
```json
{
  "id": 2,
  "givenName": "길동",
  "surname": "홍",
  "email": "gildong@example.com"
}
```

**필드 설명**:
- `givenName` (필수): 이름
- `surname` (필수): 성
- `email` (필수): 이메일 주소 (형식 검증 포함)

#### 참석자 수정

```bash
curl -X PUT http://localhost:8080/api/attendees/2 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "givenName": "길동",
    "surname": "홍",
    "email": "gildong.new@example.com"
  }'
```

**응답**: HTTP 204 No Content

#### 참석자 삭제

```bash
curl -X DELETE http://localhost:8080/api/attendees/2 \
  -H "Authorization: Bearer $TOKEN"
```

**응답**: HTTP 200 OK

#### 참석자의 세션 목록 조회 (서비스 간 통신)

이 엔드포인트는 Attendee Service가 Session Service를 내부적으로 호출하는 핵심 예제입니다. **Pact 계약 테스트의 대상**이 됩니다.

```bash
GET /api/attendees/{id}/sessions
```

```bash
curl http://localhost:8080/api/attendees/1/sessions
```

**응답 예시** (Session Service에서 가져온 데이터):
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
    }
  ],
  "total": 2
}
```

> Attendee Service의 `SessionClient`가 `http://localhost:8082/sessions`로 HTTP 요청을 보냅니다. 이 호출 계약을 Pact로 검증합니다.

---

### 5.4 CFP 관리 (CFP Service)

CFP(Call for Proposals)는 발표 제안서 관리 서비스입니다.

- **Gateway 경유**: `http://localhost:8080/api/proposals`
- **직접 접근**: `http://localhost:8083/proposals`

#### 제안서 전체 목록 조회

```bash
curl http://localhost:8080/api/proposals
```

**응답 예시**:
```json
{
  "data": [],
  "total": 0
}
```

#### 제안서 단건 조회

```bash
curl http://localhost:8080/api/proposals/1
```

**응답 예시**:
```json
{
  "id": 1,
  "title": "Pact로 시작하는 계약 테스트",
  "abstract": "Consumer-Driven Contract 테스트의 핵심 개념과 Pact 프레임워크를 사용한 실전 구현 방법을 소개합니다.",
  "speakerId": 1,
  "status": "SUBMITTED",
  "sessionId": null
}
```

**status 값 목록**:
- `SUBMITTED`: 제출됨 (기본값)
- `ACCEPTED`: 채택됨
- `REJECTED`: 거절됨

#### 제안서 생성

> CFP Service는 제안서 생성 시 Attendee Service를 호출하여 speakerId가 유효한지 확인합니다. 먼저 참석자를 등록하세요.

```bash
# 1단계: 참석자 등록 (speakerId로 사용할 참석자)
curl -X POST http://localhost:8080/api/attendees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "givenName": "서연",
    "surname": "이",
    "email": "seoyeon@conference.com"
  }'
# 응답에서 id 확인 (예: 2)

# 2단계: 제안서 생성
curl -X POST http://localhost:8080/api/proposals \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Pact로 시작하는 계약 테스트",
    "abstract": "Consumer-Driven Contract 테스트의 핵심 개념과 Pact 프레임워크를 사용한 실전 구현 방법을 소개합니다.",
    "speakerId": 2
  }'
```

**응답 예시** (HTTP 201 Created):
```json
{
  "id": 1,
  "title": "Pact로 시작하는 계약 테스트",
  "abstract": "Consumer-Driven Contract 테스트의 핵심 개념과 Pact 프레임워크를 사용한 실전 구현 방법을 소개합니다.",
  "speakerId": 2,
  "status": "SUBMITTED",
  "sessionId": null
}
```

**필드 설명**:
- `title` (필수): 제안서 제목
- `abstract` (필수): 발표 초록 (최대 1000자)
- `speakerId` (필수): 발표자 참석자 ID (AttendeeService에 존재해야 함)

#### 제안서 수정

```bash
curl -X PUT http://localhost:8080/api/proposals/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "title": "Pact로 시작하는 계약 테스트 (업데이트)",
    "abstract": "Consumer-Driven Contract 테스트의 핵심 개념과 실전 사례를 깊이 있게 살펴봅니다.",
    "status": "ACCEPTED",
    "sessionId": 3
  }'
```

**응답**: HTTP 204 No Content

#### 제안서 삭제

```bash
curl -X DELETE http://localhost:8080/api/proposals/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### 투표 추가

```bash
POST /api/proposals/{id}/votes
```

```bash
curl -X POST http://localhost:8080/api/proposals/1/votes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "attendeeId": 1,
    "score": 5
  }'
```

**응답 예시** (HTTP 201 Created):
```json
{
  "id": 1,
  "proposalId": 1,
  "attendeeId": 1,
  "score": 5
}
```

**필드 설명**:
- `attendeeId` (필수): 투표하는 참석자 ID
- `score` (필수): 점수 (1 ~ 5)

#### 투표 결과 조회

```bash
GET /api/proposals/{id}/votes
```

```bash
curl http://localhost:8080/api/proposals/1/votes
```

**응답 예시** (일반 평균 알고리즘):
```json
{
  "votes": [
    {
      "id": 1,
      "proposalId": 1,
      "attendeeId": 1,
      "score": 5
    },
    {
      "id": 2,
      "proposalId": 1,
      "attendeeId": 2,
      "score": 4
    }
  ],
  "averageScore": 4.5
}
```

---

### 5.5 API 버전 관리

Session Service는 URI 경로 기반 API 버전 관리를 제공합니다.

#### V1 API - 기본 필드

```bash
GET /api/v1/sessions
GET /api/v1/sessions/{id}
```

```bash
curl http://localhost:8080/api/v1/sessions
```

**응답 예시**:
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
  "total": 1
}
```

#### V2 API - 확장 필드

```bash
GET /api/v2/sessions
GET /api/v2/sessions/{id}
```

```bash
curl http://localhost:8080/api/v2/sessions
```

**응답 예시** (V1 대비 `tags`, `capacity`, `registeredCount` 추가):
```json
{
  "data": [
    {
      "id": 1,
      "title": "gRPC로 마이크로서비스 구축하기",
      "speaker": "장호",
      "description": "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
      "dateTime": "2024-09-15T10:00:00",
      "tags": ["api"],
      "capacity": 100,
      "registeredCount": 0
    }
  ],
  "total": 1
}
```

**V1 vs V2 비교**:

| 필드 | V1 | V2 |
|------|----|----|
| `id` | O | O |
| `title` | O | O |
| `speaker` | O | O |
| `description` | O | O |
| `dateTime` | O | O |
| `tags` | X | O (제목 기반 자동 추론) |
| `capacity` | X | O (고정값: 100) |
| `registeredCount` | X | O (현재: 0) |

**tags 자동 추론 규칙** (`inferTags` 메서드 기준):
- 제목에 `grpc`, `api`, `rest` 포함 → `api`
- 제목에 `테스트`, `test`, `pact` 포함 → `testing`
- 제목에 `마이크로`, `micro` 포함 → `microservices`
- 제목에 `게이트웨이`, `gateway` 포함 → `infrastructure`
- 제목에 `kotlin`, `spring` 포함 → `framework`
- 위 조건 없음 → `general`

---

### 5.6 에러 응답 (RFC 7807 ProblemDetail)

모든 서비스는 표준 RFC 7807 형식의 에러 응답을 반환합니다.

#### 404 Not Found (존재하지 않는 리소스)

```bash
curl http://localhost:8080/api/sessions/9999
```

```json
{
  "type": "https://conference.example.com/errors/not-found",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "ID 9999인 세션을 찾을 수 없습니다",
  "instance": "/sessions/9999",
  "error": "ID 9999인 세션을 찾을 수 없습니다"
}
```

#### 400 Bad Request (입력값 검증 실패)

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title": "", "speaker": ""}'
```

```json
{
  "type": "https://conference.example.com/errors/validation",
  "title": "Validation Error",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/sessions",
  "errors": {
    "title": "제목은 필수입니다",
    "speaker": "발표자는 필수입니다"
  }
}
```

#### 401 Unauthorized (토큰 누락)

```bash
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -d '{"title": "테스트", "speaker": "발표자"}'
```

```json
{
  "type": "https://conference.example.com/errors/unauthorized",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authorization header required for write operations"
}
```

---

## 6. Pact 계약 테스트 따라하기 (핵심!)

### 6.1 계약 테스트란?

#### 마이크로서비스 환경의 문제

마이크로서비스 아키텍처에서 서비스 간 API 변경은 의도치 않은 장애를 유발합니다. 예를 들어:

- Attendee Service가 Session Service의 `GET /sessions/1`을 호출
- Session Service 팀이 응답 필드명 `title`을 `name`으로 변경
- Attendee Service는 변경 사실을 모르고 계속 `title` 필드를 읽으려 함
- 런타임에 에러 발생

이런 문제를 **별도의 E2E 테스트 없이** 잡는 방법이 계약 테스트입니다.

#### E2E 테스트 vs 계약 테스트 비교

| 비교 항목 | E2E 테스트 | 계약 테스트 |
|-----------|-----------|------------|
| 테스트 속도 | 느림 (분 단위) | 빠름 (초 단위) |
| 안정성 | 불안정 (네트워크, DB 등) | 안정적 (MockServer 사용) |
| 실패 원인 파악 | 어려움 | 명확함 (어떤 계약 위반인지) |
| 서비스 의존 | 모든 서비스 실행 필요 | 각 서비스 독립 실행 |
| CI/CD 비용 | 높음 | 낮음 |

#### Consumer-Driven Contract (CDC)

```
[Consumer: AttendeeService]          [Provider: SessionService]
        |                                       |
        | 1. "나는 /sessions/1에 GET 하면       |
        |    {id, title, speaker} 를 받길       |
        |    원해" 라는 계약을 작성             |
        |                                       |
        | 2. Pact가 계약을 JSON 파일로 저장     |
        |    → build/pacts/AttendeeService-SessionService.json
        |                                       |
        |    3. Provider가 JSON 파일을 읽고     |
        |       "우리 서비스가 이 계약을        |
        |        지킬 수 있는가?" 검증          |
        v                                       v
     Consumer 테스트 통과                Provider 테스트 통과
              \                              /
               \__ 양쪽 모두 통과 = 안전하게 배포 가능 __/
```

### 6.2 Consumer 테스트 작성 이해하기

`attendee-service/src/test/kotlin/com/conference/attendee/pact/SessionServiceConsumerPactTest.kt`

#### 핵심 구조 설명

```kotlin
@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)
class SessionServiceConsumerPactTest {

    // @Pact 메서드: "나는 이런 요청을 보내면 이런 응답을 기대한다"는 계약 정의
    @Pact(consumer = "AttendeeService")
    fun getSessionPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("세션 ID 1이 존재함")     // Provider State: 사전 조건
            .uponReceiving("세션 1 조회 요청") // 이 요청을 받으면
            .path("/sessions/1")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()               // 이런 응답을 기대함
            .status(200)
            .body(
                PactDslJsonBody()
                    .integerType("id", 1L)           // 타입 매칭 (정확한 값 아님)
                    .stringType("title", "예시 제목") // 문자열이면 통과
                    .stringType("speaker", "예시 발표자")
            )
            .toPact()
    }

    // @PactTestFor: MockServer를 주입받아 실제 클라이언트 코드 테스트
    @Test
    @PactTestFor(pactMethod = "getSessionPact")
    fun `세션 단건 조회 시 올바른 세션 정보를 반환한다`(mockServer: MockServer) {
        // MockServer가 계약에 정의된 응답을 반환
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = SessionClient(restClient)

        val session = client.getSession(1)

        // 클라이언트가 응답을 올바르게 파싱하는지 검증
        assertThat(session!!.id).isEqualTo(1)
        assertThat(session.title).isEqualTo("gRPC로 마이크로서비스 구축하기")
    }
}
```

#### 핵심 개념: 타입 매칭 vs 값 매칭

- `integerType("id", 1L)`: id가 정수 타입이면 통과 (1이 아니어도 됨)
- `stringType("title", "예시")`: title이 문자열 타입이면 통과
- `equalTo("status", 200)`: 정확히 200이어야 통과

계약 테스트에서는 **타입 매칭**을 권장합니다. 실제 값보다 구조와 타입이 중요합니다.

#### 생성되는 Pact JSON 파일 위치

Consumer 테스트 실행 후:
- `attendee-service/build/pacts/AttendeeService-SessionService.json`
- `cfp-service/build/pacts/CfpService-SessionService.json`
- `cfp-service/build/pacts/CfpService-AttendeeService.json`

### 6.3 Provider 테스트 작성 이해하기

`session-service/src/test/kotlin/com/conference/session/pact/SessionServiceProviderPactTest.kt`

#### 핵심 구조 설명

```kotlin
@Provider("SessionService")          // 이 서비스의 이름 (Consumer가 지정한 이름과 일치해야 함)
@PactFolder("build/pacts")           // Consumer가 생성한 pact 파일 위치
@IgnoreNoPactsToVerify               // pact 파일이 없을 때 테스트 스킵 (에러 아님)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionServiceProviderPactTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sessionStore: SessionStoreInterface

    @BeforeEach
    fun setUp(context: PactVerificationContext?) {
        // 실제 실행 중인 서버를 테스트 대상으로 지정
        context?.target = HttpTestTarget("localhost", port)
    }

    // 계약에 정의된 모든 상호작용을 자동으로 실행
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext?) {
        context?.verifyInteraction()
    }

    // @State: Consumer가 정의한 "사전 조건"을 실제 데이터로 구성
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

    @State("세션 ID 999가 존재하지 않음")
    fun sessionWithId999DoesNotExist() {
        sessionStore.clear()
        // 저장소를 비워서 999가 없는 상태 만들기
    }
}
```

#### @IgnoreNoPactsToVerify 사용 이유

Consumer 테스트를 먼저 실행하지 않으면 `build/pacts/` 폴더에 파일이 없습니다. 이때 테스트가 실패하는 대신 스킵되도록 합니다. 올바른 실행 순서:

1. Consumer 테스트 실행 → pact 파일 생성
2. `collectPacts` task로 파일 수집
3. Provider 테스트 실행 → 계약 검증

### 6.4 Pact 파일 수집 (collectPacts)

Session Service가 Provider이므로, 모든 Consumer(AttendeeService, CfpService)의 pact 파일을 한 곳에 모아야 합니다.

```bash
# Consumer 테스트 먼저 실행 (pact 파일 생성)
./gradlew :attendee-service:test --tests "*SessionServiceConsumerPactTest"
./gradlew :cfp-service:test --tests "*SessionServiceConsumerPactTest"

# pact 파일 수집
./gradlew :session-service:collectPacts
```

수집 결과:
- `attendee-service/build/pacts/AttendeeService-SessionService.json` → `session-service/build/pacts/`
- `cfp-service/build/pacts/CfpService-SessionService.json` → `session-service/build/pacts/`

`session-service/build.gradle.kts`에 정의된 내용:
```kotlin
tasks.register<Copy>("collectPacts") {
    from("../attendee-service/build/pacts") {
        include("*-SessionService.json")
    }
    from("../cfp-service/build/pacts") {
        include("*-SessionService.json")
    }
    into("build/pacts")
}
```

### 6.5 Pact Broker 사용

Pact Broker는 계약 파일을 중앙에서 관리하고, CI/CD에서 `can-i-deploy`를 사용할 수 있게 해줍니다.

#### Pact Broker 시작

```bash
# Pact Broker + PostgreSQL 시작
docker compose up -d

# 시작 확인
docker compose ps
# 브라우저: http://localhost:9292 (사용자: pact / 비밀번호: pact)
```

#### Pact 파일 게시 (pactPublish)

`session-service/build.gradle.kts`나 각 Consumer 모듈의 gradle 설정에 pactPublish 태스크를 추가하거나, 아래 방법으로 수동 게시합니다:

```bash
# Consumer 테스트 실행 + pact 파일 게시
./gradlew :attendee-service:test :cfp-service:test pactPublish \
  -Ppact.broker.url=http://localhost:9292 \
  -Ppact.broker.username=pact \
  -Ppact.broker.password=pact
```

#### 브로커 UI에서 계약 확인

1. `http://localhost:9292` 접속
2. 사용자명/비밀번호: `pact` / `pact`
3. Consumer-Provider 관계 및 계약 파일 확인

#### can-i-deploy

배포 전 모든 관련 서비스와의 계약이 충족되는지 확인합니다.

```bash
# pact-broker CLI 설치
npm install -g @pact-foundation/pact-node

# SessionService를 배포해도 되는지 확인
pact-broker can-i-deploy \
  --pacticipant SessionService \
  --version 0.0.1-SNAPSHOT \
  --to-environment production \
  --broker-base-url http://localhost:9292 \
  --broker-username pact \
  --broker-password pact
```

---

## 7. 테스트 실행 가이드

### 7.1 전체 테스트 실행

```bash
# 모든 모듈 테스트 (Consumer 테스트 → collectPacts → Provider 테스트 순서 자동)
./gradlew test
```

### 7.2 특정 모듈 테스트

```bash
# Session Service 테스트만
./gradlew :session-service:test

# Attendee Service 테스트만
./gradlew :attendee-service:test

# CFP Service 테스트만
./gradlew :cfp-service:test
```

### 7.3 특정 테스트 클래스 실행

```bash
# Session Service 컨트롤러 테스트
./gradlew :session-service:test --tests "*SessionControllerTest"

# Pact Consumer 테스트 (AttendeeService → SessionService 계약)
./gradlew :attendee-service:test --tests "*SessionServiceConsumerPactTest"

# Pact Provider 테스트 (SessionService 계약 검증)
./gradlew :session-service:test --tests "*SessionServiceProviderPactTest"

# API 버전 관리 테스트
./gradlew :session-service:test --tests "*SessionVersioningTest"

# CFP Feature Flag 테스트
./gradlew :cfp-service:test --tests "*FeatureFlagTest"

# CFP Attendee Consumer 테스트
./gradlew :cfp-service:test --tests "*AttendeeServiceConsumerPactTest"
```

### 7.4 계약 테스트 전체 흐름 실행

```bash
# 1단계: Consumer 테스트 (pact 파일 생성)
./gradlew :attendee-service:test --tests "*Consumer*"
./gradlew :cfp-service:test --tests "*Consumer*"

# 2단계: pact 파일 수집
./gradlew :session-service:collectPacts

# 3단계: Provider 테스트 (계약 검증)
./gradlew :session-service:test --tests "*Provider*"
./gradlew :attendee-service:test --tests "*Provider*"
```

### 7.5 테스트 리포트 확인

```bash
# 테스트 실행 후 HTML 리포트 경로
open session-service/build/reports/tests/test/index.html
open attendee-service/build/reports/tests/test/index.html
open cfp-service/build/reports/tests/test/index.html
```

### 7.6 통합 테스트 (Testcontainers)

Session Service에는 PostgreSQL Testcontainers를 사용한 통합 테스트가 포함됩니다.

```bash
# Docker 실행 중인지 확인 후
./gradlew :session-service:test --tests "*SessionRepositoryIntegrationTest"
```

---

## 8. Feature Flag 사용법

CFP Service는 투표 알고리즘 전환을 Feature Flag로 제어합니다.

### 8.1 설정 구조

`cfp-service/src/main/kotlin/com/conference/cfp/config/FeatureFlags.kt`:
```kotlin
@ConfigurationProperties(prefix = "features")
data class FeatureFlags(
    val newVotingAlgorithm: Boolean = false,  // 가중 투표 알고리즘
    val detailedLogging: Boolean = false       // 상세 로깅
)
```

### 8.2 새 투표 알고리즘 활성화

`cfp-service/src/main/resources/application.yml`:
```yaml
features:
  new-voting-algorithm: false
  detailed-logging: false
```

런타임 파라미터로 활성화:

```bash
# Gradle bootRun으로 활성화
./gradlew :cfp-service:bootRun \
  --args='--features.new-voting-algorithm=true'

# JAR 직접 실행
java -jar cfp-service/build/libs/cfp-service-0.0.1-SNAPSHOT.jar \
  --features.new-voting-algorithm=true
```

### 8.3 투표 알고리즘 차이

```bash
# 기본 알고리즘 (features.new-voting-algorithm=false)
curl http://localhost:8080/api/proposals/1/votes
# averageScore: 단순 평균 (점수 합계 / 투표 수)

# 가중 투표 알고리즘 (features.new-voting-algorithm=true)
curl http://localhost:8080/api/proposals/1/votes
# averageScore: 가중 평균 (최신 투표에 더 높은 가중치)
```

### 8.4 상세 로깅 활성화

```bash
./gradlew :cfp-service:bootRun \
  --args='--features.detailed-logging=true'
```

### 8.5 환경 변수로 활성화 (운영 환경)

```bash
FEATURES_NEW_VOTING_ALGORITHM=true \
FEATURES_DETAILED_LOGGING=true \
java -jar cfp-service/build/libs/cfp-service-0.0.1-SNAPSHOT.jar
```

---

## 9. JPA 모드로 실행하기

기본적으로 Session Service는 In-memory 저장소(ConcurrentHashMap)를 사용합니다. `jpa` Profile을 활성화하면 PostgreSQL을 사용하는 JPA 저장소로 전환됩니다. 이것이 **Branch by Abstraction** 패턴의 예시입니다.

### 9.1 Branch by Abstraction 패턴

```
SessionStoreInterface (추상화 레이어)
        |
        |── SessionStore.kt      (@Profile("default") → In-memory)
        └── JpaSessionStore.kt   (@Profile("jpa") → PostgreSQL)
```

`SessionController`는 `SessionStoreInterface`에만 의존합니다. Profile 전환만으로 저장소 구현체가 바뀝니다.

### 9.2 PostgreSQL 설정

```bash
# Docker로 PostgreSQL 시작
docker run -d \
  --name conference-postgres \
  -e POSTGRES_USER=conference \
  -e POSTGRES_PASSWORD=conference \
  -e POSTGRES_DB=session_db \
  -p 5432:5432 \
  postgres:17-alpine
```

### 9.3 JPA Profile로 실행

```bash
# Gradle bootRun
./gradlew :session-service:bootRun \
  --args='--spring.profiles.active=jpa --spring.datasource.url=jdbc:postgresql://localhost:5432/session_db --spring.datasource.username=conference --spring.datasource.password=conference'

# JAR 직접 실행
java -jar session-service/build/libs/session-service-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=jpa \
  --spring.datasource.url=jdbc:postgresql://localhost:5432/session_db \
  --spring.datasource.username=conference \
  --spring.datasource.password=conference
```

### 9.4 H2 인메모리 DB (테스트용)

별도 PostgreSQL 없이 H2를 사용할 수도 있습니다:

```bash
./gradlew :session-service:bootRun \
  --args='--spring.profiles.active=jpa --spring.datasource.url=jdbc:h2:mem:testdb --spring.datasource.driver-class-name=org.h2.Driver --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect'
```

---

## 10. 모니터링 & 대시보드

### 10.1 HTML 대시보드

서비스 실행 후 로컬 HTML 대시보드를 브라우저에서 열 수 있습니다:

```bash
# macOS
open dashboard/index.html

# Linux
xdg-open dashboard/index.html

# 또는 브라우저 주소창에 직접 입력
# file:///path/to/pact-conference-demo-phase1/dashboard/index.html
```

### 10.2 Swagger UI

각 서비스의 OpenAPI 문서를 브라우저에서 확인하고 직접 API를 호출할 수 있습니다.

| 서비스 | Swagger UI URL |
|--------|----------------|
| Attendee Service | http://localhost:8081/swagger-ui.html |
| Session Service | http://localhost:8082/swagger-ui.html |
| CFP Service | http://localhost:8083/swagger-ui.html |

### 10.3 Actuator 엔드포인트

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health

# Prometheus 메트릭 (raw 형식)
curl http://localhost:8082/actuator/prometheus

# 메트릭 목록
curl http://localhost:8082/actuator/metrics

# Gateway 라우팅 정보
curl http://localhost:8080/actuator/gateway/routes
```

### 10.4 Prometheus + Grafana 설정

```bash
# Pact Broker + 모니터링 스택 함께 시작
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

| 도구 | URL | 인증 |
|------|-----|------|
| Pact Broker | http://localhost:9292 | pact / pact |
| Prometheus | http://localhost:9090 | 없음 |
| Grafana | http://localhost:3000 | admin / admin |

**Prometheus 메트릭 수집 설정** (`monitoring/prometheus.yml`):
- 각 서비스의 `/actuator/prometheus` 엔드포인트를 스크레이핑합니다
- `host.docker.internal`을 통해 컨테이너에서 로컬호스트 서비스에 접근합니다

**Grafana 대시보드**:
- 사전 구성된 컨퍼런스 대시보드 (`monitoring/grafana/dashboards/conference.json`)가 자동으로 로드됩니다
- 대시보드에서 HTTP 요청 수, 응답 시간, JVM 메트릭을 확인할 수 있습니다

```bash
# 모니터링 스택만 종료
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml down
```

---

## 11. Docker & Kubernetes 배포

### 11.1 Docker 빌드

각 서비스는 Dockerfile 없이 Spring Boot의 `bootBuildImage`를 사용합니다:

```bash
# Session Service Docker 이미지 빌드
./gradlew :session-service:bootBuildImage \
  --imageName=conference/session-service:latest

# 모든 서비스 이미지 빌드
./gradlew bootBuildImage
```

직접 Dockerfile로 빌드하려면:

```bash
# 먼저 JAR 빌드
./gradlew :session-service:bootJar

# Docker 빌드 (Dockerfile이 있다면)
docker build \
  -t conference/session-service:latest \
  -f session-service/Dockerfile \
  ./session-service
```

### 11.2 Docker Compose로 Pact Broker 실행

```bash
# Pact Broker + PostgreSQL 시작
docker compose up -d

# 상태 확인
docker compose ps
docker compose logs pact-broker

# 종료
docker compose down
docker compose down -v  # 볼륨도 함께 삭제
```

### 11.3 Kubernetes 배포

`k8s/` 디렉토리에 각 서비스의 Deployment 및 Service 매니페스트가 있습니다.

```bash
# 네임스페이스 생성
kubectl create namespace conference

# 모든 서비스 배포
kubectl apply -f k8s/

# 특정 서비스만 배포
kubectl apply -f k8s/session-service-deployment.yaml
kubectl apply -f k8s/attendee-service-deployment.yaml
kubectl apply -f k8s/cfp-service-deployment.yaml
kubectl apply -f k8s/gateway-deployment.yaml

# Pact Broker 배포
kubectl apply -f k8s/pact-broker-deployment.yaml

# 배포 상태 확인
kubectl get pods -n conference
kubectl get services -n conference

# 로그 확인
kubectl logs -n conference -l app=session-service -f
```

---

## 12. 성능 테스트

### 12.1 Apache Bench 사용

```bash
# 기본 실행 (10 concurrent, 100 requests)
./performance-test/load-test.sh

# 커스텀 파라미터 (50 concurrent, 1000 requests)
./performance-test/load-test.sh http://localhost:8080 50 1000
```

스크립트가 테스트하는 엔드포인트:
- `GET /api/sessions` (Session Service)
- `GET /api/attendees` (Attendee Service)
- `GET /api/proposals` (CFP Service)
- `GET /api/v1/sessions` (V1 API)
- `GET /api/v2/sessions` (V2 API)

출력 예시:
```
--- Test 1: GET /api/sessions ---
Requests per second:    523.45 [#/sec] (mean)
Time per request:       19.103 [ms] (mean)
Failed requests:        0
```

### 12.2 k6 사용

k6는 더 정교한 부하 테스트를 지원합니다 (ramp-up/down, 임계값 설정).

```bash
# k6 설치 (macOS)
brew install k6

# 기본 실행
k6 run performance-test/k6-load-test.js

# 커스텀 BASE_URL
k6 run -e BASE_URL=http://localhost:8080 performance-test/k6-load-test.js

# HTML 리포트 생성
k6 run --out csv=results.csv performance-test/k6-load-test.js
```

**k6 부하 시나리오** (`k6-load-test.js`):

| 단계 | 기간 | 목표 VU |
|------|------|---------|
| Ramp up | 10s | 0 → 10 |
| Steady | 30s | 10 |
| Spike | 10s | 10 → 50 |
| Recover | 10s | 50 → 10 |
| Ramp down | 10s | 10 → 0 |

**성능 임계값**:
- p(95) 응답 시간 < 500ms
- 오류율 < 10%

### 12.3 결과 해석

- **Requests per second (RPS)**: 서비스의 처리 능력 지표
- **p(95) latency**: 95번째 백분위 응답 시간 (SLA 기준으로 많이 사용)
- **Failed requests**: 0이어야 정상
- Prometheus + Grafana로 부하 테스트 중 메트릭 시각화 가능

---

## 13. 직접 계약 추가해보기 (실습 가이드)

### 13.1 새로운 Consumer 테스트 작성 (Step-by-step)

가상 시나리오: "CFP Service가 Session Service에서 세션 목록 전체를 가져온다"는 새 계약 추가

**Step 1**: CFP Service의 Consumer Pact 테스트에 새 `@Pact` 메서드 추가

`cfp-service/src/test/kotlin/com/conference/cfp/pact/SessionServiceConsumerPactTest.kt`에 추가:

```kotlin
@Pact(consumer = "CfpService")
fun getSessionsPact(builder: PactDslWithProvider): RequestResponsePact {
    val responseBody = PactDslJsonBody()
        .integerType("total")
        .eachLike("data", 1)            // data 배열에 1개 이상 항목
            .integerType("id")
            .stringType("title", "예시 세션")
            .stringType("speaker", "예시 발표자")
            .closeArray()!! as PactDslJsonBody

    return builder
        .given("세션 목록이 존재함")
        .uponReceiving("전체 세션 목록 조회 요청 - CfpService")
        .path("/sessions")
        .method("GET")
        .headers("Accept", "application/json")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body(responseBody)
        .toPact()
}

@Test
@PactTestFor(pactMethod = "getSessionsPact")
fun `세션 목록 조회 시 세션 리스트를 반환한다`(mockServer: MockServer) {
    val restClient = RestClient.builder()
        .baseUrl(mockServer.getUrl())
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build()
    val client = SessionClient(restClient)

    // CfpService의 SessionClient에 getSessions() 메서드가 필요
    // val sessions = client.getSessions()
    // assertThat(sessions).isNotEmpty()
}
```

**Step 2**: Consumer 테스트 실행 (pact 파일 생성)

```bash
./gradlew :cfp-service:test --tests "*SessionServiceConsumerPactTest"
```

생성 확인:
```bash
cat cfp-service/build/pacts/CfpService-SessionService.json
```

**Step 3**: pact 파일 수집

```bash
./gradlew :session-service:collectPacts
```

**Step 4**: Provider 테스트 실행 (기존 `@State("세션 목록이 존재함")`이 이미 있으므로 추가 작업 불필요)

```bash
./gradlew :session-service:test --tests "*SessionServiceProviderPactTest"
```

### 13.2 Provider 테스트 업데이트

새로운 Provider State가 필요한 경우, `SessionServiceProviderPactTest.kt`에 `@State` 메서드 추가:

```kotlin
@State("특정 조건의 세션이 존재함")
fun specificSessionCondition() {
    sessionStore.clear()
    sessionStore.addSession(Session(
        title = "새로운 테스트 세션",
        speaker = "테스트 발표자"
    ))
}
```

### 13.3 의도적 계약 위반 시나리오 실습

계약 테스트의 힘을 느끼려면 의도적으로 계약을 위반해보세요.

**실습 1**: Provider가 응답 필드를 제거

`SessionController.kt`의 `SessionResponse`에서 `speaker` 필드를 제거하고 Provider 테스트를 실행하면:

```
Pact Test failed:
Expected: speaker='장호'
Actual: speaker=null (field not present)
```

**실습 2**: Provider가 응답 타입 변경

`id` 필드를 String으로 변경하면:

```
Pact Test failed:
Expected: id to be an Integer
Actual: id was a String
```

**실습 3**: Provider가 HTTP 상태 코드 변경

404 응답을 200으로 변경하면:

```
Pact Test failed:
Expected status: 404
Actual status: 200
```

---

## 14. 자주 묻는 질문 (FAQ)

**Q1. 서비스 시작 후 `/api/sessions` 요청이 실패합니다.**

A. Gateway가 Session Service로 라우팅하는데, Session Service가 아직 시작 중일 수 있습니다. `http://localhost:8082/actuator/health`가 `UP`을 반환하는지 먼저 확인하세요.

**Q2. GET 요청은 되는데 POST 요청에서 401이 납니다.**

A. Gateway를 통한 쓰기 작업은 JWT 토큰이 필요합니다. `POST /api/auth/token`으로 토큰을 먼저 발급받고 `Authorization: Bearer {token}` 헤더를 추가하세요. 또는 직접 서비스 포트(8081~8083)로 요청하면 인증 없이 사용 가능합니다.

**Q3. Provider 테스트가 "No pacts found" 메시지와 함께 스킵됩니다.**

A. Consumer 테스트를 먼저 실행해서 pact 파일을 생성하고, `collectPacts`를 실행해야 합니다. `@IgnoreNoPactsToVerify` 어노테이션이 있어 에러가 아닌 스킵으로 처리됩니다.

**Q4. Provider 테스트가 실패합니다. 어떻게 디버깅하나요?**

A. Consumer 테스트 실행 → `collectPacts` → Provider 테스트 순서로 진행하세요. `pact.verifier.publishResults=false` 설정으로 검증 결과 출력을 비활성화하고 에러 메시지를 자세히 확인하세요.

**Q5. Pact 파일의 내용을 직접 보고 싶습니다.**

A. `build/pacts/` 디렉토리의 JSON 파일을 열어보세요:
```bash
cat attendee-service/build/pacts/AttendeeService-SessionService.json | python3 -m json.tool
```

**Q6. JPA 모드로 실행 시 테이블이 자동 생성되지 않습니다.**

A. `spring.jpa.hibernate.ddl-auto=create-drop` 또는 `update` 설정을 추가하세요. Session Service는 기본적으로 `create-drop`을 사용합니다.

**Q7. 포트 8080이 이미 사용 중입니다.**

A. `lsof -ti:8080 | xargs kill -9` 명령으로 해당 포트를 점유한 프로세스를 종료하거나, `--server.port=8090`과 같이 다른 포트를 지정하세요.

**Q8. `./gradlew` 실행 권한이 없습니다.**

A. `chmod +x gradlew` 명령으로 실행 권한을 부여하세요.

**Q9. V1과 V2 API의 차이가 무엇인가요?**

A. V2는 V1의 모든 필드를 포함하면서 `tags`, `capacity`, `registeredCount` 세 필드를 추가합니다. 기존 V1 Consumer는 변경 없이 계속 V1을 사용할 수 있습니다.

**Q10. Feature Flag를 코드 변경 없이 활성화하려면 어떻게 하나요?**

A. 애플리케이션 시작 시 `--features.new-voting-algorithm=true` 인자를 전달하거나 환경변수 `FEATURES_NEW_VOTING_ALGORITHM=true`를 설정하세요.

**Q11. CFP Service에서 제안서 생성 시 404가 발생합니다.**

A. `speakerId`로 지정한 참석자 ID가 AttendeeService에 존재해야 합니다. 먼저 `POST /api/attendees`로 참석자를 등록하고, 반환된 `id`를 `speakerId`로 사용하세요.

**Q12. 투표 점수 범위는 얼마인가요?**

A. 1에서 5 사이의 정수만 허용됩니다. 범위를 벗어나면 400 Validation Error가 반환됩니다.

**Q13. Pact Broker에 계약을 게시하려면 Docker가 필요한가요?**

A. 로컬 파일 기반으로도 계약 테스트는 동작합니다. Docker 기반 Pact Broker는 CI/CD 환경에서 계약을 중앙 관리하고 `can-i-deploy`를 사용하기 위한 선택 사항입니다.

**Q14. 서비스 로그는 어디서 확인하나요?**

A. `start-all.sh`로 시작한 경우 `/tmp/` 디렉토리에 로그 파일이 생성됩니다:
```bash
tail -f /tmp/session-service.log
tail -f /tmp/attendee-service.log
tail -f /tmp/cfp-service.log
tail -f /tmp/gateway.log
```

**Q15. `SessionStore`의 초기 데이터를 변경하려면?**

A. `session-service/src/main/kotlin/com/conference/session/store/SessionStore.kt`에서 `store` 초기화 블록을 수정하세요. 서비스 재시작 후 반영됩니다.

---

## 15. 트러블슈팅

### 15.1 빌드 실패

**증상**: `./gradlew build`가 실패합니다.

```
해결 방법:
1. JDK 버전 확인
   java -version  # 21 이상이어야 함

2. 캐시 삭제 후 재빌드
   ./gradlew clean build

3. Gradle wrapper 재다운로드
   ./gradlew wrapper --gradle-version 8.10
```

**증상**: `Kotlin compilation error`

```
해결 방법:
1. IntelliJ의 경우: File > Invalidate Caches > Invalidate and Restart
2. 터미널: ./gradlew clean :모듈명:compileKotlin
```

### 15.2 테스트 실패

**증상**: `NoPactsFoundException` 발생

```
원인: Provider 테스트 실행 전 pact 파일이 없음

해결 방법:
# @IgnoreNoPactsToVerify 어노테이션이 있으면 에러가 아닌 스킵
# 파일을 생성하려면:
./gradlew :attendee-service:test --tests "*Consumer*"
./gradlew :session-service:collectPacts
```

**증상**: `Docker not available` (Testcontainers 테스트)

```
원인: Docker 데몬이 실행 중이지 않음

해결 방법:
- macOS: Docker Desktop 실행
- Linux: sudo systemctl start docker
- Testcontainers 없이 테스트: ./gradlew test -x integrationTest
```

**증상**: Pact Provider 테스트 실패 - 필드 불일치

```
원인: SessionResponse 필드가 Consumer가 기대하는 것과 다름

해결 방법:
1. 에러 메시지에서 어떤 필드가 문제인지 확인
2. SessionController의 응답 DTO와 Consumer 계약 비교
3. Consumer 계약 수정이 아닌, Provider 응답을 계약에 맞게 수정
```

### 15.3 포트 충돌

**증상**: `Port 8082 is already in use`

```bash
# 포트를 사용 중인 프로세스 확인 (macOS/Linux)
lsof -i :8082

# 프로세스 종료
lsof -ti:8082 | xargs kill -9

# 또는 stop-all.sh 실행
./stop-all.sh
```

**증상**: Gateway 라우팅이 작동하지 않음 (502 Bad Gateway)

```bash
# 각 서비스 헬스 확인
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# Gateway 로그 확인
tail -f /tmp/gateway.log

# 서비스가 완전히 시작될 때까지 약 30초 대기 필요
```

### 15.4 JWT 인증 오류

**증상**: `Invalid or expired token` (401)

```bash
# 토큰 재발급
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "roles": ["ADMIN"]}'

# 토큰 유효 시간: 1시간
# 만료 후 재발급 필요
```

**증상**: `Authorization header required` (401)

```bash
# -H "Authorization: Bearer {token}" 헤더가 누락된 경우
# 토큰 앞에 "Bearer " (공백 포함)를 붙여야 함
curl -X POST http://localhost:8080/api/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{...}'
```

### 15.5 Pact Broker 연결 실패

**증상**: Pact Broker UI가 열리지 않음 (`http://localhost:9292`)

```bash
# Docker Compose 상태 확인
docker compose ps

# 로그 확인
docker compose logs pact-broker
docker compose logs postgres

# 재시작
docker compose down
docker compose up -d

# PostgreSQL이 완전히 시작될 때까지 약 30초 소요
```

### 15.6 CFP Service 제안서 생성 실패

**증상**: `Attendee with id X not found` (404)

```bash
# AttendeeService 실행 확인
curl http://localhost:8081/actuator/health

# speakerId가 존재하는 참석자인지 확인
curl http://localhost:8081/attendees/1

# 참석자가 없다면 먼저 생성
curl -X POST http://localhost:8080/api/attendees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"givenName": "발표자", "surname": "이름", "email": "speaker@example.com"}'
```

---

## 16. 프로젝트 학습 로드맵

### 16.1 추천 학습 순서

```
1단계: 서비스 기동 및 API 탐색 (1~2시간)
  ├── start-all.sh 실행
  ├── Swagger UI에서 각 API 탐색
  └── curl로 모든 CRUD 엔드포인트 직접 호출

2단계: 코드 이해 (2~3시간)
  ├── common 모듈: Session, Attendee 도메인 모델
  ├── SessionController → SessionStore → SessionStoreInterface
  ├── AttendeeController → SessionClient (서비스 간 호출)
  └── Gateway: JwtAuthFilter, application.yml 라우팅

3단계: 계약 테스트 이해 (2~3시간)
  ├── SessionServiceConsumerPactTest 코드 분석
  ├── ./gradlew :attendee-service:test --tests "*Consumer*" 실행
  ├── build/pacts/*.json 파일 내용 확인
  ├── collectPacts 실행
  └── SessionServiceProviderPactTest 실행

4단계: 계약 위반 실습 (1~2시간)
  ├── SessionResponse 필드 변경 후 Provider 테스트 실행
  ├── 에러 메시지 분석
  └── 수정 후 재검증

5단계: 고급 기능 (2~3시간)
  ├── API 버전 관리 (V1 vs V2 비교)
  ├── Feature Flag 활성화 및 투표 알고리즘 차이 확인
  ├── JPA 프로파일로 전환
  └── Prometheus + Grafana 모니터링 확인

6단계: Pact Broker 연동 (1시간)
  ├── docker compose up -d
  ├── Pact Broker UI 탐색
  └── can-i-deploy 실습
```

### 16.2 "Mastering API Architecture" 관련 챕터 매핑

| 챕터 | 주제 | 관련 코드 |
|------|------|----------|
| Chapter 1-2 | API 설계 원칙 | `common/model/`, `GlobalExceptionHandler` (RFC 7807) |
| Chapter 3-4 | API 게이트웨이 | `gateway/` 전체, `JwtAuthFilter`, `application.yml` 라우팅 |
| Chapter 5 | 계약 테스트 | `*/pact/` 하위 모든 테스트 파일 |
| Chapter 6 | API 버전 관리 | `SessionV1Controller`, `SessionV2Controller`, `SessionV2Dto` |
| Chapter 7 | 서비스 디스커버리 | Gateway 라우팅 설정 |
| Chapter 8 | 모니터링 | `MetricsConfig`, `docker-compose.monitoring.yml` |
| Chapter 9 | 배포 전략 | `k8s/`, `docker-compose.yml`, Feature Flags |

### 16.3 추가 학습 자료

| 자료 | 설명 | URL |
|------|------|-----|
| Pact 공식 문서 | JVM Pact 사용 가이드 | https://docs.pact.io/implementation_guides/jvm |
| Pact Specification | Pact JSON 형식 명세 | https://github.com/pact-foundation/pact-specification |
| Spring Cloud Gateway | MVC 기반 게이트웨이 | https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-mvc.html |
| RFC 7807 | ProblemDetail 표준 | https://www.rfc-editor.org/rfc/rfc7807 |
| k6 문서 | 성능 테스트 | https://k6.io/docs/ |
| Testcontainers | JVM 통합 테스트 | https://testcontainers.com/guides/getting-started-with-testcontainers-for-java/ |

---

*이 가이드는 "Mastering API Architecture" 책의 예제를 기반으로 작성되었습니다. 코드 수정이나 질문이 있으면 프로젝트 README를 참고하거나 이슈를 등록해주세요.*

---

> 세션: USER_GUIDE.md 전면 재작성 (Phase 1~4 전체 커버)
> 최근 작업:
> 1. USER_GUIDE.md 완전 재작성 요청
> 2. 소스 파일 분석 (컨트롤러, DTO, 설정 파일 등)
> 3. 기존 USER_GUIDE.md 구조 파악

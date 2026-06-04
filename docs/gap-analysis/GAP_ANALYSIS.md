# Mastering API Architecture vs pact-conference-demo 갭 분석서

> **작성일**: 2026-06-05
> **대상**: pact-conference-demo 프로젝트
> **원서**: Mastering API Architecture — James Gough, Daniel Bryant, Matthew Auburn
> **분석 목적**: 책에서 다루는 주제 중 현재 구현에 누락된 항목을 식별하고 구현 우선순위를 정한다

---

## 1. 현재 구현 현황 요약

| 항목 | 현재 상태 |
|------|----------|
| **서비스** | attendee-service (Consumer, :8080), session-service (Provider, :8081) |
| **공유 모듈** | common — Attendee, Session, ApiResponse, GlobalExceptionHandler |
| **테스트** | Pact Consumer 3건, Pact Provider 3건, Unit Test 6건 |
| **API 문서** | springdoc-openapi 2.3.0 (Swagger UI 자동 생성) |
| **인프라** | Docker Compose (Pact Broker + PostgreSQL만) |
| **저장소** | ConcurrentHashMap 인메모리 (DB 없음) |
| **인증/인가** | 없음 |
| **API Gateway** | 없음 |
| **Feature Flag** | 없음 |
| **DDD 패턴** | 없음 |
| **대시보드** | 정적 HTML/JS (부분 구현) |
| **기술 스택** | Kotlin 1.9.25, Spring Boot 3.3.6, JDK 21+, Pact JVM 4.6.14 |

**총 소스 코드**: ~615줄 (경량 학습 프로젝트)

---

## 2. 챕터별 갭 분석

### Part 1: API 기초

#### Chapter 0: API 아키텍처 설계 여정

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| Conference 시스템 사례 | O 부분 | Attendee/Session만 존재, **CFP/Vote 서비스 미구현** | CODE |
| C4 다이어그램 | X | 4레벨 아키텍처 다이어그램 부재 | DOC |
| ADR (Architecture Decision Records) | X | 아키텍처 의사결정 기록 부재 | DOC |
| 트래픽 패턴 (N-S/E-W) | X | 남북/동서 트래픽 구분 시연 불가 | DOC/CODE |
| 시스템 진화 단계 (모놀리스→마이크로서비스) | X | 단계별 전환 과정 미시연 | DOC |

#### Chapter 1: API 설계, 구현, 명세

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| REST API (Richardson Level 2) | O 부분 | HTTP 메서드/상태 코드 사용 중이나, AttendeeController POST에 Location 헤더 누락 | CODE |
| gRPC + Protocol Buffers | X | REST만 사용, 서비스 간 gRPC 통신 미구현 | CODE/DOC |
| GraphQL | X | 미구현 (선택적) | CODE |
| OpenAPI 명세 강화 | X | `@Operation`, `@ApiResponse` 어노테이션 미사용, 설명 빈약 | CODE |
| OpenAPI Spec-First Design | X | 코드 우선 생성만 사용 | DOC |
| openapi-diff (Breaking Change 감지) | X | CI에서 명세 비교 미구현 | CONFIG |
| OpenApiInteractionValidator | X | 런타임 명세 검증 미구현 | CODE |
| API 버전 관리 | X | 버전 관리 전략 미적용 | CODE |
| DTO 분리 (API 모델 vs 도메인 모델) | X | 도메인 모델을 API 응답으로 직접 노출 | CODE |
| RFC 7807 Problem Details | X | 에러 응답이 `{"error":"message"}` 비표준 형식 | CODE |
| API 데이터 교환 모델링 (Mapper) | X | 변환 계층 없음 | CODE |

#### Chapter 2: API 테스트

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| 테스트 피라미드 | O 부분 | 5계층 중 단위/계약만 존재 | CODE |
| 단위 테스트 | O | SessionControllerTest 6건 존재 | - |
| 컴포넌트 테스트 (REST-Assured) | X | given-when-then 스타일 HTTP 테스트 부재 | CODE |
| 계약 테스트 (Pact CDC) | O | Consumer 3건 + Provider 3건 구현 완료 | - |
| 통합 테스트 (Testcontainers) | X | 인메모리만 사용, DB 연동 테스트 불가 | CODE |
| E2E 테스트 | X | 전체 시스템 시나리오 테스트 부재 | CODE |
| 성능 테스트 (Gatling/K6) | X | 부하 테스트 미구현 | CODE |
| Pact Broker 연동 | X | `@PactFolder` 사용 중, Broker 미연동 | CODE/CONFIG |
| can-i-deploy | X | 배포 가능 여부 확인 프로세스 부재 | CONFIG |

### Part 2: API 트래픽 관리

#### Chapter 3: API 게이트웨이

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| API Gateway 도입 | X | 서비스 직접 노출, 단일 진입점 없음 | CODE |
| 라우팅 | X | 게이트웨이 라우팅 규칙 미구현 | CONFIG |
| 레이트 리미팅 | X | 요청 제한 없음 | CODE/CONFIG |
| TLS 종료 | X | HTTPS 미적용 | CONFIG |
| 요청/응답 변환 | X | 프로토콜/페이로드 변환 미구현 | CODE |
| 서킷 브레이커 (Gateway 레벨) | X | Gateway 레벨 장애 격리 없음 | CODE |
| 캐싱 | X | 응답 캐시 미적용 | CONFIG |
| Edge Stack 구성 | X | CDN/WAF/LB 계층 문서 부재 | DOC |
| 게이트웨이 배포 패턴 | X | 단일/멀티/DMZ 패턴 문서 부재 | DOC |
| 게이트웨이 안티패턴 | X | Hub-and-Spoke 등 안티패턴 문서 부재 | DOC |
| API 생명주기 관리 | X | APLM 프로세스 미정의 | DOC |

#### Chapter 4: 서비스 메시

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| 서비스 메시 (Istio/Linkerd) | X | 실행 환경 없음 (K8s 필요) | DOC/CONFIG |
| 사이드카 프록시 패턴 | X | 개념 문서 부재 | DOC |
| mTLS | X | 서비스 간 암호화 없음 | DOC/CONFIG |
| 분산 트레이싱 | X | Correlation ID/Jaeger 미구현 | CODE/DOC |
| 서비스 메트릭 자동 수집 | X | Prometheus/Micrometer 미적용 | CODE/CONFIG |
| 카나리 배포 설정 | X | VirtualService YAML 예시 부재 | DOC/CONFIG |
| 장애 주입 (Chaos Engineering) | X | 장애 주입 설정 부재 | DOC/CONFIG |
| 트래픽 미러링 | X | 미러링 설정 부재 | DOC/CONFIG |
| Strangler Fig 패턴 | X | 점진적 마이그레이션 패턴 문서 부재 | DOC |

### Part 3: API 보안과 진화

#### Chapter 5: 기능 플래그와 릴리스 전략

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| 릴리스 플래그 | X | 점진적 롤아웃 미구현 | CODE |
| 실험 플래그 (A/B 테스트) | X | 변형 비교 미구현 | CODE |
| 운영 플래그 (킬 스위치) | X | 긴급 비활성화 미구현 | CODE |
| 권한 플래그 | X | 사용자 그룹별 기능 제어 미구현 | CODE |
| 카나리 배포 전략 | X | 가중치 기반 트래픽 분할 미구현 | DOC/CONFIG |
| 블루/그린 배포 | X | 환경 전환 전략 문서 부재 | DOC |
| 다크 런칭 / 트래픽 미러링 | X | 사전 검증 전략 미구현 | DOC |

#### Chapter 6: 위협 모델링

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| STRIDE 분석 | X | 위협 분류 문서 부재 | DOC |
| DFD (데이터 흐름 다이어그램) | X | 신뢰 경계/데이터 흐름 시각화 부재 | DOC |
| OWASP API Security Top 10 | X | 취약점 체크리스트 부재 | DOC |
| BOLA 방지 (객체 수준 인가) | X | 리소스 소유자 검증 없음 | CODE |
| 입력 검증 | X | Bean Validation 미적용 | CODE |

#### Chapter 7: 인증과 인가

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| OAuth 2.0 Grant Types | X | 인증 플로우 미구현 | CODE/DOC |
| JWT 발급/검증 | X | 토큰 기반 인증 없음 | CODE |
| OIDC | X | ID Token/UserInfo 미구현 | DOC |
| API Gateway 인증 | X | 중앙집중 인증 미구현 | CODE |
| 서비스 간 인증 (Client Credentials) | X | M2M 토큰 교환 미구현 | CODE/DOC |
| Zero Trust | X | 매 요청 검증 원칙 미적용 | DOC |
| RBAC/ABAC | X | 역할 기반 접근 제어 없음 | CODE |

#### Chapter 8: DDD와 API 진화

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| Bounded Context | X | common 공유 모듈이 BC 원칙 위배 | DOC/CODE |
| Ubiquitous Language | X | 도메인 용어 정의 부재 | DOC |
| Context Map | X | 컨텍스트 간 관계 시각화 부재 | DOC |
| Context Mapping 패턴 (ACL, OHS 등) | X | 패턴 적용 부재 | DOC/CODE |
| Aggregate/Entity/Value Object | X | 전술적 설계 미적용 | CODE |
| Domain Event | X | 이벤트 기반 통신 부재 | CODE |
| Event Storming | X | 워크숍 결과 문서 부재 | DOC |
| 서비스 분리 기준 (Seam 식별) | X | 분리 근거 문서 부재 | DOC |

#### Chapter 9: 클라우드 이전과 API 리팩토링

| 주제 | 구현 여부 | 갭 상세 | 유형 |
|------|----------|--------|------|
| 6R 마이그레이션 전략 | X | 전략 분석 문서 부재 | DOC |
| Strangler Fig 패턴 적용 | X | 점진적 전환 시나리오 부재 | DOC |
| Branch by Abstraction | X | 추상화 기반 전환 미구현 | DOC/CODE |
| Kubernetes 배포 | X | K8s 매니페스트 부재 | CONFIG |
| Dockerfile | X | 서비스 컨테이너 이미지 부재 | CONFIG |

---

## 3. 누락된 구현 목록 (우선순위별)

### CRITICAL (필수 — 책의 핵심 시나리오 시연에 필수)

| # | 항목 | 챕터 | 근거 | 유형 |
|---|------|------|------|------|
| 1 | **CFP/Vote 서비스** | Ch0,8 | 책 전체에 걸친 세 번째 핵심 도메인. 다자간 CDC 테스트, DDD 바운디드 컨텍스트 시연에 필수 | CODE |
| 2 | **Pact Broker 실제 연동** | Ch2 | Docker Compose에 Broker가 있지만 미연동. CDC의 핵심 가치(독립 배포/can-i-deploy) 시연 불가 | CODE/CONFIG |
| 3 | **테스트 피라미드 보완** (컴포넌트/통합) | Ch2 | 5계층 중 2계층만 존재. 테스트 전략의 핵심 주제를 시연 불가 | CODE |

### HIGH (권장 — 해당 챕터의 주요 주제 시연에 필요)

| # | 항목 | 챕터 | 근거 | 유형 |
|---|------|------|------|------|
| 4 | **API Gateway** | Ch3 | 책 Part 2의 핵심 주제. 남북 트래픽 관리, 중앙 인증/레이트 리미팅 시연 | CODE/CONFIG |
| 5 | **RFC 7807 에러 응답** | Ch1 | REST API 설계 표준. 현재 비표준 에러 형식 사용 중 | CODE |
| 6 | **DTO 분리 + Mapper** | Ch1,8 | API 모델과 도메인 모델 결합. DDD 원칙 위배 | CODE |
| 7 | **입력 검증 (Bean Validation)** | Ch1,6 | API 보안의 첫 번째 방어선. OWASP 취약점 방지 기본 | CODE |
| 8 | **JWT 인증/인가** | Ch7 | Part 3 보안 핵심 주제. 토큰 기반 인증 시연 필수 | CODE |
| 9 | **OpenAPI 명세 강화** | Ch1 | 어노테이션 기반 문서화, 에러 응답 스키마 정의 | CODE |
| 10 | **ADR 문서** | Ch0 | 아키텍처 의사결정의 체계적 기록. 실제 프로젝트 적용 시연 | DOC |

### MEDIUM (선택 — 교육 가치는 있으나 코드 구현보다 문서/설정)

| # | 항목 | 챕터 | 근거 | 유형 |
|---|------|------|------|------|
| 11 | **Feature Flag 데모** | Ch5 | 릴리스 전략 시연. property 기반 간이 구현으로 충분 | CODE |
| 12 | **DDD 문서화** | Ch8 | Bounded Context Map, Context Mapping 패턴 시각화 | DOC |
| 13 | **C4 다이어그램** | Ch0 | 4레벨 아키텍처 다이어그램 작성 | DOC |
| 14 | **위협 모델 문서** | Ch6 | STRIDE 분석, DFD, OWASP 체크리스트 | DOC |
| 15 | **Observability (Actuator + Micrometer)** | Ch4 | 건강 검사/메트릭 엔드포인트 추가 | CODE/CONFIG |

### LOW (보너스 — 시간 여유 시 구현)

| # | 항목 | 챕터 | 근거 | 유형 |
|---|------|------|------|------|
| 16 | **서비스 메시 YAML 예시** | Ch4 | Istio VirtualService/DestinationRule 설정 문서 | DOC/CONFIG |
| 17 | **API 버전 관리** | Ch1 | URI 기반 v1/v2 데모 | CODE |
| 18 | **성능 테스트 (Gatling/K6)** | Ch2 | 부하 테스트 시뮬레이션 | CODE |
| 19 | **클라우드 마이그레이션 문서** | Ch9 | 6R 전략, Strangler Fig 패턴 문서 | DOC |
| 20 | **Kubernetes 매니페스트 + Dockerfile** | Ch9 | 컨테이너 배포 예시 | CONFIG |
| 21 | **GraphQL 엔드포인트** | Ch1 | REST 대비 GraphQL 비교 시연 | CODE |

---

## 4. 현재 구현의 기술적 이슈

분석 과정에서 발견된 기존 코드의 문제점:

| # | 이슈 | 파일 | 상세 |
|---|------|------|------|
| 1 | attendee-service에 불필요한 Provider 의존성 | `attendee-service/build.gradle.kts` | `pact.provider:junit5`, `pact.provider:spring`이 포함되어 있으나 Consumer 서비스에 불필요 |
| 2 | AttendeeController POST에 Location 헤더 누락 | `AttendeeController.kt` | Richardson Level 2 미달 (SessionController는 포함) |
| 3 | ApiResponse 래퍼 불일치 | SessionController | `getSessions()`는 ApiResponse 반환, `getSession()`은 Session 직접 반환 — 클라이언트 파싱 로직 이중화 |
| 4 | CrossOrigin wildcard | 양쪽 Controller | `@CrossOrigin(origins = ["*"])`는 보안 안티패턴 |
| 5 | SessionClient 장애 무음 처리 | `SessionClient.kt` | 모든 예외를 catch하여 빈 리스트 반환 — Observability 데모 부적합 |
| 6 | AttendeeStore에 clear() 미구현 | `AttendeeStore.kt` | Provider State 테스트 시 데이터 초기화 불가 (SessionStore는 clear() 존재) |
| 7 | Pact nullable 필드 미커버 | Consumer 테스트 | Session.description, dateTime이 nullable이나 null 시나리오 Pact 미정의 |

---

## 5. 구현 추천 로드맵

```
Phase 1 (CRITICAL)           Phase 2 (HIGH)              Phase 3 (MEDIUM)           Phase 4 (LOW)
━━━━━━━━━━━━━━━━━          ━━━━━━━━━━━━━━━━━          ━━━━━━━━━━━━━━━━━          ━━━━━━━━━━━━━━━━

 CFP/Vote Service  ──────▶  API Gateway    ──────▶  Feature Flag Demo  ──────▶  Service Mesh 문서
 Pact Broker 연동           DTO 분리 + Mapper        DDD 문서화                 API Versioning
 Component Tests            RFC 7807 에러            C4 다이어그램              성능 테스트
 Integration Tests          Input Validation         위협 모델 문서             Cloud Migration 문서
 기존 이슈 수정             JWT 인증/인가            Observability              K8s Manifests
                            OpenAPI 강화                                        GraphQL
                            ADR 문서
```

### 각 Phase의 핵심 산출물

**Phase 1**: `cfp-service/` 모듈, Pact Broker gradle 연동, REST-Assured 컴포넌트 테스트, Testcontainers 통합 테스트
**Phase 2**: `gateway/` 모듈, Request/Response DTO + Mapper, ProblemDetail 에러, `@Valid` 검증, JWT 필터, `docs/adr/`
**Phase 3**: Feature Flag property 구현, `docs/ddd/` 문서, `docs/c4/` 다이어그램, `docs/threat-model/`, Spring Boot Actuator
**Phase 4**: `docs/service-mesh/` YAML 예시, API v1/v2, Gatling 시뮬레이션, `docs/cloud-migration/`, `Dockerfile`, `k8s/`

---

## 6. 의사결정 필요 항목

| # | 질문 | 선택지 | 영향 |
|---|------|--------|------|
| 1 | CFP 서비스를 독립 모듈로? | A) 독립 모듈 B) session-service 내 | Pact 관계 구조, 빌드 복잡도 |
| 2 | API Gateway 기술 선택 | A) Spring Cloud Gateway B) nginx C) 문서만 | Docker Compose 복잡도, 코드량 |
| 3 | common 모듈 공유 모델 유지? | A) 유지 (Shared Kernel) B) 서비스별 분리 | DDD 원칙 준수 vs 코드 중복 |
| 4 | JWT 인증에 실제 IdP 사용? | A) Keycloak B) 자체 Mock JWT C) 문서만 | Docker 리소스, 데모 현실성 |
| 5 | DB 전환 범위 | A) 전 서비스 JPA B) 1개 서비스만 C) 인메모리 유지 | Testcontainers 데모 가능 여부 |
| 6 | Pact Broker can-i-deploy 구현? | A) Gradle task B) 문서만 | CI/CD 파이프라인 데모 범위 |

---

## 7. 챕터 커버리지 요약

```
Chapter 0 (시스템 사례)     ████░░░░░░  40%  — 2/3 서비스만, ADR/C4 부재
Chapter 1 (API 설계)        ██░░░░░░░░  20%  — REST 기본만, 명세/DTO/버전 부재
Chapter 2 (API 테스트)      ████░░░░░░  40%  — Pact CDC 구현, 나머지 계층 부재
Chapter 3 (API Gateway)     ░░░░░░░░░░   0%  — 전혀 미구현
Chapter 4 (Service Mesh)    ░░░░░░░░░░   0%  — 전혀 미구현 (인프라 의존)
Chapter 5 (Feature Flag)    ░░░░░░░░░░   0%  — 전혀 미구현
Chapter 6 (Threat Model)    ░░░░░░░░░░   0%  — 전혀 미구현
Chapter 7 (인증/인가)       ░░░░░░░░░░   0%  — 전혀 미구현
Chapter 8 (DDD)             █░░░░░░░░░  10%  — common 공유 모듈만 (Shared Kernel 유사)
Chapter 9 (Cloud Migration) ░░░░░░░░░░   0%  — 전혀 미구현
```

**전체 커버리지: ~11%** (10개 챕터 중 Ch0, Ch1, Ch2, Ch8 일부만 구현)

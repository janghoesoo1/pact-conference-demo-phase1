# Context Mapping Patterns

## 적용된 패턴 분석

### 1. Shared Kernel — common 모듈

**적용 위치**: `common/` 모듈

**공유 요소**:
- 도메인 모델: `Session`, `Attendee`, `ApiResponse<T>`
- 예외: `ResourceNotFoundException`, `UnauthorizedException`, `ForbiddenException`
- 보안: `JwtUtil`, `UserContext`, `RoleCheckInterceptor`
- 에러 처리: `GlobalExceptionHandler` (RFC 7807 ProblemDetail)

**트레이드오프**:
| 장점 | 단점 |
|------|------|
| 컴파일 타임 타입 안전성 | 서비스 간 결합도 증가 |
| 코드 중복 제거 | common 변경 시 전체 재빌드 |
| 일관된 에러 응답 형식 | 독립 배포 제약 |

**대안**: 각 서비스가 자체 모델을 정의하고 ACL로 변환 → 프로덕션에서는 이 방식이 더 적합하지만 데모 프로젝트에서는 Shared Kernel이 학습 효율적

### 2. Customer-Supplier — 서비스 간 HTTP 통신

**적용 위치**:
- `CfpService` (Customer) → `SessionService` (Supplier)
- `CfpService` (Customer) → `AttendeeService` (Supplier)
- `AttendeeService` (Customer) → `SessionService` (Supplier)

**구현 방식**:
- Consumer(Customer)가 Pact 계약을 정의
- Provider(Supplier)가 계약을 검증
- Consumer-Driven Contract로 Supplier의 API 안정성 보장

**CDC 계약 관계**:
```
CfpService ──Pact Consumer──▶ SessionService (Provider)
CfpService ──Pact Consumer──▶ AttendeeService (Provider)
AttendeeService ──Pact Consumer──▶ SessionService (Provider)
```

### 3. Open Host Service (OHS) — REST API

**적용 위치**: 각 서비스의 REST Controller

**특징**:
- 표준 HTTP 메서드 (GET/POST/PUT/DELETE)
- JSON 직렬화
- RFC 7807 ProblemDetail 에러 응답
- OpenAPI 명세 (springdoc)

### 4. Anti-Corruption Layer (ACL) — RestClient 래퍼

**적용 위치**:
- `cfp-service/client/SessionClient.kt`
- `cfp-service/client/AttendeeClient.kt`
- `attendee-service/client/SessionClient.kt`

**역할**: 외부 서비스의 API 응답을 내부 도메인 모델로 변환. 외부 API 변경이 내부 도메인에 전파되지 않도록 격리.

```
CFP Domain ←── ACL(SessionClient) ←── HTTP ←── SessionService API
```

## 패턴 미적용 (향후 고려)

| 패턴 | 설명 | 적용 시점 |
|------|------|----------|
| **Published Language** | 표준 이벤트 스키마 (Avro/Protobuf) | 이벤트 기반 전환 시 |
| **Conformist** | Upstream API를 그대로 수용 | 외부 서비스 연동 시 |
| **Separate Ways** | 완전 독립 (통신 없음) | 도메인 분리가 명확할 때 |

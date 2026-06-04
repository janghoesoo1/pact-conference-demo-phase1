# OWASP API Security Top 10 체크리스트

> 참고: [OWASP API Security Top 10 2023](https://owasp.org/API-Security/)

## 체크리스트

### API1:2023 Broken Object Level Authorization (BOLA)

| 항목 | 상태 | 설명 |
|------|------|------|
| 리소스 접근 시 소유권 검증 | 부분 | X-User-Id 헤더는 전달되나 소유권 검증은 미구현 |
| ID 기반 접근 시 권한 확인 | 부분 | @RoleRequired로 역할 검증, 객체 수준 검증은 미구현 |
| 예측 가능한 ID 사용 방지 | 미구현 | 순차 Int ID 사용 (데모) |

**권장**: 프로덕션에서는 UUID 사용, 리소스 접근 시 소유자 검증 로직 추가

### API2:2023 Broken Authentication

| 항목 | 상태 | 설명 |
|------|------|------|
| JWT 토큰 만료 검증 | 구현 | 1시간 만료, JwtUtil에서 검증 |
| 토큰 갱신 메커니즘 | 미구현 | Refresh Token 없음 (데모) |
| 비밀번호 정책 | N/A | 인증은 토큰 발급으로 대체 |
| 브루트포스 방지 | 부분 | Rate Limiting으로 간접 방어 |

### API3:2023 Broken Object Property Level Authorization

| 항목 | 상태 | 설명 |
|------|------|------|
| 응답 필드 필터링 | 구현 | DTO 분리로 노출 필드 제어 |
| 입력 필드 제한 | 구현 | Request DTO로 허용 필드만 바인딩 |
| 민감 필드 마스킹 | 부분 | email은 그대로 노출 (데모) |

### API4:2023 Unrestricted Resource Consumption

| 항목 | 상태 | 설명 |
|------|------|------|
| Rate Limiting | 구현 | Gateway Token Bucket (100 req/IP) |
| 요청 크기 제한 | 부분 | @Size로 필드 크기 제한, 전체 Body 제한은 Spring 기본값 |
| 페이지네이션 | 미구현 | 목록 API에 페이지네이션 없음 |
| 타임아웃 설정 | 구현 | RestClient 5s/10s 타임아웃 |

### API5:2023 Broken Function Level Authorization

| 항목 | 상태 | 설명 |
|------|------|------|
| 역할 기반 접근 제어 | 구현 | @RoleRequired + RoleCheckInterceptor |
| 관리 기능 분리 | 구현 | GET 공개, POST/PUT/DELETE 인증 필요 |
| HTTP 메서드별 권한 | 구현 | Gateway JwtAuthFilter에서 GET 면제 |

### API6:2023 Unrestricted Access to Sensitive Business Flows

| 항목 | 상태 | 설명 |
|------|------|------|
| 중복 투표 방지 | 미구현 | 같은 사용자가 중복 투표 가능 (데모) |
| 비즈니스 로직 남용 방지 | 부분 | Rate Limiting으로 간접 방어 |

### API7:2023 Server Side Request Forgery (SSRF)

| 항목 | 상태 | 설명 |
|------|------|------|
| 내부 서비스 URL 하드코딩 | 구현 | application.yml에 고정 URL |
| 사용자 입력 기반 URL 없음 | 구현 | URL은 설정에서만 주입 |

### API8:2023 Security Misconfiguration

| 항목 | 상태 | 설명 |
|------|------|------|
| CORS 설정 | 부분 | `*` 오리진 허용 (데모), 프로덕션에서는 제한 필요 |
| 불필요한 HTTP 메서드 | 구현 | 명시적 매핑만 허용 |
| 디버그 정보 비노출 | 구현 | ProblemDetail로 표준화 |
| Actuator 보안 | 미구현 | /actuator 엔드포인트 인증 없이 노출 |

### API9:2023 Improper Inventory Management

| 항목 | 상태 | 설명 |
|------|------|------|
| API 버전 관리 | 미구현 | 단일 버전 (향후 Phase 4) |
| API 명세 문서 | 구현 | SpringDoc OpenAPI 자동 생성 |
| 사용하지 않는 엔드포인트 | 구현 | 불필요한 엔드포인트 없음 |

### API10:2023 Unsafe Consumption of APIs

| 항목 | 상태 | 설명 |
|------|------|------|
| 외부 API 응답 검증 | 부분 | RestClient에서 예외 처리, 세밀한 검증은 미구현 |
| 타임아웃 설정 | 구현 | Connect 5s, Read 10s |
| 외부 API TLS 검증 | N/A | 내부 HTTP 통신만 사용 (데모) |

## 요약

| 카테고리 | 구현 | 부분 | 미구현 |
|----------|------|------|--------|
| 총 10개 항목 | 4 | 4 | 2 |

**구현 완료**: API5 (Function Auth), API7 (SSRF), API9 (Inventory), API3 (Property Auth)
**부분 구현**: API1 (BOLA), API2 (Auth), API4 (Resource), API8 (Misconfig)
**미구현**: API6 (Business Flows), API10 (Unsafe Consumption) — 데모 범위 초과

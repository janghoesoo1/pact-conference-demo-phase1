# ADR-005: 데모용 JWT 자체 발급

## 상태
승인 (Accepted)

## 컨텍스트
API 인증/인가 데모를 위해 토큰 기반 인증이 필요하다.
- 실제 IdP(Keycloak, Auth0 등) 설정은 데모 범위 초과
- 인증 흐름과 RBAC 개념 시연이 목적

## 결정
자체 JWT 유틸리티(JwtUtil)로 토큰을 발급하고 Gateway에서 검증한다.
- HMAC-SHA256 서명 (대칭키)
- 역할: ATTENDEE(읽기), ORGANIZER(읽기/쓰기)
- Gateway → X-User-Id, X-User-Roles 헤더로 하위 서비스에 전달
- GET 요청은 인증 없이 허용 (데모 편의)

## 결과
### 긍정적
- 외부 IdP 없이 인증/인가 시연 가능
- RBAC 개념 학습에 충분한 구현
- Gateway 패턴에서의 인증 위치 시연

### 부정적
- 프로덕션에서는 사용 불가 (하드코딩된 시크릿, 자체 발급)
- OAuth 2.0/OIDC 플로우 미구현
- 토큰 갱신 메커니즘 없음

## 참고
- Mastering API Architecture, Ch7: API 인증/인가
- RFC 7519: JSON Web Token

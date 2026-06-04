# ADR-002: Consumer-Driven Contract Testing에 Pact 선택

## 상태
승인 (Accepted)

## 컨텍스트
마이크로서비스 간 API 호환성을 보장할 메커니즘이 필요하다.
- 선택지: Pact, Spring Cloud Contract, 수동 E2E 테스트
- 3방향 서비스 의존 관계 존재 (CFP→Session, CFP→Attendee, Attendee→Session)

## 결정
Pact JVM 4.6.14를 사용한 Consumer-Driven Contract(CDC) 테스트를 채택한다.
- Consumer가 계약을 정의하고 Provider가 검증하는 방식
- Pact Broker를 통한 계약 중앙 관리
- can-i-deploy로 배포 안전성 확인

## 결과
### 긍정적
- Consumer 관점의 API 호환성 보장
- 독립적 배포 시 안전성 확인 (can-i-deploy)
- 다양한 언어/프레임워크 지원

### 부정적
- Pact Broker 인프라 운영 필요
- Provider State 설정의 복잡성
- 학습 곡선 존재

## 참고
- Mastering API Architecture, Ch2: 테스트 주도 API 개발
- Pact 공식 문서: https://docs.pact.io

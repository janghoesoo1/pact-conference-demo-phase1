# ADR-004: API Gateway로 Spring Cloud Gateway MVC 선택

## 상태
승인 (Accepted)

## 컨텍스트
3개 마이크로서비스의 단일 진입점(Single Entry Point)이 필요하다.
- 선택지: Spring Cloud Gateway (Reactive), Spring Cloud Gateway MVC, Kong, Envoy
- 기존 서비스는 Spring MVC(Servlet) 기반

## 결정
Spring Cloud Gateway MVC를 사용한다.
- Servlet 기반으로 기존 서비스와 동일한 프로그래밍 모델
- 포트 8080에서 3개 서비스로 라우팅
- CORS, JWT, 레이트 리미팅, 로깅을 중앙에서 처리

## 결과
### 긍정적
- 기존 MVC 스킬셋 활용 가능
- Spring 생태계 통합 (Actuator, Security 등)
- 서블릿 필터 체인으로 일관된 요청 처리

### 부정적
- Reactive Gateway 대비 처리량 제한
- Spring Cloud 버전 종속성 관리 필요

## 참고
- Mastering API Architecture, Ch4: API Gateway 패턴
- Spring Cloud Gateway MVC 공식 문서

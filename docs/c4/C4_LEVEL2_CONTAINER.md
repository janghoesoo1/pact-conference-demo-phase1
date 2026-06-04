# C4 Level 2: Container Diagram

## 컨테이너 구성

```mermaid
C4Container
    title Conference Management System - Container Diagram

    Person(user, "사용자", "참석자/주최자/발표자")

    Container_Boundary(system, "Conference Management System") {
        Container(gateway, "API Gateway", "Spring Cloud Gateway MVC", "Port 8080. 라우팅, JWT 인증, CORS, 레이트 리미팅")
        Container(attendee, "Attendee Service", "Spring Boot + Kotlin", "Port 8081. 참석자 등록/관리")
        Container(session, "Session Service", "Spring Boot + Kotlin", "Port 8082. 세션 일정/관리")
        Container(cfp, "CFP Service", "Spring Boot + Kotlin", "Port 8083. 발표 제안/투표 관리")
        Container(common, "Common Library", "Kotlin JAR", "공유 모델, 예외, 보안 유틸리티")
    }

    System_Ext(pactBroker, "Pact Broker", "Contract 중앙 관리")
    ContainerDb(postgres, "PostgreSQL", "세션 데이터 영구 저장 (jpa 프로필)")

    Rel(user, gateway, "API 요청", "HTTPS")
    Rel(gateway, attendee, "라우팅", "HTTP /api/attendees/**")
    Rel(gateway, session, "라우팅", "HTTP /api/sessions/**")
    Rel(gateway, cfp, "라우팅", "HTTP /api/proposals/**")

    Rel(attendee, session, "세션 조회", "HTTP")
    Rel(cfp, session, "세션 조회", "HTTP")
    Rel(cfp, attendee, "참석자 검증", "HTTP")

    Rel(attendee, common, "uses")
    Rel(session, common, "uses")
    Rel(cfp, common, "uses")
    Rel(gateway, common, "uses")

    Rel(session, postgres, "읽기/쓰기", "JDBC (jpa 프로필)")
    Rel(attendee, pactBroker, "계약 게시/검증")
    Rel(cfp, pactBroker, "계약 게시")
```

## 컨테이너 상세

| Container | 기술 스택 | Port | 역할 |
|-----------|----------|------|------|
| **API Gateway** | Spring Cloud Gateway MVC | 8080 | 단일 진입점, 인증, 라우팅 |
| **Attendee Service** | Spring Boot 3.3.6 + Kotlin | 8081 | 참석자 CRUD |
| **Session Service** | Spring Boot 3.3.6 + Kotlin | 8082 | 세션 CRUD, JPA/인메모리 |
| **CFP Service** | Spring Boot 3.3.6 + Kotlin | 8083 | 제안/투표 관리 |
| **Common Library** | Kotlin JAR | - | Shared Kernel |

## 통신 프로토콜

모든 서비스 간 통신: **동기 HTTP/REST + JSON**

| From | To | 방식 | 용도 |
|------|----|------|------|
| Gateway → Services | HTTP Proxy | 요청 라우팅 |
| CFP → Attendee | RestClient | 발표자/투표자 검증 |
| CFP → Session | RestClient | 세션 정보 조회 |
| Attendee → Session | RestClient | 세션 목록 조회 |

# Bounded Context Map

## 컨퍼런스 관리 시스템

### Bounded Contexts

```mermaid
graph LR
    subgraph "Attendee Context"
        A[Attendee]
        A_REG[Registration]
    end

    subgraph "Session Context"
        S[Session]
        S_SCHED[Schedule]
    end

    subgraph "CFP Context"
        P[Proposal]
        V[Vote]
        P_REVIEW[Review Process]
    end

    subgraph "Gateway Context"
        GW[API Gateway]
        AUTH[Authentication]
        RL[Rate Limiting]
    end

    GW -->|routes to| A
    GW -->|routes to| S
    GW -->|routes to| P

    P -->|Customer-Supplier| S
    P -->|Customer-Supplier| A
    A -->|Customer-Supplier| S
```

### Context 설명

| Context | 책임 | 핵심 엔티티 | 소유 팀 |
|---------|------|------------|---------|
| **Attendee** | 참석자 등록, 프로필 관리 | Attendee | 참석자 팀 |
| **Session** | 세션 일정, 발표 관리 | Session | 프로그램 팀 |
| **CFP** | 발표 제안 접수, 투표, 리뷰 | Proposal, Vote | CFP 팀 |
| **Gateway** | API 진입점, 인증, 라우팅 | - | 플랫폼 팀 |

### Context 간 관계

| Upstream | Downstream | 패턴 | 설명 |
|----------|-----------|------|------|
| Session | Attendee | Customer-Supplier | Attendee가 Session 목록 조회 |
| Session | CFP | Customer-Supplier | CFP가 승인된 Proposal의 Session 연결 |
| Attendee | CFP | Customer-Supplier | CFP가 발표자/투표자 검증 |

### 데이터 흐름

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant CFP
    participant Attendee
    participant Session

    Client->>Gateway: POST /api/proposals (JWT)
    Gateway->>CFP: Forward (X-User-Id, X-User-Roles)
    CFP->>Attendee: GET /attendees/{speakerId}
    Attendee-->>CFP: Attendee exists
    CFP-->>Gateway: 201 Created
    Gateway-->>Client: 201 + Location

    Client->>Gateway: POST /api/proposals/{id}/votes
    Gateway->>CFP: Forward
    CFP->>Attendee: GET /attendees/{voterId}
    Attendee-->>CFP: Attendee exists
    CFP-->>Gateway: 200 Vote recorded
```

# C4 Level 1: System Context Diagram

## 시스템 컨텍스트

```mermaid
C4Context
    title Conference Management System - System Context

    Person(attendee, "참석자", "컨퍼런스에 참가하는 사람")
    Person(organizer, "주최자", "컨퍼런스를 기획하고 운영하는 사람")
    Person(speaker, "발표자", "세션에서 발표하는 사람")

    System(conference, "Conference Management System", "컨퍼런스 참석자, 세션, 발표 제안을 관리하는 시스템")

    System_Ext(pactBroker, "Pact Broker", "Consumer-Driven Contract 계약을 중앙 관리하는 외부 서비스")

    Rel(attendee, conference, "참석 등록, 세션 조회, 투표")
    Rel(organizer, conference, "세션 관리, 제안 심사, 시스템 운영")
    Rel(speaker, conference, "발표 제안 접수")
    Rel(conference, pactBroker, "계약 게시/검증", "HTTP")
```

## 핵심 사용자

| 사용자 | 역할 | 주요 활동 |
|--------|------|----------|
| **참석자** (Attendee) | ATTENDEE | 등록, 세션 조회, 발표 제안 투표 |
| **주최자** (Organizer) | ORGANIZER | 세션 CRUD, 제안 승인/거부, 참석자 관리 |
| **발표자** (Speaker) | ATTENDEE | 발표 제안 접수 (참석자이기도 함) |

## 외부 시스템

| 시스템 | 역할 | 연동 방식 |
|--------|------|----------|
| **Pact Broker** | CDC 계약 중앙 관리 | HTTP (Docker Compose) |
| **PostgreSQL** | 데이터 영구 저장 (jpa 프로필) | JDBC |

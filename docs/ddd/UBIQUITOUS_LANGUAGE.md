# Ubiquitous Language (유비쿼터스 언어)

## 도메인 용어 사전

### Attendee Context

| 한국어 | 영어 | 정의 | 코드 매핑 |
|--------|------|------|-----------|
| 참석자 | Attendee | 컨퍼런스에 등록한 사람 | `Attendee` data class |
| 등록 | Registration | 참석자가 시스템에 가입하는 행위 | `POST /attendees` |
| 이름 | Given Name | 참석자의 이름 (성 제외) | `Attendee.givenName` |
| 성 | Surname | 참석자의 성(姓) | `Attendee.surname` |

### Session Context

| 한국어 | 영어 | 정의 | 코드 매핑 |
|--------|------|------|-----------|
| 세션 | Session | 컨퍼런스의 개별 발표 시간 | `Session` data class |
| 발표자 | Speaker | 세션을 진행하는 사람 | `Session.speaker` |
| 일정 | Schedule | 세션의 날짜와 시간 | `Session.dateTime` |
| 세션 목록 | Session List | 등록된 모든 세션의 집합 | `GET /sessions` |

### CFP Context

| 한국어 | 영어 | 정의 | 코드 매핑 |
|--------|------|------|-----------|
| 발표 제안 | Proposal | 발표자가 제출한 세션 후보 | `Proposal` data class |
| 초록 | Abstract | 제안의 내용 요약 | `Proposal.abstract_` |
| 투표 | Vote | 참석자가 제안에 매긴 점수 | `Vote` data class |
| 점수 | Score | 투표의 평가 점수 (1-5) | `Vote.score` |
| 평균 점수 | Average Score | 제안의 투표 평균 | `VoteStore.getAverageScore()` |
| 제안 상태 | Proposal Status | 제안의 현재 진행 단계 | `ProposalStatus` enum |
| 접수됨 | Submitted | 제안이 최초 접수된 상태 | `ProposalStatus.SUBMITTED` |
| 심사중 | Under Review | 심사위원이 검토중인 상태 | `ProposalStatus.UNDER_REVIEW` |
| 승인됨 | Approved | 발표가 확정된 상태 | `ProposalStatus.APPROVED` |
| 거부됨 | Rejected | 발표가 거부된 상태 | `ProposalStatus.REJECTED` |

### Gateway / Infrastructure

| 한국어 | 영어 | 정의 | 코드 매핑 |
|--------|------|------|-----------|
| API 게이트웨이 | API Gateway | 모든 API 요청의 단일 진입점 | `gateway` module |
| 토큰 | JWT Token | 인증 정보를 담은 JSON Web Token | `JwtUtil` |
| 역할 | Role | 사용자의 권한 수준 | `ATTENDEE`, `ORGANIZER` |
| 레이트 리미팅 | Rate Limiting | API 호출 빈도 제한 | `RateLimitFilter` |
| 계약 테스트 | Contract Test | Consumer-Provider 간 API 호환성 검증 | Pact CDC |

### 비즈니스 규칙

| 규칙 | 설명 | 구현 위치 |
|------|------|----------|
| 발표자 검증 | 제안 접수 시 speakerId가 유효한 참석자여야 함 | `CfpController.createProposal()` |
| 투표자 검증 | 투표 시 attendeeId가 유효한 참석자여야 함 | `CfpController.castVote()` |
| 점수 범위 | 투표 점수는 1~5 사이 정수 | `CreateVoteRequest.score` (@Min/@Max) |
| 역할 기반 접근 | ORGANIZER만 쓰기 작업 가능 | `@RoleRequired`, `JwtAuthFilter` |

# Pact Conference Demo 대시보드 - 구현 계획서

## 1. 기술 결정

| 항목 | 결정 | 근거 |
|------|------|------|
| 프레임워크 | Vanilla JS + ES Modules | 빌드 불필요, 즉시 실행, 의존성 제로 |
| 스타일링 | 순수 CSS + CSS Variables | 외부 라이브러리 없이 테마 지원 가능 |
| 라우팅 | Hash Router (#/overview, #/contracts, ...) | 서버 설정 불필요, 정적 파일 호환 |
| API 호출 | Fetch API | 브라우저 내장, 별도 라이브러리 불필요 |
| JSON 표시 | 자체 구현 (재귀 DOM 생성) | 외부 라이브러리 없이 구문 하이라이팅 |
| 위치 | dashboard/ (프로젝트 루트 하위) | Gradle 모듈 아님, 독립 디렉토리 |

---

## 2. 화면 구성 (6페이지)

### 2.1 Overview (메인 페이지)

```
┌─────────────────────────────────────────────────┐
│  Pact Conference Demo Dashboard                  │
│  [Overview] [Contracts] [API] [Tests] [Status]  │
├─────────────────────────────────────────────────┤
│                                                  │
│  ┌─── Quick Stats ────────────────────────────┐ │
│  │  Services: 2  │  Contracts: 3  │  APIs: 11 │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
│  ┌─── Architecture Diagram ───────────────────┐ │
│  │                                             │ │
│  │  [AttendeeService]  ──HTTP──> [SessionSvc] │ │
│  │      :8080                      :8081      │ │
│  │         │                         │        │ │
│  │         └──── Pact ──────────────┘        │ │
│  │                  │                         │ │
│  │            [Pact Broker]                   │ │
│  │               :9292                        │ │
│  │                  │                         │ │
│  │            [PostgreSQL]                    │ │
│  │               :5432                        │ │
│  └────────────────────────────────────────────┘ │
│                                                  │
│  ┌─── Service Status ─────────────────────────┐ │
│  │  AttendeeService (8080)    [UP] / [DOWN]   │ │
│  │  SessionService  (8081)    [UP] / [DOWN]   │ │
│  │  Pact Broker     (9292)    [UP] / [DOWN]   │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

- SVG 기반 아키텍처 다이어그램 (서비스 노드 + 화살표)
- 서비스 상태 카드: 10초 간격 HTTP ping, UP(초록)/DOWN(빨강) 표시
- Quick Stats: 서비스 수, 계약 수, API 엔드포인트 수

### 2.2 Contracts (계약 현황)

```
┌─────────────────────────────────────────────────┐
│  Pact Contracts                                  │
│                                                  │
│  Consumer: AttendeeService                       │
│  Provider: SessionService                        │
│  Pact Spec: V3                                   │
│                                                  │
│  ┌─── Contract 1 ────────────────────────────┐  │
│  │  "세션 1 조회 요청"                         │  │
│  │  State: 세션 ID 1이 존재함                  │  │
│  │  GET /sessions/1 → 200                     │  │
│  │  [요청 보기] [응답 보기] [매칭 규칙 보기]    │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Contract 2 ────────────────────────────┐  │
│  │  "전체 세션 목록 조회 요청"                  │  │
│  │  State: 세션 목록이 존재함                  │  │
│  │  GET /sessions → 200                       │  │
│  │  [요청 보기] [응답 보기] [매칭 규칙 보기]    │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Contract 3 ────────────────────────────┐  │
│  │  "존재하지 않는 세션 조회 요청"              │  │
│  │  State: 세션 ID 999가 존재하지 않음         │  │
│  │  GET /sessions/999 → 404                   │  │
│  │  [요청 보기] [응답 보기]                     │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Raw Pact JSON ─────────────────────────┐  │
│  │  { "consumer": { "name": "AttendeeService" │  │
│  │    ...                                     │  │
│  │  (접기/펼치기 토글)                          │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

- 데이터 소스: `attendee-service/build/pacts/AttendeeService-SessionService.json` 직접 로드
- 파일 없을 시: "Consumer 테스트를 먼저 실행하세요" 안내
- 각 계약의 요청 헤더, 경로, 응답 바디, matchingRules 표시
- Raw JSON 뷰어: 구문 하이라이팅 + 접기/펼치기

### 2.3 API Explorer (API 탐색기)

```
┌─────────────────────────────────────────────────┐
│  API Explorer                                    │
│                                                  │
│  ┌─── Service ─────┐  ┌─── Endpoint ──────────┐ │
│  │ SessionService ▼ │  │ GET /sessions      ▼ │ │
│  └─────────────────┘  └──────────────────────┘ │
│                                                  │
│  ┌─── Request ───────────────────────────────┐  │
│  │  Method: GET                               │  │
│  │  URL: http://localhost:8081/sessions        │  │
│  │  Headers: Accept: application/json         │  │
│  │  Body: (GET이므로 없음)                     │  │
│  │                                            │  │
│  │  [Send Request]                            │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Response ──────────────────────────────┐  │
│  │  Status: 200 OK                            │  │
│  │  Time: 23ms                                │  │
│  │                                            │  │
│  │  {                                         │  │
│  │    "data": [                               │  │
│  │      {                                     │  │
│  │        "id": 1,                            │  │
│  │        "title": "gRPC로 마이크로서비스..."  │  │
│  │      },                                    │  │
│  │      ...                                   │  │
│  │    ],                                      │  │
│  │    "total": 3                              │  │
│  │  }                                         │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

- 서비스 선택 (SessionService / AttendeeService)
- 엔드포인트 드롭다운: 실제 11개 API 목록
- GET: 파라미터 입력 (ID 등)
- POST/PUT: JSON Body 편집기
- 응답: 상태 코드 + 응답 시간(ms) + JSON 하이라이팅
- 서비스 미실행 시: "서비스에 연결할 수 없습니다" 오류 표시

### 2.4 Test Results (테스트 결과)

```
┌─────────────────────────────────────────────────┐
│  Test Results                                    │
│                                                  │
│  ┌─── How to Run ────────────────────────────┐  │
│  │  # Consumer 테스트 (Pact 파일 생성)        │  │
│  │  ./gradlew :attendee-service:test          │  │
│  │                                            │  │
│  │  # Provider 검증                           │  │
│  │  ./gradlew :session-service:test           │  │
│  │                                            │  │
│  │  # 전체 테스트                              │  │
│  │  ./gradlew test                            │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── CDC Test Flow ─────────────────────────┐  │
│  │                                            │  │
│  │  Step 1: Consumer 계약 정의                │  │
│  │    └ @Pact 메서드가 MockServer 계약 정의   │  │
│  │            ↓                               │  │
│  │  Step 2: Consumer 테스트 실행              │  │
│  │    └ SessionClient가 MockServer에 요청     │  │
│  │            ↓                               │  │
│  │  Step 3: Pact JSON 파일 생성              │  │
│  │    └ build/pacts/...json                   │  │
│  │            ↓                               │  │
│  │  Step 4: Provider 상태 설정               │  │
│  │    └ @State 핸들러가 테스트 데이터 준비    │  │
│  │            ↓                               │  │
│  │  Step 5: Provider 검증                    │  │
│  │    └ 실제 서버에 HTTP 요청 → 응답 검증     │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Test Summary ──────────────────────────┐  │
│  │  Consumer Pact Tests:   3/3 PASSED        │  │
│  │  Provider Verification: 3/3 PASSED        │  │
│  │  Controller Unit Tests: 6/6 PASSED        │  │
│  │  Total: 12/12 PASSED                      │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

- Gradle 테스트 명령어 안내 (복사 버튼)
- CDC 테스트 흐름을 5단계 시각화
- 테스트 결과 요약 (build/test-results/ XML 파싱 또는 정적 데이터)
- Pact 파일 존재 여부로 Consumer 테스트 실행 여부 판단

### 2.5 Service Status (서비스 상태)

```
┌─────────────────────────────────────────────────┐
│  Service Status                                  │
│  Auto-refresh: 10s                               │
│                                                  │
│  ┌─── Session Service ───────────────────────┐  │
│  │  URL: http://localhost:8081               │  │
│  │  Status: UP (200 OK)                       │  │
│  │  Response Time: 12ms                       │  │
│  │                                            │  │
│  │  Data:                                     │  │
│  │  Sessions: 3건                             │  │
│  │  [GET /sessions]  [Swagger UI]             │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Attendee Service ──────────────────────┐  │
│  │  URL: http://localhost:8080               │  │
│  │  Status: UP (200 OK)                       │  │
│  │  Response Time: 15ms                       │  │
│  │                                            │  │
│  │  Data:                                     │  │
│  │  Attendees: 3건                            │  │
│  │  [GET /attendees]  [Swagger UI]            │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌─── Pact Broker ───────────────────────────┐  │
│  │  URL: http://localhost:9292               │  │
│  │  Status: DOWN (Connection refused)         │  │
│  │  Hint: docker compose up -d 실행 필요      │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

- 10초 폴링으로 상태 갱신
- UP: 초록 배경 + 응답 시간
- DOWN: 빨강 배경 + 힌트 메시지
- 각 서비스의 데이터 건수 표시 (API 호출)
- Swagger UI 바로가기 링크

### 2.6 Learning Guide (학습 가이드)

```
┌─────────────────────────────────────────────────┐
│  CDC 테스트 학습 가이드                            │
│                                                  │
│  진행률: [=====>     ] 3/7 단계                   │
│                                                  │
│  Step 1: 마이크로서비스와 API 호환성 문제  [완료]  │
│  Step 2: 계약 테스트란 무엇인가?          [완료]  │
│  Step 3: Consumer가 계약을 정의하는 이유  [진행중] │
│  Step 4: Pact MockServer의 동작 원리     [대기]   │
│  Step 5: Provider State와 데이터 준비    [대기]   │
│  Step 6: 매칭 규칙 (Type vs Value)       [대기]   │
│  Step 7: can-i-deploy와 CI/CD 통합       [대기]   │
│                                                  │
│  ┌─── Step 3: Consumer가 계약을 정의하는 이유 ─┐ │
│  │                                             │ │
│  │  왜 Provider가 아닌 Consumer가 계약을       │ │
│  │  정의하는 걸까요?                            │ │
│  │                                             │ │
│  │  Consumer는 자신이 실제로 필요한 필드만      │ │
│  │  계약에 포함합니다. Provider가 추가 필드를   │ │
│  │  보내더라도 계약에 포함되지 않은 필드는      │ │
│  │  무시됩니다.                                 │ │
│  │                                             │ │
│  │  코드 예시:                                  │ │
│  │  @Pact(consumer = "AttendeeService")        │ │
│  │  fun getSessionPact(...) {                  │ │
│  │    return builder                           │ │
│  │      .given("세션 ID 1이 존재함")           │ │
│  │      .uponReceiving("세션 1 조회 요청")     │ │
│  │      ...                                    │ │
│  │  }                                          │ │
│  │                                             │ │
│  │  [이전] [다음: Step 4]                       │ │
│  └────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────┘
```

- 7단계 인터랙티브 학습
- 각 단계: 개념 설명 + 실제 코드 스니펫 + 핵심 포인트
- 진행률 바 (localStorage에 저장)
- 이전/다음 내비게이션

---

## 3. 디렉토리 구조

```
dashboard/
├── index.html                 # 메인 HTML (SPA 셸)
├── css/
│   └── style.css              # 전체 스타일 (CSS Variables 기반)
├── js/
│   ├── app.js                 # 앱 초기화 + Hash 라우터
│   ├── api.js                 # API 클라이언트 (Fetch 래퍼)
│   ├── pages/
│   │   ├── overview.js        # Overview 페이지
│   │   ├── contracts.js       # Contracts 페이지
│   │   ├── api-explorer.js    # API Explorer 페이지
│   │   ├── tests.js           # Test Results 페이지
│   │   ├── status.js          # Service Status 페이지
│   │   └── guide.js           # Learning Guide 페이지
│   └── components/
│       ├── json-viewer.js     # JSON 구문 하이라이팅 + 접기/펼치기
│       ├── status-badge.js    # UP/DOWN 상태 뱃지
│       ├── nav.js             # 내비게이션 바
│       └── code-block.js      # 코드 블록 렌더러
└── data/
    ├── api-endpoints.json     # 11개 API 엔드포인트 정의
    ├── guide-steps.json       # 7단계 학습 가이드 콘텐츠
    └── architecture.json      # 아키텍처 다이어그램 데이터
```

**총 16개 파일 (신규 생성)**

---

## 4. 기존 코드 수정 사항

대시보드가 서비스 API를 브라우저에서 호출하려면 CORS 설정이 필요합니다.

### 4.1 SessionController.kt

```kotlin
// 추가할 어노테이션
@CrossOrigin(origins = ["*"])
@RestController
class SessionController(private val sessionStore: SessionStore) {
```

### 4.2 AttendeeController.kt

```kotlin
// 추가할 어노테이션
@CrossOrigin(origins = ["*"])
@RestController
class AttendeeController(
```

### 4.3 .gitignore

```
# 기존 내용에 추가 없음 (dashboard/는 추적 대상)
```

**수정 파일: 2개** (SessionController.kt, AttendeeController.kt)

---

## 5. 데이터 소스 매핑

| 화면 요소 | 데이터 소스 | 접근 방법 | 서비스 필요? |
|----------|-----------|----------|------------|
| 아키텍처 다이어그램 | data/architecture.json | 정적 로드 | 아니오 |
| 서비스 상태 | HTTP ping | fetch('http://localhost:8081/sessions') | 예 |
| 계약 카드 | Pact JSON 파일 | fetch('../attendee-service/build/pacts/...json') | 아니오 |
| API 탐색기 응답 | 서비스 API | fetch() 직접 호출 | 예 |
| 테스트 결과 | 정적 데이터 | data/ 또는 build/test-results/ | 아니오 |
| 학습 가이드 | data/guide-steps.json | 정적 로드 | 아니오 |
| 데이터 건수 | 서비스 API | fetch('http://localhost:8081/sessions') | 예 |

**핵심**: 서비스가 내려가 있어도 계약 현황, 아키텍처 다이어그램, 학습 가이드는 정상 표시됩니다.

---

## 6. 구현 단계

### Phase 1: 스캐폴딩 (Step 1)

| 파일 | 내용 |
|------|------|
| index.html | HTML 셸 (nav + main#app) |
| css/style.css | CSS Variables, 레이아웃, 카드 스타일 |
| js/app.js | Hash 라우터, 페이지 전환 |
| js/api.js | Fetch 래퍼 (timeout, error handling) |
| js/components/nav.js | 내비게이션 바 렌더링 |
| SessionController.kt | @CrossOrigin 추가 |
| AttendeeController.kt | @CrossOrigin 추가 |

**검증**: index.html을 브라우저에서 열어 내비게이션 동작 확인

### Phase 2: Overview + Service Status (Step 2)

| 파일 | 내용 |
|------|------|
| js/pages/overview.js | SVG 다이어그램, Quick Stats, 서비스 상태 |
| js/pages/status.js | 서비스별 상세 상태, 10초 폴링 |
| js/components/status-badge.js | UP/DOWN 뱃지 컴포넌트 |
| data/architecture.json | 서비스 노드, 연결 정보 |

**검증**: 서비스 실행 상태에서 UP 표시, 중지 후 DOWN 전환 확인

### Phase 3: Contracts (Step 3)

| 파일 | 내용 |
|------|------|
| js/pages/contracts.js | 계약 카드 목록, Raw JSON 뷰 |
| js/components/json-viewer.js | JSON 구문 하이라이팅 |

**검증**: Pact JSON 파일 로드하여 3개 계약 카드 표시

### Phase 4: API Explorer (Step 4)

| 파일 | 내용 |
|------|------|
| js/pages/api-explorer.js | 서비스/엔드포인트 선택, 요청/응답 패널 |
| data/api-endpoints.json | 11개 API 정의 (method, path, body 예시) |

**검증**: GET /sessions 호출하여 JSON 응답 표시

### Phase 5: Test Results (Step 5)

| 파일 | 내용 |
|------|------|
| js/pages/tests.js | CDC 흐름도, 테스트 요약, 명령어 안내 |
| js/components/code-block.js | 코드 블록 렌더러 (명령어 복사) |

**검증**: CDC 흐름 시각화, 테스트 명령어 복사 기능

### Phase 6: Learning Guide (Step 6)

| 파일 | 내용 |
|------|------|
| js/pages/guide.js | 7단계 학습 가이드, 진행률 바 |
| data/guide-steps.json | 각 단계 콘텐츠 (설명, 코드, 핵심 포인트) |

**검증**: 7단계 내비게이션, 진행률 localStorage 저장

---

## 7. 실행 방법

```bash
# 1. 서비스 실행 (별도 터미널)
./gradlew :session-service:bootRun
./gradlew :attendee-service:bootRun

# 2. 대시보드 실행 (방법 A: Python 내장 서버)
cd dashboard
python3 -m http.server 3000
# 브라우저에서 http://localhost:3000 접속

# 2. 대시보드 실행 (방법 B: 직접 열기)
open dashboard/index.html
# 주의: file:// 프로토콜에서는 fetch()가 CORS로 차단될 수 있음
# Python 서버 사용 권장
```

---

## 8. 일정 추정

| 단계 | 파일 수 | 의존성 |
|------|---------|--------|
| Phase 1: 스캐폴딩 + CORS | 7 | 없음 |
| Phase 2: Overview + Status | 4 | Phase 1 |
| Phase 3: Contracts | 2 | Phase 1 |
| Phase 4: API Explorer | 2 | Phase 1 |
| Phase 5: Test Results | 2 | Phase 1 |
| Phase 6: Learning Guide | 2 | Phase 1 |

Phase 2~6은 서로 독립적이므로 **병렬 구현 가능**.

---

## 9. 성공 기준

| 기준 | 측정 방법 |
|------|----------|
| 서비스 실행 중 → 모든 페이지 정상 표시 | 6개 페이지 순회 |
| 서비스 미실행 → 정적 정보 표시 + 오류 안내 | 서비스 중지 후 확인 |
| API 탐색기에서 11개 API 호출 가능 | 각 API 실행 |
| Pact 계약 3건 카드 표시 | Contracts 페이지 확인 |
| 학습 가이드 7단계 진행 가능 | 1~7 단계 순회 |
| 빌드 도구 없이 즉시 실행 | python3 -m http.server로 확인 |

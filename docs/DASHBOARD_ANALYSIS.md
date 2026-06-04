# Pact Conference Demo 대시보드 - 분석서

## 1. 현황 분석

### 1.1 프로젝트 구조

| 모듈 | 역할 | 포트 | API 수 |
|------|------|------|--------|
| common | 공유 도메인 모델 | - | - |
| session-service | Provider (세션 CRUD) | 8081 | 5개 |
| attendee-service | Consumer (참석자 CRUD + 세션 조회) | 8080 | 6개 |
| Pact Broker | 계약 저장소 (Docker) | 9292 | - |
| PostgreSQL | Broker DB (Docker) | 5432 | - |

### 1.2 기존 시각화 도구

현재 프로젝트에 이미 내장된 UI:

| 도구 | URL | 제공 기능 |
|------|-----|----------|
| Swagger UI (Session) | http://localhost:8081/swagger-ui.html | 세션 API 탐색/실행 |
| Swagger UI (Attendee) | http://localhost:8080/swagger-ui.html | 참석자 API 탐색/실행 |
| Pact Broker UI | http://localhost:9292 | 계약 현황/검증 결과 |
| Gradle Test Report | build/reports/tests/test/index.html | 테스트 실행 결과 |

### 1.3 누락된 시각화

기존 도구들이 커버하지 못하는 영역:

- 시스템 전체 구조를 한눈에 보는 통합 뷰
- 서비스 간 통신 흐름 시각화
- CDC 테스트 흐름을 단계별로 이해하는 학습 가이드
- 서비스 Health Check 통합 모니터링
- Pact 계약의 요청/응답 구조를 코드 없이 이해하는 뷰
- 실제 API 호출과 계약 정의의 비교 뷰

---

## 2. 대시보드 요구사항

### 2.1 대상 사용자

| 사용자 | 니즈 | 우선순위 |
|--------|------|----------|
| CDC 학습자 | 계약 테스트 개념을 시각적으로 이해 | 최우선 |
| 데모 발표자 | 프로젝트를 청중에게 보여주는 도구 | 높음 |
| 개발자 | API 상태 확인 + 빠른 테스트 | 중간 |

### 2.2 기능 요구사항

#### 필수 (MVP)

| ID | 기능 | 설명 | 데이터 소스 |
|----|------|------|-----------|
| F1 | 아키텍처 시각화 | 서비스 간 관계, 통신 흐름을 SVG로 표시 | 정적 |
| F2 | 서비스 상태 | 각 서비스의 alive/dead 상태 표시 (10초 폴링) | HTTP ping |
| F3 | 계약 현황 | 3개 Pact 계약의 요약 카드 | build/pacts/ JSON |
| F4 | 계약 상세 | 각 계약의 요청/응답/매칭 규칙 표시 | build/pacts/ JSON |
| F5 | API 탐색기 | 11개 API를 선택하여 호출하고 응답 확인 | 실시간 fetch |
| F6 | 학습 가이드 | CDC 테스트 흐름을 7단계로 안내 | 정적 JSON |

#### 선택 (Full Version)

| ID | 기능 | 설명 | 복잡도 |
|----|------|------|--------|
| F7 | 테스트 실행 | 대시보드에서 Gradle 테스트 트리거 | 높음 |
| F8 | Pact Broker 연동 | Broker API로 게시된 계약 조회 | 중간 |
| F9 | 계약 vs 실제 비교 | Pact 계약 정의와 실제 API 응답 나란히 비교 | 중간 |
| F10 | 다크 모드 | UI 테마 전환 | 낮음 |

### 2.3 비기능 요구사항

| 항목 | 기준 |
|------|------|
| 빌드 도구 | 불필요 (정적 파일, 브라우저에서 직접 실행) |
| 외부 의존성 | 없음 (CDN 포함하지 않음) |
| 브라우저 지원 | Chrome, Firefox, Safari 최신 2개 버전 |
| 응답 시간 | 페이지 로드 1초 미만, API 호출 5초 타임아웃 |
| 언어 | 한국어 |

---

## 3. 기술 스택 비교

### 3.1 후보

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A. React + Vite** | SPA 프레임워크 | 풍부한 UI, 컴포넌트 재사용 | npm 의존성, 빌드 필요, 기술 스택 불일치 |
| **B. Next.js** | 풀스택 React | SSR, API Routes | 과도한 프레임워크, 빌드 필요 |
| **C. 정적 HTML/CSS/JS** | Vanilla JS + ES Modules | 빌드 불필요, 즉시 실행, 경량 | 상태 관리 수동, 컴포넌트 재사용 제한 |
| **D. Thymeleaf + HTMX** | Spring Boot 내장 | 기존 스택 일관, CORS 불필요 | Gradle 빌드 의존, 서비스 재시작 필요 |

### 3.2 추천: C안 (정적 HTML/CSS/JS)

**선택 근거:**

1. **빌드 불필요** - `npm install`, `gradlew build` 없이 브라우저에서 `index.html`을 열면 즉시 실행
2. **기존 프로젝트 무침범** - `settings.gradle.kts` 수정 없음, Gradle 빌드에 영향 없음
3. **학습 데모에 최적** - 대시보드 자체가 목적이 아니라 학습 보조 도구
4. **충분한 기능** - Vanilla JS + Fetch API + ES Modules로 SPA 라우팅, API 호출, DOM 조작 모두 가능
5. **의존성 제로** - CDN, npm, 외부 라이브러리 없이 순수 브라우저 API만 사용

**CORS 해결:**
- 두 서비스의 Controller에 `@CrossOrigin` 어노테이션 추가 (최소 변경)
- 또는 Python 내장 서버로 대시보드 서빙: `python3 -m http.server 3000`

---

## 4. 위험 요소

### 4.1 중복 구현 위험

| 기능 | 기존 도구 | 대시보드에서 할 것 | 안 할 것 |
|------|----------|------------------|---------|
| API 탐색 | Swagger UI | 간소화된 호출 + 응답 뷰 | 전체 스펙 표시 (Swagger 링크 제공) |
| 계약 현황 | Pact Broker UI | 로컬 Pact 파일 기반 요약 | Broker 전체 기능 재구현 |
| 테스트 결과 | Gradle Report | 결과 요약 + 실행 명령 안내 | Gradle 직접 실행 (MVP에서 제외) |

### 4.2 기술 위험

| 위험 | 영향 | 완화 방안 |
|------|------|----------|
| CORS 차단 | API 호출 불가 | @CrossOrigin 추가 또는 로컬 프록시 |
| 서비스 미실행 | 빈 대시보드 | 정적 데이터로 fallback 표시 |
| Pact 파일 미생성 | 계약 탭 빈칸 | "테스트를 먼저 실행하세요" 안내 |
| Actuator 미포함 | Health check 제한 | 단순 HTTP 200 체크로 대체 |

### 4.3 전제 조건

1. 두 서비스에 `@CrossOrigin` 추가 허용
2. 대시보드는 `dashboard/` 디렉토리에 독립 (Gradle 모듈 아님)
3. 서비스가 로컬에서 실행 중일 때 최적, 미실행 시 정적 정보만 표시
4. 인메모리 데이터는 서비스 재시작 시 초기화됨을 사용자에게 안내

---

## 5. 미결 사항 (Open Questions)

| 번호 | 질문 | 영향 범위 | 기본값 |
|------|------|----------|--------|
| OQ1 | 대시보드의 주 사용자는? | UI 복잡도 | CDC 학습자 |
| OQ2 | 서비스에 Actuator 추가 허용? | Health check 깊이 | 단순 HTTP ping |
| OQ3 | API 탐색기에서 쓰기(POST/PUT/DELETE) 허용? | 데이터 오염 위험 | GET만 기본, 쓰기는 토글 |
| OQ4 | 다크 모드 필요? | 추가 CSS 작업 | Phase 2로 연기 |
| OQ5 | Pact Broker 연동을 MVP에 포함? | Broker CORS/인증 이슈 | Phase 2로 연기 |

# 대시보드 및 모니터링 가이드

**Conference Management System — Phase 1**

> 이 가이드는 Pact Conference Demo Phase 1 프로젝트에서 제공하는 세 가지 대시보드와 모니터링 도구의 설치, 설정, 활용 방법을 다룹니다.

---

## 목차

1. [대시보드 개요](#1-대시보드-개요)
2. [HTML 대시보드](#2-html-대시보드)
3. [Grafana + Prometheus 모니터링](#3-grafana--prometheus-모니터링)
4. [Swagger UI (OpenAPI)](#4-swagger-ui-openapi)
5. [Pact Broker 대시보드](#5-pact-broker-대시보드)
6. [Spring Actuator 엔드포인트](#6-spring-actuator-엔드포인트)
7. [트러블슈팅](#7-트러블슈팅)
8. [빠른 시작 체크리스트](#8-빠른-시작-체크리스트)

---

## 1. 대시보드 개요

이 프로젝트는 목적이 다른 세 가지 대시보드를 제공합니다. 각 도구는 서로 보완적으로 동작하며, 용도에 따라 선택적으로 사용할 수 있습니다.

### 1.1 대시보드 유형 비교

| 구분 | HTML 대시보드 | Grafana + Prometheus | Swagger UI |
|------|--------------|---------------------|-----------|
| 파일 위치 | `dashboard/index.html` | `docker-compose.monitoring.yml` | 각 서비스 내장 |
| 접속 방식 | 파일 직접 열기 (브라우저) | http://localhost:3000 | http://localhost:{port}/swagger-ui.html |
| 사전 요구사항 | 없음 | Docker, Docker Compose | 서비스 실행 중 |
| 주 용도 | 프로젝트 전체 현황 파악 | 실시간 서비스 모니터링 | API 문서 및 테스트 |
| 데이터 갱신 | 정적 (코드 수정 시 변경) | 실시간 (30초 자동 갱신) | 정적 (코드 기반) |

### 1.2 각 대시보드의 목적

**HTML Interactive Dashboard (`dashboard/index.html`)**

프로젝트 전체 상태를 한 페이지에서 파악하기 위한 정적 대시보드입니다. 시스템 아키텍처 다이어그램, 개발 Phase 로드맵, 모든 API 엔드포인트 목록, 테스트 피라미드, 기술 스택을 시각적으로 표현합니다. Docker나 서버 실행 없이 브라우저만으로 볼 수 있어 오프라인 환경에서도 활용 가능합니다.

**Grafana + Prometheus 모니터링 스택**

실행 중인 서비스의 성능과 상태를 실시간으로 추적하기 위한 모니터링 도구입니다. JVM 메모리, HTTP 요청 처리율, 응답시간, 에러율 같은 기술 메트릭과 함께, 총 참석자 수·세션 수·제안 수·투표 수 같은 비즈니스 메트릭도 시각화합니다. Prometheus가 15초마다 각 서비스에서 메트릭을 수집하고, Grafana가 이를 대시보드로 표시합니다.

**Swagger UI (SpringDoc OpenAPI)**

각 마이크로서비스의 REST API를 문서화하고 브라우저에서 직접 테스트할 수 있는 도구입니다. 인증 토큰을 입력하면 JWT 보호 엔드포인트도 테스트할 수 있습니다.

---

## 2. HTML 대시보드

### 2.1 접속 방법

서버 실행 없이 브라우저에서 파일을 직접 엽니다.

```bash
# macOS
open /Users/janghoesu/work/ebook-capture/output/ebook/pact-conference-demo-phase1/dashboard/index.html

# 또는 브라우저 주소창에 직접 입력
file:///Users/janghoesu/work/ebook-capture/output/ebook/pact-conference-demo-phase1/dashboard/index.html
```

`start-all.sh` 실행 시 터미널에도 파일 경로가 출력됩니다:

```
================================================================
 Dashboard
================================================================
  file:///path/to/dashboard/index.html
```

### 2.2 기능 상세 설명

#### 상단 네비게이션 바

화면 상단에 고정(sticky)되는 네비게이션 바는 다크 그라데이션 배경으로 표시됩니다. 아래 섹션으로 빠르게 이동할 수 있습니다.

- **Architecture** — 시스템 아키텍처 다이어그램 섹션
- **Phases** — 개발 Phase 로드맵 섹션
- **Modules** — 모듈별 개요 섹션
- **API** — API 엔드포인트 목록 섹션
- **Tests** — 테스트 피라미드 섹션
- **Docs** — 문서 목록 섹션
- **Quick Start** — 빠른 시작 가이드 섹션

#### 히어로 헤더 (Hero Header)

페이지 최상단에 프로젝트 전체 통계를 표시합니다.

| 지표 | 값 |
|------|----|
| 마이크로서비스 수 | 4개 |
| API 엔드포인트 수 | 26개 |
| 테스트 수 | ~57개 |
| 문서 파일 수 | 30개+ |
| 교재 커버리지 | 11% → 70%+ |
| 추가 코드 라인 | 4,970줄 |

#### 시스템 아키텍처 다이어그램 (Mermaid.js)

Mermaid.js 라이브러리를 사용해 시스템 전체 구조를 동적으로 렌더링합니다. 다이어그램은 다음 계층을 표현합니다.

```
Client (Browser / API Client)
    ↓ HTTPS
API Gateway Layer (Port 8080)
    - Spring Cloud Gateway MVC
    - JWT 인증 필터
    - 요청 라우팅
    ↓ HTTP
Business Services
    - Attendee Service (:8081) — 참석자 관리
    - Session Service  (:8082) — 세션 관리, API Versioning
    - CFP Service      (:8083) — 제안 및 투표
    ↓ /actuator/prometheus
Infrastructure
    - Prometheus (:9090) → Grafana (:3000)
    - Pact Broker (:9292)
    - PostgreSQL (:5432)
```

다이어그램 아래에는 Gateway(:8080), Business Services(:8081-8083), Pact Broker(:9292), Monitoring(:9090/:3000) 포트를 색상별 카드로 표시합니다.

#### Phase 로드맵 타임라인

4개 개발 Phase의 완료 현황을 시각적으로 표시합니다.

| Phase | 우선순위 | 내용 | 파일 | 코드 |
|-------|---------|------|------|------|
| Phase 1 | CRITICAL | CFP 서비스 + 테스트 피라미드 + Pact | 36개 | +1,711줄 |
| Phase 2 | HIGH | Gateway + DTO + JWT + OpenAPI | 38개 | +1,016줄 |
| Phase 3 | MEDIUM | Feature Flag + DDD + Observability | 26개 | +882줄 |
| Phase 4 | LOW | API Versioning + Kubernetes + k6 | 26개 | +1,361줄 |

각 Phase는 색상 코딩된 타임라인 항목으로 표시되며, 완료 여부를 배지로 확인할 수 있습니다.

#### 모듈 Overview 카드

6개 모듈을 Glass-morphism 스타일 카드로 표시합니다. 각 카드에는 모듈 역할, 주요 기능 체크리스트, 포트 번호가 포함됩니다.

- **Gateway** — JWT + 라우팅 (`:8080`)
- **Attendee Service** — 참석자 CRUD, Pact Consumer (`:8081`)
- **Session Service** — 세션 CRUD, API v1/v2, Feature Flag (`:8082`)
- **CFP Service** — 제안 CRUD, 투표, Testcontainers (`:8083`)
- **Common Module** — 공통 DTO, RFC 7807, 예외 처리
- **Infrastructure** — Docker Compose, Prometheus, Kubernetes

#### API 엔드포인트 목록

전체 26개 API를 서비스별로 분류한 테이블입니다. 각 행에 HTTP 메서드 배지, 경로, 설명이 표시됩니다.

#### 테스트 피라미드 시각화

계층형 피라미드 형태로 테스트 전략을 시각화합니다. Clip-path CSS를 사용해 실제 피라미드 모양을 구현했습니다.

| 계층 | 색상 | 내용 |
|------|------|------|
| E2E / Contract Tests | 분홍 (최상단) | Pact Consumer/Provider 테스트 |
| Component Tests | 보라 | Testcontainers 기반 통합 테스트 |
| Integration Tests | 파랑 | Repository, Service 통합 테스트 |
| Unit Tests | 초록 (최하단/가장 넓음) | 도메인 로직 단위 테스트 |

#### Feature Timeline

Phase별 주요 기능 구현 항목을 타임라인으로 표시합니다.

#### Tech Stack 배지

프로젝트에서 사용하는 기술 스택을 카테고리별 배지로 표시합니다.

| 카테고리 | 기술 |
|---------|------|
| Backend | Spring Boot 3.3, Kotlin 2.0 |
| Testing | Pact JVM, Testcontainers, JUnit 5 |
| Infra | Docker, Kubernetes, Grafana, Prometheus |
| API | SpringDoc OpenAPI, JWT (JJWT 0.12.6) |

### 2.3 UI 구성 요소 가이드

#### Glass-morphism 카드

```
background: rgba(255, 255, 255, 0.05)
backdrop-filter: blur(10px)
border: 1px solid rgba(255, 255, 255, 0.1)
```

반투명 효과로 다크 배경 위에서 내용을 표시합니다. 주로 헤더 영역의 통계 카드에 사용됩니다.

#### HTTP 메서드 배지 색상

| 메서드 | 배경색 | 텍스트색 | 의미 |
|--------|--------|---------|------|
| `GET` | 연초록 (#dcfce7) | 진초록 (#15803d) | 조회 |
| `POST` | 연파랑 (#dbeafe) | 진파랑 (#1d4ed8) | 생성 |
| `PUT` | 연노랑 (#fef9c3) | 주황 (#a16207) | 수정 |
| `DELETE` | 연빨강 (#fee2e2) | 진빨강 (#dc2626) | 삭제 |

#### 상태 배지 색상

| 배지 | 사용 상황 |
|------|---------|
| `CRITICAL` (빨강) | 즉시 처리 필요, Phase 1 |
| `HIGH` (노랑) | 높은 우선순위, Phase 2 |
| `MEDIUM` (파랑) | 중간 우선순위, Phase 3 |
| `LOW` (초록) | 낮은 우선순위, Phase 4 |
| `완료` (밝은 초록) | 완료된 항목 |

### 2.4 사용 팁

- 네비게이션 링크를 클릭하면 해당 섹션으로 스크롤됩니다 (smooth scroll).
- 서비스 카드에 마우스를 올리면 위로 4px 이동하는 hover 애니메이션이 동작합니다.
- Tech Stack 배지에 마우스를 올리면 1.03배 확대 효과가 있습니다.
- 화면 폭이 좁으면 Tailwind CSS 반응형 레이아웃이 자동으로 1열로 전환됩니다 (`md:grid-cols-2`, `lg:grid-cols-3`).
- 코드 경로나 기술명은 `JetBrains Mono` 폰트로 표시되어 가독성이 높습니다.
- Mermaid.js 다이어그램 렌더링에는 CDN이 필요합니다. 인터넷이 없는 환경에서는 다이어그램이 표시되지 않을 수 있습니다.

---

## 3. Grafana + Prometheus 모니터링

### 3.1 설치 및 실행

#### 사전 요구사항

- **Docker Desktop** 또는 **Docker Engine** 설치 (v20.10+)
- **Docker Compose** v2.0+ (Docker Desktop에 포함)
- **JDK 21** (서비스 직접 실행 시 필요)
- 포트 3000(Grafana), 9090(Prometheus) 미사용 확인

버전 확인:

```bash
docker --version
# Docker version 24.x.x

docker compose version
# Docker Compose version v2.x.x
```

#### 전체 스택 시작: start-all.sh

`start-all.sh`는 4개 서비스를 JAR로 빌드하고 백그라운드에서 실행합니다.

```bash
# 프로젝트 루트 디렉토리에서 실행
./start-all.sh
```

스크립트 실행 단계:

1. **Java 설치 확인** — `java` 명령어 존재 여부 검사, JDK 21 필요
2. **전체 빌드** (`[1/5]`) — `gradlew clean bootJar -x test --parallel` 실행. 테스트를 제외하고 병렬 빌드해 시간 단축
3. **Session Service 시작** (`[2/5]`) — `java -jar *.jar --server.port=8082`, 로그: `/tmp/session-service.log`
4. **Attendee Service 시작** (`[3/5]`) — `java -jar *.jar --server.port=8081`, 로그: `/tmp/attendee-service.log`
5. **CFP Service 시작** (`[4/5]`) — `java -jar *.jar --server.port=8083`, 로그: `/tmp/cfp-service.log`
6. **Gateway 시작** (`[5/5]`) — `java -jar *.jar --server.port=8080`, 로그: `/tmp/gateway.log`
7. **20초 대기** — 서비스 기동 완료를 기다리는 대기 시간
8. **헬스 체크** — 각 포트(8082, 8081, 8083, 8080)에 `/actuator/health` 요청으로 기동 상태 확인
9. **PID 파일 저장** — `.service-pids` 파일에 4개 프로세스 PID 기록 (stop-all.sh에서 사용)

성공 출력 예시:

```
=== Conference Management System ===
[1/5] Building all modules...
Build complete.
[2/5] Starting Session Service (port 8082)...
  Session Service PID: 12345
...
Health check...
  :8082 - OK
  :8081 - OK
  :8083 - OK
  :8080 - OK
```

#### 모니터링 스택 시작

서비스가 실행된 후 별도 터미널에서 모니터링 스택을 시작합니다.

```bash
# 모니터링 스택만 시작 (단독 실행)
docker compose -f docker-compose.monitoring.yml up -d

# 또는 메인 스택과 함께 시작
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d

# 실행 확인
docker compose -f docker-compose.monitoring.yml ps
```

컨테이너 기동 확인:

```bash
# 예상 출력
NAME          IMAGE                         STATUS
prometheus    prom/prometheus:v2.53.0       Up X minutes
grafana       grafana/grafana:11.1.0        Up X minutes
```

#### 서비스 종료: stop-all.sh

```bash
./stop-all.sh
```

종료 로직:

1. `.service-pids` 파일이 있으면: 파일에 저장된 PID로 각 프로세스를 `kill` 명령으로 종료 후 PID 파일 삭제
2. PID 파일이 없으면: `lsof`로 8080-8083 포트를 사용하는 프로세스를 찾아서 종료 (폴백 방식)

#### 모니터링 스택 종료

```bash
# 컨테이너 중지 (데이터 볼륨 유지)
docker compose -f docker-compose.monitoring.yml down

# 컨테이너 + 볼륨까지 완전 삭제 (메트릭 데이터 초기화)
docker compose -f docker-compose.monitoring.yml down -v
```

#### 로그 확인

```bash
# 서비스 로그 실시간 확인
tail -f /tmp/gateway.log
tail -f /tmp/attendee-service.log
tail -f /tmp/session-service.log
tail -f /tmp/cfp-service.log

# Docker 컨테이너 로그
docker compose -f docker-compose.monitoring.yml logs -f prometheus
docker compose -f docker-compose.monitoring.yml logs -f grafana
```

### 3.2 Prometheus 설정

**접속 URL:** http://localhost:9090

#### prometheus.yml 설정 상세

```yaml
global:
  scrape_interval: 15s       # 15초마다 메트릭 수집
  evaluation_interval: 15s   # 15초마다 알림 룰 평가

scrape_configs:
  - job_name: 'gateway'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
        labels:
          service: 'gateway'

  - job_name: 'attendee-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          service: 'attendee-service'

  - job_name: 'session-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8082']
        labels:
          service: 'session-service'

  - job_name: 'cfp-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8083']
        labels:
          service: 'cfp-service'
```

**`host.docker.internal` 사용 이유:**

Spring Boot 서비스는 Docker 컨테이너가 아닌 호스트 머신에서 직접 실행됩니다. Prometheus는 Docker 컨테이너 내부에서 실행되므로, `localhost`로는 호스트의 서비스에 접근할 수 없습니다. `host.docker.internal`은 Docker Desktop이 제공하는 특수 호스트명으로, 컨테이너에서 호스트 머신의 네트워크 인터페이스로 라우팅됩니다.

`docker-compose.monitoring.yml`의 다음 설정으로 이 기능이 활성화됩니다:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

Linux 환경에서는 이 설정 없이는 `host.docker.internal`이 동작하지 않으므로 필수입니다.

**`/actuator/prometheus` 경로:**

Spring Boot Actuator + Micrometer + `micrometer-registry-prometheus` 의존성이 있을 때 활성화됩니다. Prometheus 형식의 텍스트로 메트릭을 노출합니다.

#### Prometheus Web UI 주요 기능

- **http://localhost:9090/targets** — 수집 대상 서비스 상태 확인 (UP/DOWN)
- **http://localhost:9090/graph** — PromQL 쿼리 실행 및 그래프 시각화
- **http://localhost:9090/metrics** — Prometheus 자체 메트릭 확인

#### 주요 메트릭 종류

**JVM 메트릭** (Spring Boot Actuator 자동 제공):

| 메트릭명 | 설명 | 단위 |
|---------|------|------|
| `jvm_memory_used_bytes` | JVM 메모리 사용량 (heap/non-heap 구분) | bytes |
| `jvm_memory_max_bytes` | JVM 최대 메모리 설정 | bytes |
| `jvm_threads_live_threads` | 현재 활성 스레드 수 | count |
| `jvm_threads_daemon_threads` | 데몬 스레드 수 | count |
| `jvm_gc_pause_seconds` | GC 정지 시간 히스토그램 | seconds |
| `jvm_classes_loaded_classes` | 로드된 클래스 수 | count |

**HTTP 서버 메트릭**:

| 메트릭명 | 설명 | 단위 |
|---------|------|------|
| `http_server_requests_seconds_count` | HTTP 요청 총 건수 | count |
| `http_server_requests_seconds_sum` | HTTP 요청 처리 시간 합계 | seconds |
| `http_server_requests_seconds_bucket` | HTTP 응답시간 히스토그램 버킷 | count |

레이블: `method`, `uri`, `status`, `service`

**비즈니스 메트릭** (커스텀 Gauge):

| 메트릭명 | 설명 | 서비스 |
|---------|------|--------|
| `conference_attendees_total` | 등록된 참석자 총 수 | Attendee Service |
| `conference_sessions_total` | 생성된 세션 총 수 | Session Service |
| `conference_proposals_total` | 제출된 CFP 제안 총 수 | CFP Service |
| `conference_votes_total` | 투표 총 수 | CFP Service |

**프로세스 메트릭**:

| 메트릭명 | 설명 |
|---------|------|
| `process_uptime_seconds` | 프로세스 가동 시간 |
| `process_cpu_usage` | CPU 사용률 (0~1) |
| `up` | 서비스 수집 성공 여부 (1=UP, 0=DOWN) |

#### PromQL 쿼리 예시

**1. 서비스 가동 상태 확인**

```promql
up{service="gateway"}
```

결과: `1` (UP), `0` (DOWN)

**2. 서비스별 초당 HTTP 요청 처리율 (1분 이동평균)**

```promql
sum(rate(http_server_requests_seconds_count[1m])) by (service)
```

**3. Session Service의 응답시간 p50 / p95 / p99**

```promql
# p50 (중앙값)
histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket{service="session-service"}[5m])) by (le))

# p95
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{service="session-service"}[5m])) by (le))

# p99
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket{service="session-service"}[5m])) by (le))
```

**4. 서비스별 5xx 에러율**

```promql
sum(rate(http_server_requests_seconds_count{service="gateway", status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{service="gateway"}[5m]))
```

**5. 전체 서비스 JVM Heap 메모리 사용량**

```promql
sum(jvm_memory_used_bytes{area="heap"}) by (service)
```

**6. JVM Heap 메모리 사용률 (%)**

```promql
sum(jvm_memory_used_bytes{area="heap", service="session-service"})
/
sum(jvm_memory_max_bytes{area="heap", service="session-service"}) * 100
```

**7. 최근 5분 에러 발생 건수**

```promql
sum(increase(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service, uri)
```

**8. 특정 URI 평균 응답시간**

```promql
sum(rate(http_server_requests_seconds_sum{uri="/sessions"}[1m]))
/
sum(rate(http_server_requests_seconds_count{uri="/sessions"}[1m]))
```

**9. 총 참석자 수 (비즈니스 메트릭)**

```promql
conference_attendees_total
```

**10. 프로세스 가동 시간 (초)**

```promql
process_uptime_seconds{service="gateway"}
```

### 3.3 Grafana 대시보드

**접속 URL:** http://localhost:3000

**기본 계정:**
- 아이디: `admin`
- 비밀번호: `admin`

> 최초 로그인 시 비밀번호 변경을 요청합니다. 개발 환경에서는 변경하지 않고 "Skip"을 눌러도 됩니다.

#### 자동 프로비저닝

Grafana는 시작 시 `monitoring/grafana/provisioning/` 디렉토리를 자동으로 읽어 데이터소스와 대시보드를 설정합니다. 수동 설정이 필요 없습니다.

**데이터소스 자동 설정 (`datasources/datasource.yml`):**

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090  # 컨테이너 내부 네트워크로 Prometheus에 연결
    isDefault: true
    editable: true
```

`access: proxy` 방식은 브라우저가 아닌 Grafana 서버가 Prometheus에 직접 쿼리합니다. 컨테이너 간 통신이므로 `http://prometheus:9090`을 사용합니다.

**대시보드 자동 로드 (`dashboards/dashboard.yml`):**

```yaml
apiVersion: 1
providers:
  - name: 'Conference'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards        # 컨테이너 내부 경로
      foldersFromFilesStructure: false
```

호스트의 `monitoring/grafana/dashboards/` 디렉토리가 컨테이너의 `/var/lib/grafana/dashboards`에 마운트됩니다. 디렉토리의 JSON 파일을 자동으로 대시보드로 등록합니다.

#### 대시보드 패널 구성 (conference.json)

대시보드 UID: `conference-dashboard-v1`
자동 갱신: 30초
기본 시간 범위: 최근 1시간

대시보드는 5개 섹션(Row)으로 구성됩니다.

---

**Row 1: 서비스 상태 (Service Health)**

총 4개 Stat 패널로 구성됩니다. 각 서비스의 `up` 메트릭을 표시하며, 배경색이 초록(UP) / 빨강(DOWN)으로 변경됩니다.

| 패널 ID | 제목 | PromQL |
|---------|------|--------|
| 1 | Gateway (:8080) | `up{service="gateway"}` |
| 2 | Attendee Service (:8081) | `up{service="attendee-service"}` |
| 3 | Session Service (:8082) | `up{service="session-service"}` |
| 4 | CFP Service (:8083) | `up{service="cfp-service"}` |

표시 값 매핑: `0` → "DOWN" (빨강), `1` → "UP" (초록)

---

**Row 2: HTTP 요청 처리량 & 지연시간**

2개 Time series 패널로 구성됩니다.

| 패널 ID | 제목 | PromQL | 단위 |
|---------|------|--------|------|
| 10 | HTTP 요청 처리율 (req/s) | `sum(rate(http_server_requests_seconds_count{service="..."}[1m]))` | req/s |
| 11 | HTTP 응답시간 p95 (목표: <100ms) | `histogram_quantile(0.95/0.99, ...)` | 초(s) |

패널 11의 임계값 설정:
- 초록: 0~100ms 이하
- 노랑: 100~500ms
- 빨강: 500ms 초과

---

**Row 3: JVM 메모리 사용량**

1개 Time series 패널로 구성됩니다.

| 패널 ID | 제목 | PromQL | 단위 |
|---------|------|--------|------|
| 20 | JVM Heap 메모리 사용량 (서비스별) | `sum(jvm_memory_used_bytes{area="heap", service="..."}) by (service)` | bytes |

범례(legend): 평균, 최댓값, 최신값 표시. 우측 배치.

---

**Row 4: 비즈니스 메트릭 (Business Metrics)**

4개 Stat 패널로 구성됩니다.

| 패널 ID | 제목 | PromQL | 배경색 |
|---------|------|--------|--------|
| 30 | 총 참석자 (Attendees) | `conference_attendees_total` | 파랑 |
| 31 | 총 세션 (Sessions) | `conference_sessions_total` | 초록 |
| 32 | 총 제안 (Proposals) | `conference_proposals_total` | 보라 |
| 33 | 총 투표 수 (Votes) | `conference_votes_total` | 주황 |

`graphMode: "area"` 설정으로 배경에 면적 그래프도 함께 표시됩니다.

---

**Row 5: 에러율 & Uptime**

2개 패널로 구성됩니다.

| 패널 ID | 제목 | 타입 | 설명 |
|---------|------|------|------|
| 40 | 에러율 (목표: <1%) | Time series | 5xx 에러율. 노랑(1%), 빨강(5%) 임계값 |
| 41 | 프로세스 Uptime | Gauge (LCD) | 초 단위 가동 시간. 수평 막대 게이지 |

패널 41의 임계값: 빨강(0~60초), 노랑(60~3600초), 초록(3600초 이상 = 1시간+)

---

#### 커스텀 패널 추가 방법

1. Grafana 대시보드 페이지 우상단 **"Edit"** 버튼 클릭
2. **"Add panel"** 버튼 클릭
3. 패널 유형 선택 (Stat / Time series / Gauge / Bar chart 등)
4. **"Query"** 탭에서 PromQL 입력
5. **"Panel options"** 탭에서 제목, 단위, 임계값 설정
6. **"Save dashboard"** 클릭

변경 사항을 영구 저장하려면 `monitoring/grafana/dashboards/conference.json`을 직접 수정합니다.

---

## 4. Swagger UI (OpenAPI)

각 서비스는 SpringDoc OpenAPI 라이브러리를 통해 Swagger UI를 제공합니다. 서비스가 실행 중일 때만 접근 가능합니다.

### 4.1 서비스별 Swagger UI URL

| 서비스 | Swagger UI | OpenAPI JSON |
|--------|-----------|-------------|
| Attendee Service | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |
| Session Service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs |
| CFP Service | http://localhost:8083/swagger-ui.html | http://localhost:8083/v3/api-docs |

> **참고:** start-all.sh 출력에서는 `/swagger-ui.html` 경로를 안내합니다. SpringDoc은 `/swagger-ui/index.html`과 `/swagger-ui.html` 모두 접근 가능하며, 후자는 전자로 리다이렉트됩니다.

### 4.2 Swagger UI 사용 방법

#### API 목록 확인

Swagger UI 접속 시 해당 서비스의 모든 API 엔드포인트가 HTTP 메서드별로 그룹화되어 표시됩니다.

#### Request 테스트 (Try it out)

1. 테스트할 API 항목 클릭 (펼치기)
2. **"Try it out"** 버튼 클릭
3. 필요한 파라미터 또는 Request Body 입력
4. **"Execute"** 버튼 클릭
5. **"Responses"** 섹션에서 실제 응답 확인 (HTTP 상태코드, 헤더, 본문)

#### JWT 인증이 필요한 API 테스트

일부 API는 JWT 토큰이 필요합니다. 토큰을 먼저 발급하고 Swagger에 등록합니다.

1. Gateway에서 토큰 발급:

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "password"}'
```

2. 응답의 `token` 값 복사

3. Swagger UI 우상단 **"Authorize"** 버튼 클릭

4. Value 입력란에 `Bearer {토큰값}` 형식으로 입력

5. **"Authorize"** → **"Close"** 클릭

이후 자물쇠 아이콘이 잠긴 API들도 인증 헤더가 자동으로 추가됩니다.

#### OpenAPI 스펙 JSON 활용

`/v3/api-docs` 엔드포인트에서 OpenAPI 3.0 형식의 JSON을 다운로드할 수 있습니다.

```bash
# Session Service API 스펙 다운로드
curl http://localhost:8082/v3/api-docs | jq . > session-api-spec.json

# Postman 컬렉션으로 변환 (openapi-to-postmanv2 설치 필요)
npx openapi-to-postmanv2 convert -s session-api-spec.json -o postman-collection.json
```

---

## 5. Pact Broker 대시보드

**접속 URL:** http://localhost:9292

**기본 계정:**
- 아이디: `pact`
- 비밀번호: `pact`

### 5.1 사전 요구사항

Pact Broker는 메인 `docker-compose.yml`로 실행합니다. PostgreSQL 의존성이 있습니다.

```bash
# 메인 스택 실행 (Pact Broker + PostgreSQL 포함)
docker compose up -d

# 실행 상태 확인
docker compose ps

# 예상 출력
NAME              STATUS
pact-broker       Up X minutes (healthy)
pactbroker-db     Up X minutes
```

Pact Broker가 완전히 시작되기까지 약 30초 소요될 수 있습니다.

### 5.2 Pact Broker 주요 기능

#### 계약(Pact) 파일 확인

Consumer 테스트 실행 후 생성된 Pact 파일이 Broker에 게시됩니다. 대시보드의 메인 화면에서 Consumer-Provider 쌍별로 최신 계약을 확인할 수 있습니다.

- **Matrix 뷰** — Consumer와 Provider의 계약 및 검증 결과 매트릭스
- **Consumer 뷰** — Consumer별 게시된 Pact 목록
- **Provider 뷰** — Provider별 검증 결과 목록

#### 검증 결과 매트릭스

`http://localhost:9292/matrix` 페이지에서 각 Consumer-Provider 쌍의 계약 검증 결과를 확인합니다.

| 열 | 설명 |
|----|------|
| Consumer | Pact를 생성한 서비스 |
| Provider | 계약을 검증하는 서비스 |
| Consumer Version | Consumer 애플리케이션 버전 |
| Provider Version | Provider 애플리케이션 버전 |
| Verification Result | 성공(초록)/실패(빨강) |

#### can-i-deploy 게이트

배포 전 특정 버전의 서비스가 호환 가능한지 확인합니다.

```bash
# CFP Service v1.0.0이 Session Service와 함께 배포 가능한지 확인
curl "http://localhost:9292/can-i-deploy?pacticipant=cfp-service&version=1.0.0&to=production"

# 또는 pact-broker CLI 사용
pact-broker can-i-deploy \
  --pacticipant cfp-service \
  --version 1.0.0 \
  --to production \
  --broker-base-url http://localhost:9292 \
  --broker-username pact \
  --broker-password pact
```

#### 서비스 관계 네트워크 그래프

`http://localhost:9292/groups` 에서 서비스 간 계약 관계를 네트워크 그래프로 시각화합니다. 화살표 방향이 Consumer → Provider 방향을 나타냅니다.

### 5.3 Pact Broker 활용 워크플로우

```
1. Consumer 테스트 실행 (CFP → Session)
   ./gradlew :cfp-service:test -Ptest.type=contract

2. Pact 파일 Broker에 게시
   ./gradlew :cfp-service:pactPublish

3. Broker UI에서 계약 확인
   http://localhost:9292

4. Provider 검증 실행 (Session)
   ./gradlew :session-service:test -Ptest.type=provider

5. can-i-deploy로 배포 가능 여부 확인
```

---

## 6. Spring Actuator 엔드포인트

모든 서비스는 Spring Boot Actuator를 통해 운영 관리 엔드포인트를 노출합니다.

### 6.1 서비스별 Actuator URL

**Gateway (포트 8080):**

| 엔드포인트 | URL | 설명 |
|-----------|-----|------|
| Health | http://localhost:8080/actuator/health | 게이트웨이 헬스 체크 |
| Prometheus | http://localhost:8080/actuator/prometheus | 게이트웨이 메트릭 |
| Info | http://localhost:8080/actuator/info | 서비스 정보 |

**Attendee Service (포트 8081):**

| 엔드포인트 | URL | 설명 |
|-----------|-----|------|
| Health | http://localhost:8081/actuator/health | 서비스 헬스 체크 |
| Prometheus | http://localhost:8081/actuator/prometheus | 서비스 메트릭 |
| Info | http://localhost:8081/actuator/info | 서비스 정보 |

**Session Service (포트 8082):**

| 엔드포인트 | URL | 설명 |
|-----------|-----|------|
| Health | http://localhost:8082/actuator/health | 서비스 헬스 체크 |
| Prometheus | http://localhost:8082/actuator/prometheus | 서비스 메트릭 |
| Info | http://localhost:8082/actuator/info | 서비스 정보 |

**CFP Service (포트 8083):**

| 엔드포인트 | URL | 설명 |
|-----------|-----|------|
| Health | http://localhost:8083/actuator/health | 서비스 헬스 체크 |
| Prometheus | http://localhost:8083/actuator/prometheus | 서비스 메트릭 |
| Info | http://localhost:8083/actuator/info | 서비스 정보 |

### 6.2 엔드포인트 상세

#### `/actuator/health` — 헬스 체크

서비스 상태를 JSON으로 반환합니다.

```bash
curl http://localhost:8082/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

`status`가 `UP`이면 서비스가 정상입니다. `DOWN` 또는 `OUT_OF_SERVICE`면 문제가 있습니다.

#### `/actuator/prometheus` — Prometheus 메트릭

Prometheus 텍스트 형식으로 메트릭을 반환합니다.

```bash
curl http://localhost:8082/actuator/prometheus
```

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 3.1457280E7
...
# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/sessions",} 42.0
...
# HELP conference_sessions_total Total number of sessions
# TYPE conference_sessions_total gauge
conference_sessions_total 5.0
```

#### `/actuator/info` — 서비스 정보

`application.yml`에 설정된 서비스 메타데이터를 반환합니다.

```bash
curl http://localhost:8082/actuator/info
```

---

## 7. 트러블슈팅

### 7.1 서비스가 시작되지 않을 때

**증상:** `start-all.sh` 실행 후 헬스 체크에서 `Starting...` 메시지가 지속됨

**확인 방법:**

```bash
# 로그 확인
tail -50 /tmp/session-service.log
tail -50 /tmp/gateway.log

# 프로세스 확인
ps aux | grep bootJar
lsof -i :8082
```

**원인 및 해결:**

| 원인 | 해결 방법 |
|------|---------|
| Java 21 미설치 | `java -version` 확인 후 JDK 21 설치 |
| 빌드 실패 | `./gradlew clean bootJar` 단독 실행으로 에러 메시지 확인 |
| 포트 이미 사용 중 | `lsof -ti:8082 | xargs kill` 로 기존 프로세스 종료 |
| 메모리 부족 | `java -Xmx512m -jar ...` 옵션으로 메모리 제한 |
| 데이터베이스 연결 실패 | `docker compose up -d` 로 PostgreSQL 먼저 실행 |

### 7.2 Prometheus가 메트릭을 수집하지 못할 때

**증상:** http://localhost:9090/targets 에서 서비스가 "DOWN" 표시

**확인 방법:**

```bash
# 서비스에서 메트릭 직접 확인
curl http://localhost:8082/actuator/prometheus

# Prometheus 컨테이너에서 서비스 접근 테스트
docker exec -it <prometheus-container-id> \
  wget -qO- http://host.docker.internal:8082/actuator/prometheus | head -5
```

**원인 및 해결:**

| 원인 | 해결 방법 |
|------|---------|
| 서비스 미실행 | `./start-all.sh` 로 서비스 먼저 실행 |
| `host.docker.internal` 미동작 | Linux에서 `extra_hosts` 설정 확인 |
| Actuator 비활성화 | `management.endpoints.web.exposure.include=prometheus` 설정 확인 |
| 방화벽 차단 | macOS 방화벽 설정에서 포트 허용 |

**Prometheus 설정 재로드:**

```bash
# 설정 파일 변경 후 재로드 (재시작 불필요)
curl -X POST http://localhost:9090/-/reload
```

### 7.3 Grafana 대시보드가 표시되지 않을 때

**증상:** Grafana 접속은 되지만 대시보드가 비어 있거나 "No data" 표시

**확인 1 — 대시보드 프로비저닝 확인:**

```bash
docker compose -f docker-compose.monitoring.yml logs grafana | grep -i "dashboard\|provision\|error"
```

**확인 2 — 볼륨 마운트 확인:**

```bash
docker exec -it <grafana-container-id> ls /var/lib/grafana/dashboards/
# conference.json 파일이 있어야 함
```

**확인 3 — 데이터소스 연결 확인:**

Grafana UI → 왼쪽 메뉴 → **Connections** → **Data sources** → **Prometheus** → **"Test"** 버튼 클릭

**해결:**

| 원인 | 해결 방법 |
|------|---------|
| 대시보드 JSON 없음 | `monitoring/grafana/dashboards/conference.json` 파일 확인 |
| Prometheus 연결 실패 | Prometheus 컨테이너 실행 여부 확인 |
| 프로비저닝 경로 오류 | `dashboard.yml`의 `path` 설정 확인 |
| Grafana 캐시 문제 | `docker compose restart grafana` |

### 7.4 Pact Broker 연결 실패

**증상:** http://localhost:9292 접속 불가 또는 로그인 후 오류

**확인 방법:**

```bash
docker compose ps
docker compose logs pact-broker
docker compose logs pactbroker-db
```

**원인 및 해결:**

| 원인 | 해결 방법 |
|------|---------|
| 컨테이너 미실행 | `docker compose up -d` 실행 |
| DB 초기화 중 | 30초 대기 후 재시도 |
| 포트 9292 충돌 | `lsof -i:9292` 확인 후 기존 프로세스 종료 |
| DB 볼륨 손상 | `docker compose down -v && docker compose up -d` |

### 7.5 포트 충돌 해결

서비스 시작 시 포트가 이미 사용 중인 경우:

```bash
# 어떤 프로세스가 포트를 사용하는지 확인
lsof -i :8080
lsof -i :8081
lsof -i :8082
lsof -i :8083
lsof -i :3000
lsof -i :9090
lsof -i :9292

# 특정 포트의 프로세스 종료
lsof -ti:8082 | xargs kill -9

# 또는 PID를 확인 후 종료
kill -9 <PID>
```

Docker 컨테이너 포트 충돌:

```bash
# 3000번 포트를 사용하는 컨테이너 확인
docker ps | grep 3000

# 컨테이너 강제 종료
docker stop <container-id>
```

---

## 8. 빠른 시작 체크리스트

전체 스택을 처음 실행할 때 아래 단계를 순서대로 따르세요.

### 사전 확인

- [ ] Java 21 설치 확인: `java -version`
- [ ] Docker 설치 확인: `docker --version`
- [ ] Docker Compose 설치 확인: `docker compose version`
- [ ] Docker Desktop 실행 중 확인 (macOS/Windows)
- [ ] 포트 확인: 8080, 8081, 8082, 8083, 3000, 9090, 9292 미사용

### Step 1: 인프라 컨테이너 시작

```bash
# PostgreSQL + Pact Broker 시작
docker compose up -d

# 기동 대기 (약 30초)
sleep 30

# 상태 확인
docker compose ps
# pact-broker, pactbroker-db 모두 "Up" 상태여야 함
```

- [ ] Pact Broker 접속 확인: http://localhost:9292 (pact/pact)

### Step 2: Spring Boot 서비스 시작

```bash
./start-all.sh
```

- [ ] 빌드 완료 메시지 확인: `Build complete.`
- [ ] 4개 서비스 PID 출력 확인
- [ ] 헬스 체크 OK 확인: `:8082 - OK`, `:8081 - OK`, `:8083 - OK`, `:8080 - OK`

### Step 3: 모니터링 스택 시작

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

- [ ] Prometheus 기동 확인: http://localhost:9090
- [ ] Grafana 기동 확인: http://localhost:3000 (admin/admin)

### Step 4: HTML 대시보드 열기

```bash
open dashboard/index.html
# 또는 파일 경로를 브라우저 주소창에 직접 입력
```

- [ ] 시스템 아키텍처 다이어그램 표시 확인
- [ ] 서비스 카드 4개 확인

### Step 5: 서비스별 Swagger UI 확인

- [ ] Attendee Swagger: http://localhost:8081/swagger-ui.html
- [ ] Session Swagger: http://localhost:8082/swagger-ui.html
- [ ] CFP Swagger: http://localhost:8083/swagger-ui.html

### Step 6: Grafana 대시보드 확인

1. http://localhost:3000 접속
2. admin / admin 로그인
3. 왼쪽 메뉴 → **Dashboards** → **Conference Management System** 선택
4. Service Health 패널에서 4개 서비스 모두 "UP" 확인

- [ ] Gateway (:8080): UP
- [ ] Attendee Service (:8081): UP
- [ ] Session Service (:8082): UP
- [ ] CFP Service (:8083): UP

### Step 7: Prometheus 수집 확인

1. http://localhost:9090/targets 접속
2. 4개 job 모두 "UP" 상태 확인

- [ ] gateway: UP
- [ ] attendee-service: UP
- [ ] session-service: UP
- [ ] cfp-service: UP

### 종료 순서

```bash
# 1. 모니터링 스택 종료 (데이터 보존)
docker compose -f docker-compose.monitoring.yml down

# 2. Spring Boot 서비스 종료
./stop-all.sh

# 3. 인프라 컨테이너 종료 (데이터 보존)
docker compose down

# 완전 초기화 시 (모든 데이터 삭제)
docker compose down -v
docker compose -f docker-compose.monitoring.yml down -v
```

---

## 포트 정리

| 서비스 | 포트 | 용도 |
|--------|------|------|
| Gateway | 8080 | API 게이트웨이, JWT 인증 |
| Attendee Service | 8081 | 참석자 관리 API |
| Session Service | 8082 | 세션 관리 API |
| CFP Service | 8083 | 제안 및 투표 API |
| PostgreSQL | 5432 | 데이터베이스 |
| Pact Broker | 9292 | 계약 레지스트리 |
| Prometheus | 9090 | 메트릭 수집 |
| Grafana | 3000 | 모니터링 대시보드 |

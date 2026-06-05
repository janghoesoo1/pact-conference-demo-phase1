# Conference Management System - 빠른 시작 가이드

이 가이드는 Conference Management System의 모든 도구를 실제로 사용하는 방법을 단계별로 설명합니다.
각 단계를 순서대로 따라하면 전체 시스템을 직접 체험할 수 있습니다.

---

## 목차

1. [시작하기 전에](#1-시작하기-전에)
2. [전체 시스템 시작하기](#2-전체-시스템-시작하기)
3. [Swagger UI 사용법 (API 테스트)](#3-swagger-ui-사용법-api-테스트)
4. [Grafana 사용법 (실시간 모니터링)](#4-grafana-사용법-실시간-모니터링)
5. [Prometheus 사용법 (메트릭 쿼리)](#5-prometheus-사용법-메트릭-쿼리)
6. [Pact Broker 사용법 (계약 관리)](#6-pact-broker-사용법-계약-관리)
7. [Actuator 엔드포인트 직접 접근](#7-actuator-엔드포인트-직접-접근)
8. [성능 테스트 실행](#8-성능-테스트-실행)
9. [전체 시스템 종료](#9-전체-시스템-종료)
10. [자주 하는 시나리오](#10-자주-하는-시나리오)
11. [트러블슈팅 FAQ](#11-트러블슈팅-faq)

---

## 1. 시작하기 전에

시스템을 시작하기 전에 다음 요구사항을 확인합니다.

### 사전 요구사항 체크리스트

**JDK 21 확인:**
```bash
java -version
```
정상 출력 예시:
```
openjdk version "21.0.x" 2024-xx-xx
OpenJDK Runtime Environment ...
```
`21`이 아닌 버전이 표시되면 JDK 21을 설치해야 합니다.

**Docker 확인:**
```bash
docker info
```
`Server Version:` 항목이 보이면 Docker가 정상 실행 중입니다.
`Cannot connect to the Docker daemon` 메시지가 나오면 Docker Desktop을 먼저 실행하세요.

**포트 충돌 사전 확인:**
```bash
lsof -i :8080,8081,8082,8083,3000,9090,9292,5432 2>/dev/null | grep LISTEN
```
출력이 없으면 모든 포트가 비어 있습니다. 기존 프로세스가 있으면 먼저 종료하세요.

**프로젝트 디렉토리 이동:**
```bash
cd /path/to/pact-conference-demo-phase1
```

---

## 2. 전체 시스템 시작하기

### Step 1: Java 서비스 시작

```bash
./start-all.sh
```

스크립트가 다음 순서로 실행됩니다:

| 단계 | 내용 | 예상 시간 |
|------|------|----------|
| `[1/5] Building all modules` | Gradle로 4개 모듈 전체 빌드 (`clean bootJar -x test --parallel`) | 30-60초 |
| `[2/5] Starting Session Service` | port 8082, 로그: `/tmp/session-service.log` | 즉시 (백그라운드) |
| `[3/5] Starting Attendee Service` | port 8081, 로그: `/tmp/attendee-service.log` | 즉시 (백그라운드) |
| `[4/5] Starting CFP Service` | port 8083, 로그: `/tmp/cfp-service.log` | 즉시 (백그라운드) |
| `[5/5] Starting Gateway` | port 8080, 로그: `/tmp/gateway.log` | 즉시 (백그라운드) |
| 대기 (20초) | 각 서비스 Spring Boot 초기화 완료 대기 | 20초 |
| Health check | 4개 포트 `/actuator/health` 응답 확인 | 즉시 |

**성공 시 출력 예시:**
```
=== Conference Management System ===

Java version: openjdk version "21.0.3" ...
[1/5] Building all modules...
BUILD SUCCESSFUL in 45s
Build complete.

[2/5] Starting Session Service (port 8082)...
  Session Service PID: 12345
[3/5] Starting Attendee Service (port 8081)...
  Attendee Service PID: 12346
[4/5] Starting CFP Service (port 8083)...
  CFP Service PID: 12347
[5/5] Starting Gateway (port 8080)...
  Gateway PID: 12348

Waiting for services to start (20s)...
  [5s] Session service starting...
  [10s] Attendee service starting...
  [15s] CFP service starting...
  [20s] Gateway starting...

Health check...
  :8082 - OK
  :8081 - OK
  :8083 - OK
  :8080 - OK
```

**헬스체크에서 "Starting..."이 나오면:**
```bash
# 5초 더 기다린 후 직접 확인
sleep 5 && curl -s http://localhost:8082/actuator/health | python3 -m json.tool
```

**수동으로 개별 서비스 시작하는 방법:**
```bash
# 예: Session Service만 다시 시작
java -jar session-service/build/libs/*.jar --server.port=8082 \
  > /tmp/session-service.log 2>&1 &

# 로그 실시간 확인
tail -f /tmp/session-service.log
```

### Step 2: Docker 컨테이너 시작

Pact Broker와 모니터링 스택(Prometheus, Grafana)을 Docker로 실행합니다.

**Pact Broker + PostgreSQL 시작:**
```bash
docker compose up -d
```

**모니터링 스택 시작:**
```bash
docker compose -f docker-compose.monitoring.yml up -d
```

**각 컨테이너 역할:**

| 컨테이너 | 이미지 | 포트 | 역할 |
|---------|--------|------|------|
| `postgres` | postgres:17-alpine | 5432 | Pact Broker 메타데이터 저장소 |
| `pact-broker` | pactfoundation/pact-broker:latest | 9292 | 계약 파일 중앙 저장 및 검증 결과 관리 |
| `prometheus` | prom/prometheus:v2.53.0 | 9090 | 4개 서비스에서 15초마다 메트릭 수집 |
| `grafana` | grafana/grafana:11.1.0 | 3000 | Prometheus 데이터 시각화 대시보드 |

**시작 확인:**
```bash
docker ps
```

모든 컨테이너가 `Up`이어야 합니다. pact-broker는 postgres 헬스체크 통과 후 시작되므로 약 10-15초 소요됩니다.

**예상 출력:**
```
CONTAINER ID   IMAGE                              STATUS          PORTS
a1b2c3d4e5f6   grafana/grafana:11.1.0             Up 2 minutes    0.0.0.0:3000->3000/tcp
b2c3d4e5f6g7   prom/prometheus:v2.53.0            Up 2 minutes    0.0.0.0:9090->9090/tcp
c3d4e5f6g7h8   pactfoundation/pact-broker:latest  Up 1 minute     0.0.0.0:9292->9292/tcp
d4e5f6g7h8i9   postgres:17-alpine                 Up 2 minutes    0.0.0.0:5432->5432/tcp
```

**전체 서비스 접속 URL 요약:**

| 서비스 | URL | 인증 |
|--------|-----|------|
| Gateway | http://localhost:8080 | JWT (POST 요청 시 필요) |
| Attendee Service | http://localhost:8081 | 없음 (직접 접속) |
| Session Service | http://localhost:8082 | 없음 (직접 접속) |
| CFP Service | http://localhost:8083 | 없음 (직접 접속) |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | 없음 |
| Pact Broker | http://localhost:9292 | pact / pact |

---

## 3. Swagger UI 사용법 (API 테스트)

각 서비스에는 Swagger UI가 내장되어 있어 브라우저에서 직접 API를 테스트할 수 있습니다.

### 3.1 Swagger UI 접속 URL

- **Session Service**: http://localhost:8082/swagger-ui/index.html
- **Attendee Service**: http://localhost:8081/swagger-ui/index.html
- **CFP Service**: http://localhost:8083/swagger-ui/index.html

> Gateway(8080)는 Swagger UI를 제공하지 않습니다. Gateway를 경유하는 API 테스트는 curl로 진행합니다.

### 3.2 세션 목록 조회 (GET /sessions)

1. 브라우저에서 http://localhost:8082/swagger-ui/index.html 접속
2. 화면에 표시된 API 목록에서 **`GET /sessions`** 클릭
3. 펼쳐진 패널 오른쪽의 **"Try it out"** 버튼 클릭
4. **"Execute"** 버튼 클릭
5. 아래 "Response body"에서 세션 목록 확인

**응답 예시 (200 OK):**
```json
{
  "data": [
    {
      "id": 1,
      "title": "Kotlin과 Spring Boot로 마이크로서비스 구축하기",
      "speaker": "김철수",
      "description": "Kotlin의 간결한 문법과 Spring Boot의 강력한 기능을 활용한 마이크로서비스 아키텍처",
      "dateTime": "2024-11-15T10:00:00"
    },
    {
      "id": 2,
      "title": "Pact로 구현하는 Consumer-Driven Contract Testing",
      "speaker": "이영희",
      "description": "마이크로서비스 간 계약 테스트의 원리와 실전 적용",
      "dateTime": "2024-11-15T14:00:00"
    },
    {
      "id": 3,
      "title": "Prometheus와 Grafana로 서비스 관측 가능성 확보하기",
      "speaker": "박민준",
      "description": "메트릭 수집부터 대시보드 구성까지",
      "dateTime": "2024-11-16T10:00:00"
    }
  ]
}
```

### 3.3 JWT 토큰 발급 (Gateway 경유 API 사용 전 필수)

Gateway를 경유하는 **POST/PUT/DELETE** 요청은 JWT 인증이 필요합니다.

```bash
curl -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "roles": ["ORGANIZER"]}'
```

**응답 예시:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInJvbGVzIjpbIk9SR0FOSVpFUiJdLCJpYXQiOjE3MDAwMDAwMDB9.xxxxxxxx",
  "expiresIn": 3600
}
```

`token` 값을 복사해서 이후 요청에 사용합니다.

> **역할 종류:** `ORGANIZER` (세션 생성/수정 가능), `ATTENDEE` (조회만 가능), `SPEAKER` (제안서 제출 가능)

### 3.4 세션 생성 (Gateway 경유, JWT 필요)

위에서 발급한 토큰을 `{토큰}` 자리에 붙여넣습니다:

```bash
curl -X POST http://localhost:8080/sessions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {토큰}" \
  -d '{
    "title": "새로운 세션",
    "speaker": "홍길동",
    "description": "세션 설명입니다",
    "dateTime": "2024-12-01T10:00:00"
  }'
```

**성공 응답 (201 Created):**
```json
{
  "id": 4,
  "title": "새로운 세션",
  "speaker": "홍길동",
  "description": "세션 설명입니다",
  "dateTime": "2024-12-01T10:00:00"
}
```
응답 헤더의 `Location: http://localhost:8080/sessions/4`에서 생성된 리소스 URL을 확인할 수 있습니다.

### 3.5 참석자 등록

Gateway 경유 없이 Attendee Service에 직접 요청합니다 (인증 불필요):

```bash
curl -X POST http://localhost:8081/attendees \
  -H "Content-Type: application/json" \
  -d '{
    "givenName": "길동",
    "surname": "홍",
    "email": "gildong.hong@example.com"
  }'
```

**성공 응답 (201 Created):**
```json
{
  "id": 1,
  "givenName": "길동",
  "surname": "홍",
  "email": "gildong.hong@example.com"
}
```

**참석자 목록 조회:**
```bash
curl http://localhost:8081/attendees | python3 -m json.tool
```

### 3.6 제안서 등록 및 투표

**제안서 등록 (먼저 참석자가 등록되어 있어야 합니다):**
```bash
curl -X POST http://localhost:8083/proposals \
  -H "Content-Type: application/json" \
  -d '{
    "title": "GraphQL로 API 게이트웨이 구축하기",
    "abstract": "GraphQL의 장점과 REST API와의 비교 분석, 실전 구현 방법을 다룹니다. 스키마 설계부터 N+1 문제 해결까지.",
    "speakerId": 1
  }'
```

**성공 응답 (201 Created):**
```json
{
  "id": 1,
  "title": "GraphQL로 API 게이트웨이 구축하기",
  "abstract": "GraphQL의 장점과 REST API와의 비교 분석...",
  "speakerId": 1,
  "status": "SUBMITTED",
  "sessionId": null
}
```

**제안서에 투표 (attendeeId: 참석자 ID, score: 1-5점):**
```bash
curl -X POST http://localhost:8083/proposals/1/votes \
  -H "Content-Type: application/json" \
  -d '{
    "attendeeId": 1,
    "score": 5
  }'
```

**투표 결과 조회:**
```bash
curl http://localhost:8083/proposals/1/votes | python3 -m json.tool
```

**응답 예시:**
```json
{
  "votes": [
    {
      "id": 1,
      "proposalId": 1,
      "attendeeId": 1,
      "score": 5
    }
  ],
  "averageScore": 5.0
}
```

### 3.7 API 버전 비교 (V1 vs V2)

Session Service는 두 가지 버전의 API를 제공합니다. Gateway를 통해 비교해 봅니다:

```bash
# V1 API - 기본 세션 정보
curl http://localhost:8080/v1/sessions | python3 -m json.tool

# V2 API - tags, capacity, registeredCount 추가
curl http://localhost:8080/v2/sessions | python3 -m json.tool
```

**V1 응답 (기본 필드만):**
```json
{
  "data": [
    {
      "id": 1,
      "title": "Kotlin과 Spring Boot로 마이크로서비스 구축하기",
      "speaker": "김철수",
      "description": "...",
      "dateTime": "2024-11-15T10:00:00"
    }
  ]
}
```

**V2 응답 (확장 필드 포함):**
```json
{
  "data": [
    {
      "id": 1,
      "title": "Kotlin과 Spring Boot로 마이크로서비스 구축하기",
      "speaker": "김철수",
      "description": "...",
      "dateTime": "2024-11-15T10:00:00",
      "tags": ["framework", "microservices"],
      "capacity": 100,
      "registeredCount": 0
    }
  ]
}
```

**핵심 차이점:** V2에 추가된 세 필드:
- `tags`: 세션 제목에서 자동 추출된 키워드 (api, testing, microservices, infrastructure, framework, general)
- `capacity`: 최대 수용 인원 (현재 100으로 고정)
- `registeredCount`: 등록된 참석자 수

### 3.8 에러 응답 체험 (RFC 7807 ProblemDetail)

이 시스템은 에러 응답에 RFC 7807 표준(ProblemDetail)을 사용합니다.

**존재하지 않는 세션 조회 (404):**
```bash
curl -v http://localhost:8080/sessions/999 2>&1 | grep -E "< HTTP|{|}"
```

**응답 예시:**
```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Session with id 999 not found",
  "instance": "/sessions/999"
}
```

**인증 없이 POST 요청 (401):**
```bash
curl -X POST http://localhost:8080/sessions \
  -H "Content-Type: application/json" \
  -d '{"title":"test","speaker":"test"}'
```

**응답 예시:**
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Authorization header is required",
  "instance": "/sessions"
}
```

**유효성 검사 실패 (400) - 필수 필드 누락:**
```bash
curl -X POST http://localhost:8082/sessions \
  -H "Content-Type: application/json" \
  -d '{"title":"제목만 있고 speaker 없음"}'
```

**응답 예시:**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "발표자는 필수입니다",
  "instance": "/sessions"
}
```

---

## 4. Grafana 사용법 (실시간 모니터링)

### 4.1 로그인

1. 브라우저에서 http://localhost:3000 접속
2. **Username:** `admin`
3. **Password:** `admin`
4. "Change password" 화면이 나오면 하단의 **"Skip"** 클릭

### 4.2 대시보드 찾기

1. 왼쪽 사이드바에서 네모 4개 아이콘(**Dashboards**) 클릭
2. 폴더 목록에서 **"Conference"** 폴더 또는 검색창에 `Conference` 입력
3. **"Conference Management System"** 클릭
4. 대시보드가 열리면 상단 오른쪽에서 시간 범위를 **"Last 5 minutes"** 또는 **"Last 15 minutes"**로 설정

> 또는 직접 URL로 접속: http://localhost:3000/d/conference-dashboard-v1

### 4.3 패널별 설명

대시보드는 다음 패널들로 구성됩니다:

**서비스 상태 패널**
- 4개 서비스(Gateway, Attendee, Session, CFP)의 UP/DOWN 상태 표시
- 초록색 `1` = UP, 빨간색 `0` = DOWN
- PromQL: `up{job=~"gateway|attendee-service|session-service|cfp-service"}`

**JVM 메모리 사용량**
- 각 서비스의 Heap 메모리 사용량 (MB 단위)
- 급격한 증가는 메모리 누수 가능성 신호
- PromQL: `jvm_memory_used_bytes{area="heap"} / 1024 / 1024`

**HTTP 요청 비율 (Request Rate)**
- 분당 HTTP 요청 수를 서비스별로 표시
- API 호출 후 15-30초 내에 그래프 변화 확인 가능

**HTTP 응답 시간 (P95 Latency)**
- 상위 95% 요청의 응답 시간 (ms 단위)
- 500ms 초과 시 성능 이슈 점검 필요

**HTTP 에러율**
- 4xx/5xx 응답 비율
- 0에 가까울수록 정상

**비즈니스 메트릭**
- `sessions_total`: 등록된 세션 수
- `attendees_total`: 등록된 참석자 수
- `proposals_total`: 제출된 제안서 수

### 4.4 트래픽 발생시켜 대시보드 변화 확인

다음 스크립트를 실행하여 20회 반복 API 호출을 발생시킵니다:

```bash
for i in $(seq 1 20); do
  curl -s http://localhost:8080/sessions > /dev/null
  curl -s http://localhost:8080/attendees > /dev/null
  curl -s http://localhost:8080/proposals > /dev/null
  curl -s http://localhost:8082/sessions > /dev/null
  sleep 0.2
done
echo "완료. Grafana에서 변화를 확인하세요."
```

**Grafana에서 확인 순서:**
1. 스크립트 실행 완료 후 약 15-30초 대기 (Prometheus 스크레이프 간격: 15초)
2. Grafana 대시보드 상단의 새로고침 버튼(원형 화살표) 클릭
3. "HTTP Request Rate" 패널에서 그래프 높이 변화 확인
4. 시간 범위가 "Last 5 minutes"여야 변화가 잘 보임

### 4.5 커스텀 PromQL 쿼리 실행 (Explore)

1. 왼쪽 사이드바에서 나침반 모양 아이콘(**Explore**) 클릭
2. 상단 드롭다운에서 데이터소스 **"Prometheus"** 선택
3. 쿼리 입력란에 아래 쿼리 중 하나를 입력하고 **"Run query"** 클릭

**유용한 PromQL 쿼리:**

```promql
# JVM 힙 메모리 (바이트 단위)
jvm_memory_used_bytes{area="heap"}

# 서비스별 JVM 메모리 (MB 단위)
jvm_memory_used_bytes{area="heap"} / 1024 / 1024

# 총 HTTP 요청 수 (서비스별)
http_server_requests_seconds_count

# 분당 HTTP 요청 비율
rate(http_server_requests_seconds_count[1m])

# 등록된 세션 수 (비즈니스 메트릭)
sessions_total

# 등록된 참석자 수
attendees_total

# 제출된 제안서 수
proposals_total
```

---

## 5. Prometheus 사용법 (메트릭 쿼리)

### 5.1 접속

브라우저에서 http://localhost:9090 접속합니다. (인증 불필요)

### 5.2 타겟 상태 확인

1. 상단 메뉴 **Status** 클릭
2. 드롭다운에서 **Targets** 클릭
3. 다음 4개 타겟이 모두 **UP** (초록) 상태여야 합니다:

| Job | Target | 메트릭 경로 |
|-----|--------|------------|
| gateway | host.docker.internal:8080 | /actuator/prometheus |
| attendee-service | host.docker.internal:8081 | /actuator/prometheus |
| session-service | host.docker.internal:8082 | /actuator/prometheus |
| cfp-service | host.docker.internal:8083 | /actuator/prometheus |

"Last scrape" 컬럼에서 마지막 수집 시간을 확인합니다. `15s` 미만이면 정상입니다.

### 5.3 메트릭 쿼리 실행

1. 메인 화면 상단의 **Expression** 입력란 클릭
2. 쿼리 입력
3. **"Execute"** 버튼 클릭
4. 하단에서 **Table** 탭 (현재 값) 또는 **Graph** 탭 (시계열) 선택

### 5.4 유용한 PromQL 쿼리 10가지

**1. 서비스 UP/DOWN 상태:**
```promql
up
```
`1` = 정상, `0` = 다운. `job` 레이블로 서비스 구분.

**2. JVM 힙 메모리 사용량:**
```promql
jvm_memory_used_bytes{area="heap"}
```
각 서비스의 힙 메모리 사용량 (바이트). 1GB = 1,073,741,824.

**3. 활성 스레드 수:**
```promql
jvm_threads_live_threads
```
서비스당 현재 실행 중인 Java 스레드 수.

**4. 총 HTTP 요청 수:**
```promql
http_server_requests_seconds_count
```
서비스 시작 이후 누적 요청 수. `uri`, `method`, `status` 레이블로 필터 가능.

**5. 5분간 HTTP 요청 비율:**
```promql
rate(http_server_requests_seconds_count[5m])
```
초당 요청 수. 5분 이동 평균. 트래픽 패턴 분석에 사용.

**6. 최대 응답 시간:**
```promql
http_server_requests_seconds_max
```
각 엔드포인트의 최대 응답 시간 (초 단위). 0.5 이하가 정상.

**7. 등록된 세션 수 (커스텀 비즈니스 메트릭):**
```promql
sessions_total
```
Session Service에서 현재 관리 중인 세션 수.

**8. 등록된 참석자 수:**
```promql
attendees_total
```
Attendee Service에서 현재 관리 중인 참석자 수.

**9. 제출된 제안서 수:**
```promql
proposals_total
```
CFP Service에서 현재 관리 중인 제안서 수.

**10. CPU 사용률:**
```promql
process_cpu_usage
```
각 서비스 JVM 프로세스의 CPU 사용률 (0.0~1.0). 0.8 이상이면 부하 높음.

**고급 쿼리 - 서비스별 에러율:**
```promql
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
/
rate(http_server_requests_seconds_count[5m])
```
5xx 에러 비율. 0에 가까울수록 정상.

**고급 쿼리 - P95 응답 시간 (서비스별):**
```promql
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket[5m])
)
```
상위 5% 느린 요청의 응답 시간.

---

## 6. Pact Broker 사용법 (계약 관리)

### 6.1 로그인

1. 브라우저에서 http://localhost:9292 접속
2. 인증 팝업에서:
   - **Username:** `pact`
   - **Password:** `pact`
3. 로그인 후 Pact Broker 메인 화면으로 이동

### 6.2 현재 상태 확인

처음 접속하면 "No pacts found"와 같은 빈 화면이 표시됩니다.
Consumer-Driven Contract Testing을 사용하려면 먼저 Pact 파일을 발행해야 합니다.

### 6.3 Pact 계약 발행하기

다음 명령들을 순서대로 실행합니다:

**1단계: Consumer 테스트 실행 (Pact JSON 생성)**
```bash
./gradlew :attendee-service:test :cfp-service:test
```
- Attendee Service는 Session Service의 Consumer
- CFP Service는 Session Service와 Attendee Service의 Consumer
- 테스트 실행 후 `build/pacts/` 디렉토리에 `.json` 파일들이 생성됩니다

**생성된 Pact 파일 확인:**
```bash
find . -name "*.json" -path "*/pacts/*" 2>/dev/null
```

**2단계: Pact 파일 수집**
```bash
./gradlew collectPacts
```

**3단계: Pact Broker에 발행**
```bash
./gradlew pactPublish
```

성공 시 출력:
```
Pact successfully published for cfp-service version 0.0.1.
Pact successfully published for attendee-service version 0.0.1.
```

### 6.4 Pact Broker UI 탐색

발행 후 http://localhost:9292 를 새로고침합니다.

**메인 페이지 - 서비스 관계 매트릭스:**
- 행(Row): Consumer (계약을 요구하는 서비스)
- 열(Column): Provider (계약을 이행하는 서비스)
- 셀의 버전 번호 클릭 시 계약 상세 확인 가능

**계약 상세 보기:**
- 매트릭스에서 버전 번호 클릭
- `GET /sessions` 같은 각 interaction 목록 확인
- 각 interaction의 요청/응답 예시 JSON 확인

**Matrix 페이지 - 검증 결과:**
- 상단 메뉴 **Matrix** 클릭
- Consumer 버전과 Provider 버전 간의 호환성 검증 결과 확인
- 초록 체크: 검증 통과, 빨간 X: 검증 실패

**Network 페이지 - 서비스 관계 그래프:**
- 상단 메뉴 **Network** 클릭
- 서비스 간 의존 관계를 시각적으로 확인

### 6.5 Provider 검증 실행

Consumer가 요청한 계약을 Provider가 실제로 이행하는지 검증합니다:

```bash
./gradlew :session-service:test -Dpact.verifier.publishResults=true
```

### 6.6 can-i-deploy 확인

특정 서비스 버전이 프로덕션에 배포 가능한지 확인합니다:

```bash
# CFP Service 0.0.1 버전이 배포 가능한지 확인
curl -u pact:pact \
  "http://localhost:9292/can-i-deploy?pacticipant=cfp-service&version=0.0.1&to=production"
```

**배포 가능 응답:**
```json
{
  "summary": {
    "deployable": true,
    "reason": "All verification results are published and successful"
  }
}
```

**배포 불가 응답:**
```json
{
  "summary": {
    "deployable": false,
    "reason": "One or more verifications have failed"
  }
}
```

---

## 7. Actuator 엔드포인트 직접 접근

Spring Boot Actuator를 통해 각 서비스의 내부 상태를 직접 조회할 수 있습니다.
포트 번호만 바꾸면 모든 서비스에 동일하게 적용됩니다 (8080, 8081, 8082, 8083).

### 7.1 헬스 체크

```bash
curl http://localhost:8082/actuator/health | python3 -m json.tool
```

**정상 응답:**
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### 7.2 Prometheus 메트릭 원시 데이터 확인

```bash
# 처음 50줄만 확인
curl http://localhost:8082/actuator/prometheus | head -50
```

출력 예시 (Prometheus exposition format):
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 1.2345678E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 2.3456789E7
...
# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",status="200",uri="/sessions",} 42.0
...
```

### 7.3 서비스 정보 조회

```bash
curl http://localhost:8082/actuator/info | python3 -m json.tool
```

### 7.4 사용 가능한 메트릭 목록

```bash
curl http://localhost:8082/actuator/metrics | python3 -m json.tool
```

특정 메트릭 상세 조회:
```bash
# sessions_total 메트릭 상세
curl "http://localhost:8082/actuator/metrics/sessions_total" | python3 -m json.tool

# JVM 힙 메모리 상세
curl "http://localhost:8082/actuator/metrics/jvm.memory.used?tag=area:heap" | python3 -m json.tool
```

### 7.5 4개 서비스 헬스 한번에 확인

```bash
for port in 8080 8081 8082 8083; do
  status=$(curl -s "http://localhost:$port/actuator/health" | python3 -c "import sys,json; print(json.load(sys.stdin).get('status','UNKNOWN'))" 2>/dev/null)
  echo "Port $port: $status"
done
```

**정상 출력:**
```
Port 8080: UP
Port 8081: UP
Port 8082: UP
Port 8083: UP
```

---

## 8. 성능 테스트 실행

### 8.1 Apache Bench를 이용한 기본 부하 테스트

Apache Bench(`ab`)는 macOS에 기본 설치되어 있습니다:

```bash
./performance-test/load-test.sh
```

스크립트는 기본값으로 **동시 10개 요청, 총 100개 요청**을 5개 엔드포인트에 순서대로 실행합니다:
- `GET /sessions`
- `GET /attendees`
- `GET /proposals`
- `GET /v1/sessions`
- `GET /v2/sessions`

**출력 예시:**
```
=== Conference API Load Test ===
Base URL: http://localhost:8080
Concurrent: 10
Total Requests: 100

--- Test 1: GET /api/sessions ---
Requests per second:    234.56 [#/sec] (mean)
Time per request:       42.631 [ms] (mean)
Failed requests:        0

--- Test 2: GET /api/attendees ---
Requests per second:    198.34 [#/sec] (mean)
Time per request:       50.421 [ms] (mean)
Failed requests:        0
```

**부하 강도 조정:**
```bash
# 동시 50개, 총 500개 요청
./performance-test/load-test.sh http://localhost:8080 50 500
```

### 8.2 k6를 이용한 고급 부하 테스트

k6는 단계적 부하 증가(ramp-up)를 지원합니다.

**k6 설치 (macOS):**
```bash
brew install k6
```

**테스트 실행:**
```bash
k6 run performance-test/k6-load-test.js
```

k6 테스트 시나리오 (자동 실행):
```
단계 1 (10s): 0 → 10 VU (Virtual Users) 증가
단계 2 (30s): 10 VU 유지 (정상 부하)
단계 3 (10s): 10 → 50 VU 급증 (스파이크)
단계 4 (10s): 50 → 10 VU 회복
단계 5 (10s): 10 → 0 VU 종료
```

**출력 예시:**
```
          /\      |‾‾| /‾‾/   /‾‾/
     /\  /  \     |  |/  /   /  /
    /  \/    \    |     (   /   ‾‾\
   /          \   |  |\  \ |  (‾)  |
  / __________ \  |__| \__\ \_____/ .io

  execution: local
     output: -
     script: performance-test/k6-load-test.js

  scenarios: (100.00%) 1 scenario, 50 max VUs, 1m20s max duration

running (1m10s), 00/50 VUs, 1234 complete and 0 interrupted iterations
default ✓ [==============================] 00/50 VUs  1m10s

     ✓ sessions status 200
     ✓ attendees status 200
     ✓ proposals status 200
     ✓ v2 sessions status 200

     checks.........................: 100.00%
     http_req_duration..............: avg=23.4ms  min=5.2ms   med=18.3ms  max=312ms   p(90)=48.7ms  p(95)=67.2ms
     http_req_failed................: 0.00%
     iterations.....................: 1234  17.62/s
```

**성능 임계값 (자동 검증):**
- `http_req_duration p(95) < 500ms`: 95% 요청이 500ms 이내
- `errors rate < 10%`: 에러율 10% 미만

**테스트 실행 중 Grafana 모니터링:**
k6 실행과 동시에 Grafana(http://localhost:3000)에서 실시간으로 다음을 확인합니다:
- HTTP Request Rate 패널: 급격한 요청 수 증가
- P95 Latency 패널: 스파이크 구간(단계 3)에서 응답 시간 변화
- 에러율 패널: 부하 증가 시 에러 발생 여부

---

## 9. 전체 시스템 종료

### 순서대로 종료합니다

**1단계: Java 서비스 종료**
```bash
./stop-all.sh
```

`.service-pids` 파일이 있으면 저장된 PID로 정확히 종료됩니다.
파일이 없으면 포트로 프로세스를 찾아 종료합니다.

**정상 출력:**
```
=== Stopping Conference Services ===
Stopping services with PIDs: 12345 12346 12347 12348
  Stopped PID: 12345
  Stopped PID: 12346
  Stopped PID: 12347
  Stopped PID: 12348

All services stopped.
Done.
```

**2단계: 모니터링 스택 종료 (Prometheus, Grafana)**
```bash
docker compose -f docker-compose.monitoring.yml down
```

**3단계: Pact Broker 종료 (pact-broker, postgres)**
```bash
docker compose down
```

**데이터 완전 삭제 (볼륨 포함):**
```bash
docker compose down -v
docker compose -f docker-compose.monitoring.yml down -v
```
> 주의: `-v` 옵션은 PostgreSQL과 Prometheus, Grafana 저장 데이터를 모두 삭제합니다.

**종료 확인:**
```bash
docker ps
lsof -i :8080,8081,8082,8083 2>/dev/null
```
모두 비어 있으면 완전히 종료된 것입니다.

---

## 10. 자주 하는 시나리오

### 시나리오 A: "서비스가 제대로 동작하는지 빠르게 확인하고 싶다"

```bash
# 1. 서비스 시작
./start-all.sh

# 2. API 동작 확인 (curl)
curl http://localhost:8082/sessions | python3 -m json.tool
curl http://localhost:8081/attendees | python3 -m json.tool
curl http://localhost:8083/proposals | python3 -m json.tool

# 3. Gateway 경유 확인
curl http://localhost:8080/sessions | python3 -m json.tool

# 4. Swagger UI에서 직접 조작
# 브라우저: http://localhost:8082/swagger-ui/index.html
```

### 시나리오 B: "API 변경이 기존 소비자 계약을 깨뜨리지 않는지 확인하고 싶다"

```bash
# 1. Consumer 테스트 실행 (계약 생성)
./gradlew :attendee-service:test :cfp-service:test

# 2. 테스트 결과 확인
# 성공: BUILD SUCCESSFUL
# 실패: 어떤 interaction이 맞지 않는지 상세 메시지 확인

# 3. Pact Broker에 발행
./gradlew pactPublish

# 4. 브라우저에서 확인
# http://localhost:9292 → 매트릭스 확인

# 5. Provider 측에서 검증
./gradlew :session-service:test

# 6. can-i-deploy 확인
curl -u pact:pact "http://localhost:9292/can-i-deploy?pacticipant=session-service&version=0.0.1&to=production"
```

### 시나리오 C: "부하 테스트 중 실시간으로 성능을 모니터링하고 싶다"

```bash
# 터미널 1: 서비스 시작
./start-all.sh && docker compose up -d && docker compose -f docker-compose.monitoring.yml up -d

# 터미널 2: k6 부하 테스트 실행
k6 run performance-test/k6-load-test.js

# 브라우저: Grafana 대시보드 오픈
# http://localhost:3000/d/conference-dashboard-v1
# 시간 범위: Last 5 minutes, 자동 새로고침: 10s
```

### 시나리오 D: "서비스 로그를 실시간으로 보고 싶다"

```bash
# 특정 서비스 로그 확인
tail -f /tmp/session-service.log
tail -f /tmp/attendee-service.log
tail -f /tmp/cfp-service.log
tail -f /tmp/gateway.log

# 4개 서비스 로그 동시 확인 (멀티플렉서 없이)
tail -f /tmp/gateway.log /tmp/session-service.log /tmp/attendee-service.log /tmp/cfp-service.log
```

---

## 11. 트러블슈팅 FAQ

### 문제: 서비스가 시작되지 않아요

**포트 충돌 확인:**
```bash
# 어떤 프로세스가 해당 포트를 사용 중인지 확인
lsof -i :8082
```

출력 예시:
```
COMMAND   PID       USER   FD   TYPE             DEVICE SIZE/OFF NODE NAME
java    12345 janghoesu  123u  IPv6 0x...      0t0  TCP *:8082 (LISTEN)
```

**해결:**
```bash
# PID로 강제 종료
kill -9 12345

# 또는 stop-all.sh로 정리 후 재시작
./stop-all.sh && sleep 2 && ./start-all.sh
```

### 문제: Grafana에 데이터가 안 보여요

**원인 1: Prometheus 타겟이 DOWN 상태**
```
확인: http://localhost:9090/targets
```
- Java 서비스가 실행 중인지 확인: `./stop-all.sh && ./start-all.sh`
- Docker의 `host.docker.internal`이 Java 서비스를 찾지 못하는 경우

**원인 2: 시간 범위 설정 문제**
- Grafana 우측 상단에서 시간 범위를 "Last 15 minutes"로 변경
- 자동 새로고침을 "10s"로 설정

**원인 3: 아직 메트릭이 수집되지 않음**
```bash
# Prometheus가 아직 수집 안 했을 수 있음 — 15초 대기
sleep 15 && curl http://localhost:9090/api/v1/query?query=up
```

### 문제: Pact Broker에 접속이 안 돼요

```bash
# Docker 컨테이너 상태 확인
docker ps | grep pact

# 컨테이너가 없으면 시작
docker compose up -d

# 로그 확인
docker compose logs pact-broker
docker compose logs postgres
```

### 문제: JWT 인증이 안 돼요 (401 Unauthorized)

```bash
# 새 토큰 발급
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "roles": ["ORGANIZER"]}' | \
  python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 토큰 확인
echo $TOKEN

# 토큰 사용
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/sessions
```

> 토큰 유효 시간은 3600초(1시간)입니다. 만료된 경우 재발급하세요.

### 문제: Gateway 라우팅이 안 돼요 (502 Bad Gateway)

Gateway(8080)는 요청을 각 백엔드 서비스로 프록시합니다. 502 에러는 백엔드 서비스가 내려갔을 때 발생합니다.

```bash
# 백엔드 서비스 상태 확인
curl http://localhost:8082/actuator/health  # Session
curl http://localhost:8081/actuator/health  # Attendee
curl http://localhost:8083/actuator/health  # CFP
```

DOWN 상태인 서비스를 재시작:
```bash
# Session Service만 재시작하는 예시
java -jar session-service/build/libs/*.jar --server.port=8082 \
  > /tmp/session-service.log 2>&1 &
```

### 문제: Gradle 빌드가 실패해요

```bash
# 상세 오류 로그 확인
./gradlew clean bootJar --stacktrace 2>&1 | tail -30

# Java 버전 확인 (반드시 21이어야 함)
java -version
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew bootJar
```

### 문제: Docker 컨테이너가 계속 재시작해요

```bash
# 상세 로그 확인
docker compose logs --tail=50 pact-broker
docker compose logs --tail=50 postgres

# 볼륨 데이터 손상 시 완전 초기화
docker compose down -v
docker compose up -d
```

---

> 세션: QUICKSTART_GUIDE.md 문서 작성
> 최근 작업:
> 1. Conference Management System 빠른 시작 가이드 작성 요청
> 2. (없음)
> 3. (없음)

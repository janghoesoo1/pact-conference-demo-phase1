# Pact Conference Demo

Mastering API Architecture 책의 컨퍼런스 시스템 사례를 Kotlin + Spring Boot 3 + Pact 프레임워크로 구현한 CDC(Consumer-Driven Contract) 테스트 데모 프로젝트입니다.

## 프로젝트 구조

```
pact-conference-demo/
├── common/                    # 공통 도메인 모델 (Attendee, Session, ApiResponse)
├── session-service/           # 세션 서비스 (Pact Provider) - port 8081
├── attendee-service/          # 참석자 서비스 (Pact Consumer) - port 8080
├── docker-compose.yml         # Pact Broker + PostgreSQL
├── build.gradle.kts           # 루트 빌드 설정
└── settings.gradle.kts        # 멀티모듈 설정
```

## 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 | Kotlin 1.9.25 |
| 프레임워크 | Spring Boot 3.3.6 |
| 계약 테스트 | Pact JVM 4.6.14 |
| 빌드 | Gradle Kotlin DSL |
| 인프라 | Docker Compose (Pact Broker) |

## 빠른 시작

### 사전 요구사항
- JDK 21+
- Docker & Docker Compose

### 1. Pact Broker 실행
```bash
docker compose up -d
# http://localhost:9292 에서 Pact Broker UI 확인 (ID: pact / PW: pact)
```

### 2. Consumer 테스트 실행 (Pact 파일 생성)
```bash
./gradlew :attendee-service:test
# build/pacts/ 에 Pact JSON 파일 생성됨
```

### 3. Provider 검증 테스트 실행
```bash
./gradlew :session-service:test
# Consumer가 정의한 계약을 Provider가 충족하는지 검증
```

### 4. 서비스 실행
```bash
# 터미널 1: Session Service (Provider)
./gradlew :session-service:bootRun

# 터미널 2: Attendee Service (Consumer)
./gradlew :attendee-service:bootRun
```

## API 엔드포인트

### Session Service (http://localhost:8081)
| Method | Path | 설명 |
|--------|------|------|
| GET | /sessions | 전체 세션 목록 |
| GET | /sessions/{id} | 세션 상세 |
| POST | /sessions | 세션 생성 |
| PUT | /sessions/{id} | 세션 수정 |
| DELETE | /sessions/{id} | 세션 삭제 |

### Attendee Service (http://localhost:8080)
| Method | Path | 설명 |
|--------|------|------|
| GET | /attendees | 전체 참석자 목록 |
| GET | /attendees/{id} | 참석자 상세 |
| GET | /attendees/{id}/sessions | 참석자의 세션 목록 (SessionService 호출) |
| POST | /attendees | 참석자 등록 |
| PUT | /attendees/{id} | 참석자 수정 |
| DELETE | /attendees/{id} | 참석자 삭제 |

## Pact 계약 테스트 흐름

```
Consumer(AttendeeService)        Pact Broker         Provider(SessionService)
        |                            |                        |
        | 1. 계약 정의 & 테스트       |                        |
        |    (Mock Server 사용)       |                        |
        |----- 2. 계약 게시 --------->|                        |
        |                            |<-- 3. 계약 가져오기 ----|
        |                            |                        |
        |                            |    4. 실제 API로 검증   |
        |                            |<-- 5. 결과 게시 --------|
        |--- 6. can-i-deploy? ------>|                        |
        |<---- 7. OK / NG -----------|                        |
```

## 참고자료

- [Mastering API Architecture](https://www.oreilly.com/library/view/mastering-api-architecture/9781492090625/) - James Gough, Daniel Bryant, Matthew Auburn
- [Pact 공식 문서](https://docs.pact.io/)
- [GitHub: masteringapi](https://github.com/masteringapi)

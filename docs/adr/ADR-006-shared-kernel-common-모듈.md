# ADR-006: Shared Kernel으로 common 모듈 사용

## 상태
승인 (Accepted)

## 컨텍스트
마이크로서비스 간 공유 코드(모델, 예외, 보안) 관리 방법을 결정해야 한다.
- 선택지: 중복 코드, Git Submodule, Maven/Gradle 공유 라이브러리, Mono-repo Shared Kernel

## 결정
Gradle 멀티 모듈 프로젝트의 `common` 모듈을 Shared Kernel으로 사용한다.
- 공유 도메인 모델: Session, Attendee, ApiResponse
- 공유 예외: ResourceNotFoundException, GlobalExceptionHandler
- 공유 보안: JwtUtil, UserContext, RoleCheckInterceptor

## 결과
### 긍정적
- 컴파일 타임 타입 안전성
- IDE 리팩토링 지원
- 버전 일관성 보장 (같은 빌드)

### 부정적
- 서비스 간 강결합 위험
- common 변경 시 전체 서비스 재빌드
- 독립 배포 제약 (Mono-repo 한정)

## 참고
- Mastering API Architecture, Ch1: 마이크로서비스 공유 전략
- DDD: Shared Kernel 패턴

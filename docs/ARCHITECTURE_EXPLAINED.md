# 아키텍처 패턴 상세 설명서

> 이 문서는 `ARCHITECTURE.md`의 보조 자료입니다.
> 각 아키텍처 패턴이 **왜** 선택되었는지, **어떻게** 동작하는지를 실제 구현 코드와 함께 상세히 설명합니다.

---

## 목차

1. [서론](#1-서론)
2. [API Gateway 패턴 상세](#2-api-gateway-패턴-상세)
3. [JWT 인증 상세](#3-jwt-인증-상세)
4. [Consumer-Driven Contract 테스트 상세](#4-consumer-driven-contract-cdc-테스트-상세)
5. [RFC 7807 Problem Detail 상세](#5-rfc-7807-problem-detail-상세)
6. [Branch by Abstraction 패턴 상세](#6-branch-by-abstraction-패턴-상세)
7. [Feature Flag 패턴 상세](#7-feature-flag-패턴-상세)
8. [DTO 분리 패턴 상세](#8-dto-분리-패턴-상세)
9. [테스트 피라미드 상세](#9-테스트-피라미드-상세)
10. [Observability 패턴 상세](#10-observability-패턴-상세)
11. [참고 문헌](#11-참고-문헌)

---

## 1. 서론

### 1.1 ARCHITECTURE.md와의 관계

`ARCHITECTURE.md`는 이 프로젝트의 전체 구조, 기술 스택, 라우팅 규칙, 엔드포인트 목록 등 **무엇(What)**을 다룹니다. 이 문서는 그 결정들의 **왜(Why)**와 **어떻게(How)**를 설명합니다.

예를 들어 `ARCHITECTURE.md`는 "Spring Cloud Gateway MVC를 사용한다"고 명시하지만, 이 문서는 다음 질문에 답합니다:

- 왜 Reactive Gateway가 아닌 MVC 기반인가?
- `ProxyExchange`는 내부에서 어떻게 동작하는가?
- `hop-by-hop` 헤더를 왜 제거하는가?

### 1.2 이 문서의 활용 방법

각 섹션은 독립적으로 읽을 수 있습니다. 특정 패턴에 대한 궁금증이 생겼을 때 해당 섹션을 참조하세요. 실제 구현 파일의 코드가 인용되어 있으므로, 코드를 읽다가 이 문서로 돌아오는 방식으로도 활용할 수 있습니다.

---

## 2. API Gateway 패턴 상세

### 2.1 왜 Gateway가 필요한가

마이크로서비스 아키텍처에서 클라이언트가 각 서비스를 직접 호출하면 여러 문제가 발생합니다.

**문제 1: 횡단 관심사(Cross-Cutting Concerns) 중복**

인증, CORS, Rate Limit, 로깅은 모든 서비스에서 동일하게 필요합니다. Gateway 없이 각 서비스에 구현하면 동일한 로직이 4개 서비스에 중복됩니다. 인증 정책 하나를 바꾸려면 4개 서비스를 모두 수정해야 합니다.

**문제 2: 서비스 위치 노출**

클라이언트가 `localhost:8081`, `localhost:8082`, `localhost:8083`을 직접 알아야 합니다. 서비스의 포트나 호스트가 바뀌면 모든 클라이언트를 수정해야 합니다.

**문제 3: 서비스 간 인터페이스 불일치**

서비스마다 에러 응답 형식, 인증 방식, CORS 헤더가 달라질 수 있습니다. Gateway는 이를 단일 진입점에서 표준화합니다.

```
클라이언트  →  Gateway (:8080)  →  attendee-service (:8081)
                                →  session-service  (:8082)
                                →  cfp-service      (:8083)
```

Gateway는 외부 세계에 대한 **단일 계약(Single Contract)** 역할을 합니다. 내부 서비스 구조가 바뀌어도 클라이언트는 영향받지 않습니다.

### 2.2 Spring Cloud Gateway MVC vs Gateway Reactive

Spring Cloud Gateway는 두 가지 변형이 있습니다.

| 항목 | Gateway MVC | Gateway Reactive |
|------|-------------|-----------------|
| 기반 | Servlet (Spring MVC) | Netty (WebFlux) |
| 프로그래밍 모델 | 동기/블로킹 | 비동기/논블로킹 |
| 라우팅 방식 | ProxyExchange | RouteLocator |
| 성능 | 스레드 풀 기반 | 이벤트 루프 기반 |
| 기존 MVC 통합 | 자연스러움 | 별도 컨텍스트 필요 |

이 프로젝트가 **Gateway MVC**를 선택한 이유는 다음과 같습니다.

첫째, 이 프로젝트의 모든 다운스트림 서비스(`attendee-service`, `session-service`, `cfp-service`)는 Spring MVC(서블릿) 기반입니다. Gateway도 같은 프로그래밍 모델을 사용하면 코드의 일관성이 높아집니다. 개발자가 Reactive 스트림(`Mono`, `Flux`)에 익숙하지 않아도 됩니다.

둘째, 이 프로젝트는 학습 목적의 데모이므로 처리량(throughput)보다 코드 가독성이 중요합니다. Gateway MVC의 코드는 일반적인 Spring Controller 코드와 매우 유사하여 이해하기 쉽습니다.

셋째, `HttpServletRequestWrapper`를 사용해 요청 헤더를 동적으로 주입하는 패턴(JWT 인증에서 사용)은 서블릿 환경에서만 동작합니다.

### 2.3 ProxyExchange 라우팅 패턴

`ProxyController.kt`는 YAML 기반 라우트 설정이 아닌 프로그래밍 방식으로 프록시를 구현합니다. 이 방식의 장점은 라우팅 로직을 코드로 표현할 수 있어 디버깅과 테스트가 쉽다는 점입니다.

```kotlin
// gateway/src/main/kotlin/com/conference/gateway/controller/ProxyController.kt

@RestController
class ProxyController {

    // 서비스 URL은 환경 변수나 application.yml로 외부화 (하드코딩 금지)
    @Value("\${services.session.url:http://localhost:8082}")
    private lateinit var sessionUrl: String

    @Value("\${services.attendee.url:http://localhost:8081}")
    private lateinit var attendeeUrl: String

    @Value("\${services.cfp.url:http://localhost:8083}")
    private lateinit var cfpUrl: String

    // @RequestMapping으로 와일드카드 패턴 매칭
    // /sessions/1, /sessions/1/reviews 등 모든 하위 경로를 처리
    @RequestMapping("/sessions/**")
    fun sessions(proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
        return proxyTo("$sessionUrl${proxy.path()}", proxy, request)
    }

    // ... 다른 라우트들
```

**`proxy.path()`란 무엇인가?**

`proxy.path()`는 요청된 전체 경로를 반환합니다. 예를 들어 클라이언트가 `/sessions/42`를 요청하면 `proxy.path()`는 `/sessions/42`를 반환합니다. 따라서 `$sessionUrl${proxy.path()}`는 `http://localhost:8082/sessions/42`가 됩니다. 경로 매핑 없이 그대로 전달하는 **투명한 프록시(Transparent Proxy)** 동작입니다.

**HTTP 메서드별 프록시 처리:**

```kotlin
private fun proxyTo(
    targetUri: String,
    proxy: ProxyExchange<ByteArray>,
    request: HttpServletRequest
): ResponseEntity<ByteArray> {
    val configured = proxy.uri(targetUri)  // 대상 URI 설정

    // 원본 요청의 HTTP 메서드를 그대로 전달
    val response = when (request.method.uppercase()) {
        "POST"    -> configured.post()
        "PUT"     -> configured.put()
        "DELETE"  -> configured.delete()
        "PATCH"   -> configured.patch()
        "HEAD"    -> configured.head()
        "OPTIONS" -> configured.options()
        else      -> configured.get()    // GET 포함 기타 메서드
    }
```

`proxy.uri(targetUri).get()` 등의 메서드는 내부적으로 Spring의 `RestTemplate`(또는 `RestClient`)을 사용하여 실제 HTTP 요청을 실행합니다. Gateway는 단순히 요청을 받아 다운스트림으로 전달하는 **중계자** 역할을 합니다.

**hop-by-hop 헤더 제거:**

```kotlin
private val hopByHopHeaders = setOf(
    "Transfer-Encoding", "Keep-Alive", "Connection",
    "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailer", "Upgrade"
)

// 응답 헤더에서 hop-by-hop 헤더를 제거한 후 클라이언트에게 전달
val cleanHeaders = org.springframework.http.HttpHeaders()
response.headers.forEach { (name, values) ->
    if (!hopByHopHeaders.contains(name)) {
        cleanHeaders[name] = values
    }
}
```

hop-by-hop 헤더는 RFC 2616에서 정의한 개념으로, 각 네트워크 hop에서만 유효하고 최종 수신자에게 전달되면 안 되는 헤더들입니다. 특히 `Transfer-Encoding: chunked`가 문제입니다. 다운스트림 서비스가 응답을 chunked encoding으로 보내면, Gateway가 이를 그대로 클라이언트에 전달할 때 이미 완성된 응답 바디와 충돌이 발생합니다. Gateway는 chunked 응답을 받아 완전한 바이트 배열로 조립한 뒤 전달하므로, `Transfer-Encoding` 헤더를 제거해야 클라이언트가 응답을 올바르게 파싱합니다.

### 2.4 필터 체인 동작 순서

```
클라이언트 요청
    │
    ▼
RateLimitConfig (Order: HIGHEST_PRECEDENCE)
    │  초당 요청 수 초과 시 429 반환
    ▼
JwtAuthFilter (Order: HIGHEST_PRECEDENCE + 1)
    │  JWT 검증 및 X-User-Id/X-User-Roles 헤더 주입
    ▼
LoggingFilter (Order: HIGHEST_PRECEDENCE + 2)
    │  요청/응답 메타데이터 기록
    ▼
ProxyController (Spring MVC DispatcherServlet)
    │  HTTP 메서드 분기 후 다운스트림 서비스로 전달
    ▼
다운스트림 서비스 응답
```

`@Order` 어노테이션의 숫자가 낮을수록 먼저 실행됩니다. `HIGHEST_PRECEDENCE`는 `Integer.MIN_VALUE`이므로 가장 먼저 실행됩니다. Rate Limit → JWT 인증 → 로깅 순서가 중요한 이유는, Rate Limit에 걸린 요청은 JWT 검증 비용을 낭비하지 않아야 하기 때문입니다.

### 2.5 이 패턴에서 배울 수 있는 것

- **단일 진입점의 가치**: 인증, Rate Limit, 로깅 같은 횡단 관심사를 Gateway 한 곳에 모으면 각 마이크로서비스가 비즈니스 로직에만 집중할 수 있다. 이 프로젝트에서 `JwtAuthFilter`, `RateLimitConfig`, `LoggingFilter`가 모두 `gateway` 모듈에만 존재하는 이유다.
- **Gateway MVC vs Reactive 선택 기준**: Spring Cloud Gateway는 WebFlux 기반 Reactive가 기본이지만, 이 프로젝트는 MVC 기반 `ProxyExchange`를 선택했다. 팀이 MVC에 익숙하거나 기존 서블릿 필터를 재사용해야 할 때는 MVC가 현실적이다. 높은 동시성이 필요하다면 Reactive를 고려하라.
- **hop-by-hop 헤더 문제 인식**: `Transfer-Encoding: chunked`를 그대로 전달하면 클라이언트 파싱 오류가 발생한다. `ProxyExchange`를 쓸 때 반드시 hop-by-hop 헤더를 제거하는 코드를 추가해야 한다 — 이 프로젝트의 `ProxyController`가 이를 명시적으로 처리한다.
- **Filter Chain ordering의 중요성**: 필터 실행 순서가 성능과 보안에 직결된다. Rate Limit을 JWT 검증보다 앞에 두면 불필요한 암호화 연산을 방지한다. `@Order` 값을 문서화하지 않으면 나중에 필터를 추가할 때 순서가 뒤섞이는 버그가 생기기 쉽다.
- **YAML 라우팅 vs 프로그래밍 라우팅**: 단순한 path 매핑은 `application.yml`의 `spring.cloud.gateway.routes`로 선언하는 것이 직관적이다. 요청 변환, 헤더 조작, 조건부 라우팅처럼 로직이 필요한 경우에는 이 프로젝트처럼 `ProxyController`로 프로그래밍 방식을 사용하라.

### 2.6 활용할 수 있는 사용 패턴

- **프록시 패턴 (Proxy Pattern)**: `ProxyController`처럼 `ProxyExchange<ByteArray>`로 다운스트림 서비스를 투명하게 중계. HTTP 메서드별 분기(`get()`, `post()`, `put()`, `delete()`)로 모든 REST 동작 지원
- **필터 체인 패턴 (Filter Chain)**: `OncePerRequestFilter` + `@Order`로 횡단 관심사를 독립 필터로 분리. Rate Limit → JWT 인증 → 로깅 순서를 `@Order` 값으로 제어
- **헤더 주입 패턴**: `HttpServletRequestWrapper`를 확장하여 `getHeader()`, `getHeaders()`, `getHeaderNames()`를 오버라이드. 인증 정보를 커스텀 헤더(`X-User-Id`, `X-User-Roles`)로 다운스트림에 전달
- **Hop-by-hop 헤더 필터링**: 프록시 응답에서 `Transfer-Encoding`, `Connection`, `Keep-Alive` 등 hop-by-hop 헤더를 제거하여 이중 인코딩 방지

```kotlin
// 재사용 가능한 프록시 패턴 템플릿
private val hopByHopHeaders = setOf("Transfer-Encoding", "Keep-Alive", "Connection")

private fun proxyTo(targetUri: String, proxy: ProxyExchange<ByteArray>, request: HttpServletRequest): ResponseEntity<ByteArray> {
    val response = when (request.method.uppercase()) {
        "POST" -> proxy.uri(targetUri).post()
        "PUT" -> proxy.uri(targetUri).put()
        "DELETE" -> proxy.uri(targetUri).delete()
        else -> proxy.uri(targetUri).get()
    }
    val cleanHeaders = HttpHeaders()
    response.headers.forEach { (name, values) ->
        if (!hopByHopHeaders.contains(name)) cleanHeaders[name] = values
    }
    return ResponseEntity.status(response.statusCode).headers(cleanHeaders).body(response.body)
}
```

### 2.7 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `gateway/src/.../controller/ProxyController.kt` | 라우팅 규칙 정의 | 새 서비스 추가 시 |
| `gateway/src/.../filter/JwtAuthFilter.kt` | JWT 검증 + 헤더 주입 | 인증 정책 변경 시 |
| `gateway/src/.../filter/LoggingFilter.kt` | 요청/응답 로깅 | 로그 형식 변경 시 |
| `gateway/src/.../config/RateLimitConfig.kt` | IP별 토큰 버킷 Rate Limit | 제한값 조정 시 |
| `gateway/src/main/resources/application.yml` | 서비스 URL 설정 | 서비스 호스트/포트 변경 시 |

**새 서비스 추가 시 3단계**:
1. `application.yml`에 서비스 URL 추가: `services.new-service.url: http://localhost:8084`
2. `ProxyController`에 `@Value`로 URL 주입 + `@RequestMapping("/new-path/**")` 메서드 추가
3. 필요시 `JwtAuthFilter.publicPaths`에 공개 경로 추가

---

## 3. JWT 인증 상세

### 3.1 HMAC-SHA256 선택 이유

JWT 서명에는 크게 두 가지 방식이 있습니다.

| 방식 | 알고리즘 | 키 구조 | 검증 방법 |
|------|---------|---------|---------|
| HMAC | HS256, HS512 | 단일 공유 시크릿 | 동일한 시크릿으로 서명 재계산 |
| RSA/ECDSA | RS256, ES256 | 공개키/개인키 쌍 | 공개키로 서명 검증 |

이 프로젝트가 **HMAC-SHA256(HS256)**을 선택한 이유는 다음과 같습니다.

첫째, 이 프로젝트는 **단일 조직 내 신뢰 환경**입니다. Gateway가 JWT를 발급하고, 다운스트림 서비스들은 Gateway를 신뢰합니다. 서비스들이 직접 JWT를 검증할 필요가 없으므로 공개키 배포 인프라가 불필요합니다.

둘째, 구현이 단순합니다. 하나의 시크릿만 관리하면 됩니다.

단, 이 방식은 **시크릿이 노출되면 모든 서비스가 위험**합니다. 프로덕션에서는 OAuth2/OIDC와 RSA 기반 토큰 검증이 권장됩니다.

```kotlin
// common/src/main/kotlin/com/conference/common/security/JwtUtil.kt

object JwtUtil {
    // 주의: 실제 프로덕션에서는 환경 변수에서 읽어야 합니다
    // 이 값은 학습 목적의 데모용입니다
    private const val SECRET = "conference-demo-secret-key-must-be-at-least-256-bits-long!!"

    // JJWT 라이브러리: 시크릿 문자열을 SecretKey 객체로 변환
    private val key: SecretKey = Keys.hmacShaKeyFor(SECRET.toByteArray())

    // 만료 시간 1시간 (3,600,000밀리초)
    private const val EXPIRATION_MS = 3600000L

    fun generateToken(userId: Int, roles: List<String>): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())     // JWT "sub" 클레임
            .claim("roles", roles)          // 커스텀 클레임: 역할 목록
            .issuedAt(now)                  // JWT "iat" 클레임
            .expiration(Date(now.time + EXPIRATION_MS))  // JWT "exp" 클레임
            .signWith(key)                  // HMAC-SHA256으로 서명
            .compact()                      // Base64URL 인코딩된 문자열 반환
    }
```

생성된 JWT의 구조:

```
eyJhbGciOiJIUzI1NiJ9                          ← Header: {"alg":"HS256"}
.
eyJzdWIiOiIxIiwicm9sZXMiOlsiT1JHQU5JWkVSIl0...  ← Payload: {"sub":"1","roles":["ORGANIZER"],...}
.
xK8m9p2QzRvL1nT3uY7wJ5cB0oDqWiA6fS4hX          ← Signature
```

### 3.2 JwtAuthFilter 동작 상세

`JwtAuthFilter`는 `OncePerRequestFilter`를 상속합니다. 이 추상 클래스는 하나의 HTTP 요청에 대해 필터가 정확히 한 번만 실행되도록 보장합니다(포워드나 디스패치에 의한 중복 실행 방지).

```kotlin
// gateway/src/main/kotlin/com/conference/gateway/filter/JwtAuthFilter.kt

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class JwtAuthFilter : OncePerRequestFilter() {

    // JWT 검증을 완전히 건너뛰는 공개 경로 목록
    private val publicPaths = listOf(
        "/actuator",      // Kubernetes health probe, Prometheus scraping
        "/swagger-ui",    // OpenAPI 문서 UI
        "/v3/api-docs",   // OpenAPI 스펙 JSON
        "/api/auth"       // 토큰 발급 엔드포인트 (이곳에 JWT가 없으므로 제외)
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 1단계: 공개 경로 확인 (JWT 검증 완전 우회)
        if (isPublicPath(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        // 2단계: GET 요청 면제 (데모 편의성 - 읽기는 누구나 가능)
        if (request.method == "GET") {
            filterChain.doFilter(request, response)
            return
        }

        // 3단계: Authorization 헤더 존재 여부 확인
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, 401, "Authorization header required for write operations")
            return
        }

        // 4단계: "Bearer " 접두사 제거 후 토큰 추출
        val token = authHeader.substring(7)  // "Bearer ".length == 7

        // 5단계: JJWT 라이브러리로 서명 검증 및 클레임 파싱
        val claims = JwtUtil.validateToken(token)
        if (claims == null) {
            sendError(response, 401, "Invalid or expired token")
            return
        }
```

**`HttpServletRequestWrapper`로 헤더 주입하는 패턴:**

```kotlin
        // 검증 통과 후: 원본 요청을 래핑하여 커스텀 헤더 주입
        val wrappedRequest = object : HttpServletRequestWrapper(request) {

            // getHeader(): 단일 헤더 값 조회 시 호출됨
            override fun getHeader(name: String): String? {
                return when (name) {
                    "X-User-Id"    -> claims.userId.toString()
                    "X-User-Roles" -> claims.roles.joinToString(",")
                    else           -> super.getHeader(name)  // 원본 헤더 위임
                }
            }

            // getHeaders(): 동일 이름 헤더 여러 개 조회 시 호출됨
            // ProxyExchange가 헤더를 읽을 때 이 메서드를 사용하므로 반드시 오버라이드 필요
            override fun getHeaders(name: String): java.util.Enumeration<String> {
                return when (name) {
                    "X-User-Id"    -> java.util.Collections.enumeration(listOf(claims.userId.toString()))
                    "X-User-Roles" -> java.util.Collections.enumeration(listOf(claims.roles.joinToString(",")))
                    else           -> super.getHeaders(name)
                }
            }

            // getHeaderNames(): 헤더 목록 조회 시 호출됨 (헤더 복사할 때 사용)
            override fun getHeaderNames(): java.util.Enumeration<String> {
                val names = super.getHeaderNames().toList().toMutableList()
                names.add("X-User-Id")
                names.add("X-User-Roles")
                return java.util.Collections.enumeration(names)
            }
        }
```

왜 `getHeaders()`도 오버라이드해야 하는가? `ProxyExchange`는 내부적으로 요청 헤더를 다운스트림으로 복사할 때 `getHeaderNames()`로 헤더 이름 목록을 얻고, 각 이름에 대해 `getHeaders()`를 호출합니다. `getHeader()`만 오버라이드하면 `ProxyExchange`는 여전히 원본 헤더에서 `X-User-Id`를 찾지 못해 NPE가 발생하거나 헤더가 누락됩니다.

이 패턴은 HTTP 요청 객체를 불변(immutable)으로 처리하면서도 헤더를 추가하는 표준적인 서블릿 패턴입니다. 실제로 `request.setHeader()`같은 메서드는 존재하지 않습니다. `HttpServletRequestWrapper`는 이 문제를 데코레이터(Decorator) 패턴으로 해결합니다.

### 3.3 서비스 간 신뢰 모델

```
클라이언트 → Gateway → 다운스트림 서비스
                ↓
    JWT 검증 (여기서만)
    X-User-Id: 42          ← 헤더로 전파
    X-User-Roles: ORGANIZER ← 헤더로 전파
                              ↓
                    다운스트림 서비스는 헤더를 그대로 신뢰
                    RoleCheckInterceptor: X-User-Roles 헤더 확인
```

이 모델에서 다운스트림 서비스들은 JWT를 직접 검증하지 않습니다. 대신 Gateway가 이미 검증한 사용자 정보를 헤더로 받아 신뢰합니다. 이는 **Perimeter Security** 모델입니다. 외부 경계(Gateway)에서 인증을 수행하고, 내부 서비스들은 경계 안에서 이미 신뢰된 요청만 받는다고 가정합니다.

**보안 한계**: Gateway를 우회하여 다운스트림 서비스에 직접 접근하면, 임의의 `X-User-Id`/`X-User-Roles` 헤더를 가진 요청이 통과됩니다. 이 문제는 Kubernetes Network Policy나 Istio mTLS로 서비스 간 직접 통신을 차단함으로써 해결합니다.

### 3.4 프로덕션 고려사항

이 프로젝트의 JWT 구현은 학습 목적이며, 실제 프로덕션에는 적합하지 않습니다.

| 항목 | 데모 구현 | 프로덕션 권장 |
|------|---------|------------|
| 시크릿 저장 | 코드에 하드코딩 | AWS Secrets Manager / Vault |
| 시크릿 로테이션 | 없음 | 주기적 자동 로테이션 |
| 토큰 발급 | Gateway 직접 발급 | OAuth2 Authorization Server (Keycloak, Auth0) |
| 토큰 검증 | HMAC 공유 시크릿 | OAuth2 Resource Server (공개키) |
| 토큰 취소 | 없음 (만료 대기) | Token Revocation List / Refresh Token 무효화 |

### 3.5 이 패턴에서 배울 수 있는 것

- **Stateless 인증의 트레이드오프**: JWT는 서버 세션이 필요 없어 수평 확장에 유리하지만, 토큰 발급 후 취소가 불가능하다는 단점이 있다. 이 프로젝트처럼 짧은 만료 시간(1시간)을 설정하거나, 프로덕션에서는 Redis 기반 Blocklist를 추가해야 한다.
- **대칭키(HMAC) vs 비대칭키(RSA/EC) 선택 기준**: 서명과 검증을 동일 서비스(또는 신뢰된 내부 서비스)가 수행할 때는 HMAC이 단순하다. 외부 IdP가 토큰을 발급하고 여러 서비스가 독립적으로 검증해야 한다면 RSA/EC가 필수다 — 공개키만 배포하면 되기 때문이다.
- **HttpServletRequestWrapper로 헤더 주입하는 패턴**: `JwtAuthFilter`가 검증한 사용자 정보를 다운스트림에 전달할 때 `MutableHttpServletRequest`로 요청 객체를 감싸 `X-User-Id`/`X-User-Roles` 헤더를 주입한다. 이 패턴은 서블릿 필터가 요청을 변환할 때 표준적으로 사용된다.
- **데모 코드와 프로덕션 코드의 차이 인식**: 이 프로젝트의 시크릿 하드코딩, Gateway 직접 토큰 발급은 학습용 단순화다. 실무에서는 Vault/Secrets Manager로 시크릿을 주입하고, Keycloak/Auth0 같은 전용 Authorization Server를 사용해야 한다.
- **Zero Trust 관점에서의 토큰 검증**: Perimeter Security 모델은 Gateway 우회 시 취약하다. 이 프로젝트도 주석으로 언급하듯, 다운스트림 서비스에 직접 접근 시 임의 헤더가 통과된다. Kubernetes Network Policy나 Istio mTLS로 서비스 간 직접 통신을 차단해야 완전한 Zero Trust가 된다.

### 3.6 활용할 수 있는 사용 패턴

- **토큰 발급-검증 분리 패턴**: `JwtUtil.generateToken()`으로 발급, `JwtUtil.validateToken()`으로 검증. Gateway에서 발급하고 각 서비스에서 검증하는 분산 인증 구조
- **역할 기반 접근 제어 (RBAC)**: `@RoleRequired("ORGANIZER")` 어노테이션 + `RoleCheckInterceptor`로 선언적 권한 검사. Controller 메서드에 어노테이션만 붙이면 자동으로 역할 확인
- **ThreadLocal 사용자 컨텍스트**: `UserContextHolder`(ThreadLocal 기반)로 요청 스코프의 사용자 정보를 비즈니스 레이어까지 전달. `afterCompletion()`에서 반드시 `clear()` 호출

```kotlin
// 역할 기반 접근 제어 사용 예시
@PostMapping
@RoleRequired("ORGANIZER")  // ORGANIZER 역할 필수
fun createSession(@Valid @RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
    val user = UserContextHolder.get()  // 현재 인증된 사용자
    // ...
}
```

### 3.7 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `common/src/.../security/JwtUtil.kt` | 토큰 생성/검증 (HMAC-SHA256) | 시크릿 변경, 만료시간 조정 시 |
| `common/src/.../security/UserContext.kt` | ThreadLocal 사용자 컨텍스트 | 사용자 속성 추가 시 |
| `common/src/.../security/RoleRequired.kt` | 역할 검사 어노테이션 | 새 역할 타입 추가 시 |
| `common/src/.../security/RoleCheckInterceptor.kt` | 인터셉터 (역할 검증 + UserContext 설정) | 인증 로직 변경 시 |
| `gateway/src/.../controller/AuthController.kt` | `/api/auth/token` 엔드포인트 | 토큰 요청/응답 형식 변경 시 |

**JWT 인증 흐름 활용**:
1. 토큰 발급: `POST /api/auth/token` with `{"userId": 1, "roles": ["ORGANIZER"]}`
2. 인증 요청: `Authorization: Bearer <token>` 헤더 포함
3. Gateway가 토큰 검증 → `X-User-Id`, `X-User-Roles` 헤더 주입
4. 다운스트림 서비스의 `RoleCheckInterceptor`가 헤더 읽고 권한 확인

---

## 4. Consumer-Driven Contract (CDC) 테스트 상세

### 4.1 왜 CDC 테스트가 필요한가

마이크로서비스 환경에서 서비스 간 API 호환성을 보장하는 방법은 크게 세 가지입니다.

**방법 1: E2E 테스트**

모든 서비스를 실제로 띄우고 전체 흐름을 테스트합니다. 확실하지만 느리고, 테스트 환경 구축 비용이 높으며, 실패 원인 파악이 어렵습니다.

**방법 2: 문서 기반 합의**

Provider가 API 문서를 작성하고, Consumer가 그 문서를 참고하여 개발합니다. 문서와 실제 구현이 달라지면 런타임에야 발견됩니다.

**방법 3: Consumer-Driven Contract 테스트**

Consumer가 자신이 실제로 사용하는 API 형태(계약)를 테스트로 정의합니다. Provider는 그 계약을 실제 서버로 검증합니다. 계약이 깨지면 빌드 단계에서 즉시 알 수 있습니다.

CDC 테스트의 핵심은 "Consumer가 계약을 정의한다"는 점입니다. Provider 입장에서 "이 API를 제공한다"가 아니라, Consumer 입장에서 "나는 이 API를 이런 방식으로 사용한다"가 계약의 기준입니다.

### 4.2 Pact 프레임워크 동작 원리

```
1. Consumer 테스트 실행
   ├── @Pact 메서드로 예상 interaction 정의
   ├── Pact MockServer 자동 기동 (랜덤 포트)
   ├── 실제 Client 코드로 MockServer 호출
   ├── MockServer가 요청이 계약과 일치하는지 검증
   └── build/pacts/CfpService-SessionService.json 생성

2. 계약 파일 공유
   ├── 로컬: collectPacts Gradle task로 Provider build/pacts/에 복사
   └── CI/CD: Pact Broker에 publish

3. Provider 테스트 실행
   ├── @PactFolder 또는 @PactBroker로 계약 파일 로드
   ├── @SpringBootTest로 실제 서버 기동
   ├── 각 interaction마다 @State 핸들러로 데이터 준비
   ├── 실제 서버에 HTTP 요청 전송
   └── 응답이 계약의 matchingRules와 일치하는지 검증
```

### 4.3 Consumer 테스트 코드 워크스루

```kotlin
// cfp-service/src/test/kotlin/com/conference/cfp/pact/SessionServiceConsumerPactTest.kt

// PactConsumerTestExt: JUnit 5 Extension으로 MockServer 생명주기 관리
@ExtendWith(PactConsumerTestExt::class)
// providerName: Provider 테스트의 @Provider("SessionService")와 일치해야 함
// pactVersion: V3 = JSON 형식 버전 (매칭 규칙 지원 수준)
@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)
class SessionServiceConsumerPactTest {

    // @Pact 메서드: 계약(Interaction) 하나를 정의
    // consumer: 이 계약의 소비자 이름 (Pact Broker에 등록될 이름)
    @Pact(consumer = "CfpService")
    fun getSessionPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            // .given(): Provider State 이름 - Provider 테스트의 @State와 반드시 일치
            // Provider는 이 상태를 @State 핸들러에서 설정해야 함
            .given("세션 ID 1이 존재함")

            // .uponReceiving(): Interaction 설명 (Pact Broker UI에 표시됨)
            .uponReceiving("세션 조회 요청")

            // 요청 조건: 정확히 이 요청이 왔을 때 아래 응답을 돌려줄 것
            .path("/sessions/1")
            .method("GET")
            .headers("Accept", "application/json")

            // 응답 정의
            .willRespondWith()
            .status(200)
            .headers(mapOf("Content-Type" to "application/json"))

            // PactDslJsonBody: 타입 매칭 (값이 아닌 타입으로 계약)
            // integerType("id", 1L): id 필드가 정수 타입이면 통과 (값 1은 예시)
            // stringType("title", "..."): title 필드가 문자열이면 통과
            .body(
                PactDslJsonBody()
                    .integerType("id", 1L)
                    .stringType("title", "gRPC로 마이크로서비스 구축하기")
                    .stringType("speaker", "장호")
            )
            .toPact()
    }
```

**왜 값이 아닌 타입으로 매칭하는가?**

`.stringValue("title", "gRPC로 마이크로서비스 구축하기")`라고 쓰면, Provider의 실제 데이터가 이 값과 정확히 일치해야 테스트가 통과합니다. 그러나 Provider의 시드 데이터가 바뀌거나 한국어 텍스트가 수정되면 테스트가 깨집니다.

`.stringType("title", "gRPC로 마이크로서비스 구축하기")`는 "title 필드가 문자열이기만 하면 된다"는 계약입니다. Consumer가 실제로 관심 있는 것은 title이 어떤 문자열이냐가 아니라, title 필드 자체가 존재하고 문자열 타입이라는 사실이기 때문입니다.

**계약 파일 구조:**

테스트 실행 후 `build/pacts/CfpService-SessionService.json`이 생성됩니다:

```json
{
  "consumer": { "name": "CfpService" },
  "provider": { "name": "SessionService" },
  "interactions": [
    {
      "description": "세션 조회 요청",
      "providerStates": [{ "name": "세션 ID 1이 존재함" }],
      "request": {
        "method": "GET",
        "path": "/sessions/1",
        "headers": { "Accept": "application/json" }
      },
      "response": {
        "status": 200,
        "headers": { "Content-Type": "application/json" },
        "body": { "id": 1, "title": "gRPC로 마이크로서비스 구축하기", "speaker": "장호" },
        "matchingRules": {
          "body": {
            "$.id":     { "matchers": [{ "match": "integer" }] },
            "$.title":  { "matchers": [{ "match": "type" }] },
            "$.speaker":{ "matchers": [{ "match": "type" }] }
          }
        }
      }
    }
  ]
}
```

이 JSON 파일이 Consumer와 Provider 사이의 공식 계약입니다.

### 4.4 Provider 테스트 코드 워크스루

```kotlin
// session-service/src/test/kotlin/com/conference/session/pact/SessionServiceProviderPactTest.kt

// @Provider: 이 서비스의 이름 (Consumer의 providerName과 일치해야 함)
@Provider("SessionService")

// @PactFolder: 로컬 개발 시 계약 파일 위치
// collectPacts task가 Consumer 빌드 결과물을 여기로 복사함
// CI/CD에서는 아래 @PactBroker로 교체:
// @PactBroker(url = "http://pact-broker:9292")
@PactFolder("build/pacts")

// @IgnoreNoPactsToVerify: build/pacts가 비어있을 때 테스트를 실패시키지 않음
// Consumer 테스트를 먼저 실행하지 않은 경우를 위한 안전장치
@IgnoreNoPactsToVerify

// 실제 서버를 기동하여 실제 HTTP 요청으로 검증
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionServiceProviderPactTest {

    @LocalServerPort
    private var port: Int = 0

    // 테스트 격리를 위해 SessionStore에 직접 접근
    @Autowired
    private lateinit var sessionStore: SessionStoreInterface

    @BeforeEach
    fun setUp(context: PactVerificationContext?) {
        // nullable: @IgnoreNoPactsToVerify 때문에 계약이 없으면 context가 null
        context?.target = HttpTestTarget("localhost", port)
    }

    // @TestTemplate: JUnit 5의 테스트 팩토리 패턴
    // 계약 파일의 interaction 수만큼 반복 실행됨
    // PactVerificationInvocationContextProvider가 각 interaction에 대한 context 생성
    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext?) {
        context?.verifyInteraction()  // 실제 서버에 요청 보내고 계약 검증
    }

    // @State: Consumer의 .given("세션 ID 1이 존재함")과 반드시 일치
    // 각 interaction 실행 전에 호출되어 데이터베이스/저장소 상태를 설정
    @State("세션 ID 1이 존재함")
    fun sessionWithId1Exists() {
        sessionStore.clear()  // 이전 테스트의 데이터를 깨끗이 제거 (테스트 격리)
        sessionStore.addSession(Session(
            title = "gRPC로 마이크로서비스 구축하기",
            speaker = "장호",
            // Consumer가 기대하는 데이터와 일치하는 시드 데이터
            description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
            dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
        ))
    }
```

**`PactVerificationContext?`가 nullable인 이유:**

`@IgnoreNoPactsToVerify`가 없다면 계약 파일이 없을 때 테스트 자체가 실패합니다. `@IgnoreNoPactsToVerify`를 사용하면 계약 파일이 없을 때 `PactVerificationContext`가 null로 주입됩니다. `context?.verifyInteraction()`의 `?.`(safe call)은 null 안전 처리입니다. 이를 통해 개발자가 Consumer 테스트를 실행하지 않고 Provider 테스트만 실행해도 테스트가 gracefully 통과합니다.

### 4.5 collectPacts Gradle Task

```
# 루트 build.gradle.kts에서 정의된 커스텀 Gradle task

# Consumer들이 생성한 pact 파일 위치:
attendee-service/build/pacts/AttendeeService-SessionService.json
cfp-service/build/pacts/CfpService-SessionService.json
cfp-service/build/pacts/CfpService-AttendeeService.json

# collectPacts 실행 후 Provider 디렉토리:
session-service/build/pacts/AttendeeService-SessionService.json  ← attendee에서 복사
session-service/build/pacts/CfpService-SessionService.json       ← cfp에서 복사
attendee-service/build/pacts/CfpService-AttendeeService.json     ← cfp에서 복사
```

로컬 개발 워크플로우:

```bash
# 1. Consumer 테스트 실행 → pact JSON 파일 생성
./gradlew :cfp-service:test :attendee-service:test

# 2. 계약 파일을 Provider의 build/pacts로 수집
./gradlew collectPacts

# 3. Provider 테스트 실행 → 실제 서버로 계약 검증
./gradlew :session-service:test
./gradlew :attendee-service:test
```

### 4.6 Pact Broker 워크플로우 (CI/CD)

```
Consumer CI Pipeline:
  Consumer 테스트 실행
    → pact JSON 생성
    → ./gradlew pactPublish → Pact Broker에 업로드

Provider CI Pipeline:
  @PactBroker 어노테이션으로 계약 다운로드
    → Provider 테스트 실행
    → 검증 결과를 Pact Broker에 게시

배포 게이트:
  can-i-deploy --pacticipant CfpService --to production
    → Pact Broker가 모든 계약 검증 통과 여부 확인
    → 통과 시 배포 허용 / 실패 시 차단
```

`can-i-deploy`는 Pact Broker의 핵심 기능입니다. "이 버전의 서비스를 특정 환경에 배포해도 계약이 깨지지 않는가?"를 자동으로 확인합니다.

### 4.7 이 패턴에서 배울 수 있는 것

- **통합 테스트 vs CDC 테스트의 근본적 차이**: E2E 통합 테스트는 "전체 흐름이 동작하는가"를 검증하지만, CDC는 "이 서비스가 소비자와 맺은 계약을 지키는가"를 검증한다. 이 프로젝트에서 `SessionServiceConsumerPactTest`는 실제 네트워크 없이 Mock 서버로 계약을 정의한다.
- **Consumer 주도의 계약 정의가 주는 자율성**: Provider가 API 스펙을 일방적으로 정의하는 대신, Consumer(`cfp-service`)가 자신이 실제로 필요한 필드만 계약으로 명시한다. Provider(`session-service`)는 소비되지 않는 필드를 자유롭게 추가/변경할 수 있다.
- **Pact Broker를 통한 배포 안전성 확보**: `can-i-deploy` 명령이 CI/CD 파이프라인의 배포 게이트 역할을 한다. 계약이 검증되지 않은 버전은 배포가 차단된다 — 이것이 마이크로서비스 독립 배포의 안전망이다.
- **계약과 스키마 테스트의 차이**: OpenAPI 스키마 검증은 "응답 형식이 맞는가"를 확인하지만, Pact는 "소비자가 실제 사용하는 필드와 값이 맞는가"까지 확인한다. Provider State를 통해 "세션이 존재할 때", "세션이 없을 때" 같은 시나리오별 검증이 가능하다.
- **Provider States가 해결하는 문제**: Consumer 테스트는 Mock 서버에 특정 상태를 기대한다. `"a session with id 1 exists"` 같은 Provider State가 없으면 Provider 테스트에서 어떤 데이터를 준비해야 할지 알 수 없다. `@State` 어노테이션으로 각 상태별 데이터 셋업 로직을 명확히 분리하라.

### 4.8 활용할 수 있는 사용 패턴

- **Consumer 테스트 패턴 (Given-When-Then)**: `@Pact` 메서드에서 계약 정의 → `@Test` + `@PactTestFor`에서 MockServer로 검증. Consumer가 필요로 하는 필드만 명시적으로 선언

```kotlin
// Consumer 계약 정의 템플릿
@Pact(consumer = "MyConsumer")
fun myPact(builder: PactDslWithProvider): RequestResponsePact {
    return builder
        .given("특정 상태")           // Provider State
        .uponReceiving("요청 설명")    // 상호작용 설명
        .path("/resource/1")          // 요청 경로
        .method("GET")
        .willRespondWith()
        .status(200)
        .body(PactDslJsonBody()
            .integerType("id", 1L)
            .stringType("name", "example"))
        .toPact()
}
```

- **Provider 검증 패턴**: `@Provider` + `@PactFolder` + `@State`로 각 상태별 데이터 셋업. `@TestTemplate`이 Pact 파일의 모든 상호작용을 자동으로 테스트 케이스로 생성
- **Pact Broker 워크플로우**: Consumer가 Pact 파일 게시 → Provider가 검증 → `can-i-deploy`로 배포 안전성 확인

### 4.9 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `cfp-service/src/test/.../pact/SessionServiceConsumerPactTest.kt` | Session API 계약 (Consumer 관점) | CFP가 Session API 사용 방식 변경 시 |
| `cfp-service/src/test/.../pact/AttendeeServiceConsumerPactTest.kt` | Attendee API 계약 (Consumer 관점) | CFP가 Attendee API 사용 방식 변경 시 |
| `session-service/src/test/.../pact/SessionServiceProviderPactTest.kt` | 계약 검증 (Provider 관점) | Session API 응답 구조 변경 시 |
| `cfp-service/src/.../client/SessionClient.kt` | 실제 HTTP 호출 클라이언트 | 테스트 대상 |
| `cfp-service/src/.../client/AttendeeClient.kt` | 실제 HTTP 호출 클라이언트 | 테스트 대상 |

**새 서비스 간 계약 추가 시 4단계**:
1. Consumer 서비스에 `@Pact` 메서드로 기대하는 API 계약 정의
2. Consumer 테스트 실행 → `build/pacts/` 디렉토리에 JSON 계약 파일 생성
3. Provider 서비스에 `@Provider` + `@PactFolder` 테스트 작성, `@State`로 데이터 셋업
4. Provider 테스트 실행 → 계약 검증 결과 확인

---

## 5. RFC 7807 Problem Detail 상세

### 5.1 ProblemDetail 스펙의 필요성

API 에러 응답 형식은 서비스마다 제각각인 경우가 많습니다:

```json
// 팀 A의 에러 응답
{ "error": "Not Found", "code": 404 }

// 팀 B의 에러 응답
{ "message": "Session not found", "status": "ERROR" }

// 팀 C의 에러 응답
{ "errorCode": "SESS_001", "description": "..." }
```

RFC 7807은 이를 표준화합니다. 클라이언트는 어떤 API에서든 동일한 방식으로 에러를 파싱할 수 있습니다:

```json
{
  "type":     "https://conference.example.com/errors/not-found",  // 에러 유형 URI (문서 링크)
  "title":    "Resource Not Found",                               // 사람이 읽는 제목
  "status":   404,                                                // HTTP 상태 코드 (중복이지만 명시)
  "detail":   "Session with id 999 not found",                   // 구체적인 에러 설명
  "instance": "/sessions/999"                                     // 에러가 발생한 URI
}
```

`type` 필드는 URI 형식이며, 이 URI는 실제로 접근 가능한 문서 페이지가 될 수 있습니다. 해당 페이지에 에러의 원인과 해결 방법이 설명되어 있다면 이상적입니다.

### 5.2 GlobalExceptionHandler 구현

```kotlin
// common/src/main/kotlin/com/conference/common/exception/GlobalExceptionHandler.kt

// @RestControllerAdvice: 모든 @RestController에서 던진 예외를 가로챔
// common 모듈에 정의되어 있으므로 모든 서비스가 공유
@RestControllerAdvice
class GlobalExceptionHandler {

    // ResourceNotFoundException → 404
    // 도메인 예외를 HTTP 상태 코드로 매핑하는 표준 패턴
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found").apply {
            title = "Resource Not Found"
            type = URI.create("https://conference.example.com/errors/not-found")
            instance = URI.create(request.requestURI)  // 어느 URI에서 에러가 났는지
            setProperty("error", ex.message ?: "Resource not found")  // 확장 필드
        }
    }

    // MethodArgumentNotValidException → 400 (Bean Validation @Valid 실패)
    // 필드별 에러 메시지를 수집하여 "errors" 확장 필드로 제공
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ProblemDetail {
        // bindingResult.fieldErrors: @NotBlank, @Size 등 어노테이션 위반 목록
        // { "title": "title은 필수입니다", "speaker": "speaker는 필수입니다" }
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid") }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed").apply {
            title = "Validation Error"
            type = URI.create("https://conference.example.com/errors/validation")
            instance = URI.create(request.requestURI)
            setProperty("errors", errors)  // RFC 7807 확장: 필드별 에러 목록
        }
    }
```

예를 들어 `POST /sessions`에 title 없이 요청하면:

```json
{
  "type":     "https://conference.example.com/errors/validation",
  "title":    "Validation Error",
  "status":   400,
  "detail":   "Validation failed",
  "instance": "/sessions",
  "errors": {
    "title":   "title은 필수입니다",
    "speaker": "speaker는 필수입니다"
  }
}
```

### 5.3 예외 클래스별 매핑

| 예외 클래스 | HTTP 상태 | type URI | 발생 상황 |
|------------|---------|----------|---------|
| `ResourceNotFoundException` | 404 | `.../not-found` | `store.getSession(999)` → null |
| `MethodArgumentNotValidException` | 400 | `.../validation` | `@Valid` 어노테이션 실패 |
| `ConstraintViolationException` | 400 | `.../validation` | `@Validated` + 메서드 파라미터 검증 실패 |
| `IllegalArgumentException` | 400 | `.../bad-request` | 비즈니스 로직 입력값 오류 |
| `UnauthorizedException` | 401 | `.../unauthorized` | 인증 토큰 없음/만료 |
| `ForbiddenException` | 403 | `.../forbidden` | 권한 부족 |

`GlobalExceptionHandler`는 `common` 모듈에 정의되어 있으므로 `attendee-service`, `session-service`, `cfp-service` 모두 동일한 에러 형식을 사용합니다. 이것이 **Shared Kernel** 패턴의 장점입니다.

### 5.4 이 패턴에서 배울 수 있는 것

- **표준 에러 형식이 팀 협업에 미치는 영향**: 팀마다 다른 에러 형식을 사용하면 클라이언트 개발자가 서비스별로 에러 파싱 로직을 따로 작성해야 한다. RFC 7807을 채택하면 모든 서비스의 에러를 동일한 방식으로 처리할 수 있어 프론트엔드 공통 에러 핸들러 구현이 가능해진다.
- **GlobalExceptionHandler의 Shared Kernel 패턴**: 이 프로젝트에서 `GlobalExceptionHandler`를 `common` 모듈에 두어 세 서비스가 공유한다. 에러 처리 로직 변경이 한 곳에서만 이루어지므로 일관성 유지가 쉽다. 단, `common` 모듈의 변경이 모든 서비스에 영향을 미치므로 하위 호환성을 신중히 관리해야 한다.
- **type URI로 에러 문서화 자동화**: `type` 필드에 `https://api.conference.dev/errors/not-found` 같은 URI를 사용하면, 해당 URL에 에러 설명 문서를 호스팅할 수 있다. 클라이언트는 `type` 값만으로 에러 종류를 구분하고 자동으로 문서를 참조할 수 있다.
- **클라이언트 측에서의 일관된 에러 처리**: `status` 코드만으로 에러를 구분하면 같은 400이라도 검증 오류인지 비즈니스 오류인지 알 수 없다. `type` URI와 `detail` 메시지를 조합하면 클라이언트가 에러 유형별로 다른 UI 처리(토스트 메시지, 필드 에러 표시 등)를 정확하게 적용할 수 있다.

### 5.5 활용할 수 있는 사용 패턴

- **@RestControllerAdvice + ProblemDetail**: Spring 6의 `ProblemDetail` 클래스로 RFC 7807 응답 자동 생성. 예외 클래스별 `@ExceptionHandler`로 매핑
- **커스텀 예외 → ProblemDetail 변환 패턴**: `ResourceNotFoundException` → 404, `UnauthorizedException` → 401 등 예외 타입별 HTTP 상태와 `type` URI 자동 매핑

```kotlin
// 재사용 가능한 예외 핸들러 템플릿
@ExceptionHandler(ResourceNotFoundException::class)
fun handleNotFound(ex: ResourceNotFoundException, request: HttpServletRequest): ProblemDetail {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Not found").apply {
        title = "Resource Not Found"
        type = URI.create("https://api.example.com/errors/not-found")
        instance = URI.create(request.requestURI)
    }
}
```

- **필터 레벨 RFC 7807**: Spring MVC 컨텍스트 밖(필터)에서도 `application/problem+json` Content-Type으로 직접 JSON 작성

### 5.6 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `common/src/.../exception/GlobalExceptionHandler.kt` | 전역 예외 → ProblemDetail 변환 | 새 예외 타입 추가 시 |
| `common/src/.../exception/Exceptions.kt` | 커스텀 예외 클래스 정의 | 새 비즈니스 예외 추가 시 |

**새 예외 타입 추가 시 2단계**:
1. `Exceptions.kt`에 새 예외 클래스 추가: `class ConflictException(message: String) : RuntimeException(message)`
2. `GlobalExceptionHandler`에 `@ExceptionHandler(ConflictException::class)` 메서드 추가, HTTP 409와 적절한 `type` URI 설정

---

## 6. Branch by Abstraction 패턴 상세

### 6.1 패턴 원리

Branch by Abstraction은 Martin Fowler가 제안한 기법으로, 코드를 크게 변경할 때 장기 브랜치를 만들지 않고 Trunk(main 브랜치)에서 직접 변경하는 방법입니다.

**핵심 아이디어**: 변경하려는 코드 위에 추상화 계층(Interface)을 먼저 도입하고, 두 구현이 공존하는 상태에서 점진적으로 전환합니다.

**문제 상황**: InMemory 저장소에서 JPA 저장소로 마이그레이션해야 한다.

**잘못된 접근**: `feature/jpa-migration` 브랜치를 만들어 수 주 동안 개발 → 장기 브랜치의 merge 충돌

**올바른 접근 (Branch by Abstraction)**:

```
Step 1: Interface 도입
  SessionController → SessionStore (직접 참조, 구체 클래스)
                  ↓
  SessionController → SessionStoreInterface (추상화)
                  ↓
  SessionStore implements SessionStoreInterface

Step 2: 새 구현체 개발 (기존 구현은 그대로)
  SessionController → SessionStoreInterface
                  ↙ (default profile) ↘ (jpa profile)
  SessionStore                    JpaSessionStore (신규)

Step 3: 검증 완료 후 기존 구현 제거
  SessionController → SessionStoreInterface
                  ↓ (jpa profile)
  JpaSessionStore
```

### 6.2 실제 구현 코드

```kotlin
// session-service/src/main/kotlin/com/conference/session/store/SessionStoreInterface.kt

interface SessionStoreInterface {
    fun getSessions(): List<Session>
    fun getSession(id: Int): Session?
    fun addSession(session: Session): Session
    fun updateSession(id: Int, session: Session): Session?
    fun removeSession(id: Int): Boolean
    fun clear()  // Provider Pact 테스트의 @State 핸들러를 위한 메서드
}
```

```kotlin
// session-service/src/main/kotlin/com/conference/session/store/SessionStore.kt

@Component
@Profile("default")  // spring.profiles.active=default (또는 설정 없음)
class SessionStore : SessionStoreInterface {

    private val counter = AtomicInteger(3)
    private val store: ConcurrentHashMap<Int, Session> = ConcurrentHashMap<Int, Session>().apply {
        // 애플리케이션 시작 시 샘플 데이터 초기화
        put(1, Session(id = 1, title = "gRPC로 마이크로서비스 구축하기", speaker = "장호", ...))
        put(2, Session(id = 2, title = "API 게이트웨이 패턴", speaker = "김현수", ...))
        put(3, Session(id = 3, title = "계약 테스트 실전", speaker = "이서연", ...))
    }

    override fun addSession(session: Session): Session {
        val id = counter.incrementAndGet()  // AtomicInteger: 스레드 안전한 ID 증가
        val newSession = session.copy(id = id)  // Kotlin data class의 copy: 불변성 유지
        store[id] = newSession
        return newSession
    }

    override fun clear() {
        store.clear()
        counter.set(0)  // Pact Provider 테스트에서 @State마다 초기화
    }
}
```

### 6.3 @Profile 기반 전환

`@Profile("default")`와 `@Profile("jpa")`는 Spring의 Bean 조건부 등록 메커니즘입니다. 동일한 인터페이스의 구현체가 여러 개 있을 때, 활성화된 프로필에 해당하는 Bean만 등록됩니다.

```yaml
# session-service/src/main/resources/application.yml
spring:
  profiles:
    active: default  # InMemory 사용

# JPA로 전환: 환경 변수 또는 설정 변경만으로 가능
# SPRING_PROFILES_ACTIVE=jpa java -jar session-service.jar
```

Kubernetes Deployment에서의 전환:

```yaml
# k8s/session-service-deployment.yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "jpa"  # "default"에서 "jpa"로 변경
```

`SessionController`는 `SessionStoreInterface`만 의존합니다. 어떤 구현체가 주입되는지 알지 못하며, 알 필요도 없습니다. 이것이 **의존성 역전 원칙(DIP)**의 실제 적용입니다.

### 6.4 왜 Feature Flag가 아닌 Profile인가

Profile과 Feature Flag는 비슷해 보이지만 전환의 성격이 다릅니다.

| 항목 | Profile 전환 | Feature Flag |
|------|------------|-------------|
| 전환 시점 | 배포/재시작 시 | 런타임 (재시작 불필요) |
| 영향 범위 | 전체 인스턴스 | 일부 요청/사용자 |
| 롤백 방법 | 이전 배포로 롤백 | 플래그 값 변경 |
| 사용 목적 | 인프라 교체 | A/B 테스트, 점진적 출시 |

저장소 교체는 인프라 수준의 변경입니다. 동일한 요청 처리 중에 두 저장소를 동시에 사용하는 것은 데이터 일관성 문제를 야기합니다. 따라서 배포 단위로 전환하는 Profile이 적합합니다.

반면 투표 알고리즘 변경은 런타임에 특정 사용자 그룹에게만 적용할 수 있으므로 Feature Flag가 적합합니다.

### 6.5 이 패턴에서 배울 수 있는 것

- **인터페이스 추상화로 무중단 교체가 가능한 이유**: `SessionStoreInterface`를 먼저 도입하고 `InMemorySessionStore`와 `JpaSessionStore`가 동시에 존재하는 상태를 만든다. `SessionController`는 인터페이스만 의존하므로 어떤 구현체가 주입되든 코드 변경이 없다. 이것이 의존성 역전 원칙(DIP)의 실제 효과다.
- **Spring Profile과 DI의 조합**: `@Profile("jpa")`가 붙은 빈은 해당 Profile이 활성화될 때만 Spring Context에 등록된다. 환경변수 `SPRING_PROFILES_ACTIVE=jpa` 하나로 인프라를 교체할 수 있어, Kubernetes 배포 매니페스트 수정만으로 운영 환경을 전환할 수 있다.
- **Strangler Fig 패턴과의 연관성**: Branch by Abstraction은 레거시 시스템을 점진적으로 교체하는 Strangler Fig 패턴의 핵심 도구다. 레거시(InMemory)와 신규(JPA)가 공존하며, 트래픽을 조금씩 신규 구현으로 이전하고 문제가 없으면 레거시를 제거한다.
- **리스크 없는 대규모 리팩토링 접근법**: 장기 브랜치를 만들면 main과의 충돌이 누적되어 merge 시 큰 리스크가 생긴다. Branch by Abstraction은 main에서 직접 작업하면서 두 구현이 항상 배포 가능한 상태를 유지하므로 CI/CD 파이프라인이 끊기지 않는다.
- **안티패턴 주의**: 추상화 인터페이스를 만든 후 오래된 구현체를 방치하면 기술 부채가 된다. 새 구현으로 완전히 전환한 후에는 레거시 구현체와 조건부 Profile 설정을 반드시 제거해야 한다.

### 6.6 활용할 수 있는 사용 패턴

- **인터페이스 + @Profile 전환 패턴**: 인터페이스를 정의하고 `@Profile("default")`와 `@Profile("jpa")` 구현체를 동시에 유지. 환경변수 하나(`SPRING_PROFILES_ACTIVE`)로 전환

```kotlin
// Branch by Abstraction 템플릿
interface DataStoreInterface {
    fun findAll(): List<Entity>
    fun findById(id: Int): Entity?
    fun save(entity: Entity): Entity
    fun delete(id: Int): Boolean
    fun clear()
}

@Component
@Profile("default")  // 메모리 저장소 (개발/테스트)
class InMemoryDataStore : DataStoreInterface { ... }

@Component
@Profile("jpa")  // DB 저장소 (프로덕션)
class JpaDataStore(private val repository: JpaRepository<...>) : DataStoreInterface { ... }
```

- **Domain ↔ Entity 변환 패턴**: JPA 구현에서 `toDomain()`, `fromDomain()` 메서드로 도메인 모델과 JPA 엔티티 간 변환. 도메인 모델이 JPA 어노테이션에 오염되지 않음
- **Testcontainers 통합 테스트**: `@ActiveProfiles("jpa")` + `PostgreSQLContainer`로 실제 DB 환경에서 JPA 저장소 검증

### 6.7 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `session-service/src/.../store/SessionStoreInterface.kt` | 저장소 계약 (인터페이스) | 새 CRUD 메서드 추가 시 |
| `session-service/src/.../store/SessionStore.kt` | InMemory 구현 (`@Profile("default")`) | 초기 데이터 변경 시 |
| `session-service/src/.../store/JpaSessionStore.kt` | JPA 구현 (`@Profile("jpa")`) | DB 스키마 변경 시 |
| `session-service/src/.../store/SessionEntity.kt` | JPA 엔티티 + 변환 메서드 | DB 컬럼 추가/변경 시 |
| `session-service/src/.../store/SessionJpaRepository.kt` | Spring Data JPA 인터페이스 | 커스텀 쿼리 추가 시 |

**저장소 전환 방법**:
- 메모리 → JPA: `SPRING_PROFILES_ACTIVE=jpa` 환경변수 설정 후 재시작
- JPA → 메모리: 환경변수 제거 또는 `default`로 설정 후 재시작
- Kubernetes: `deployment.yaml`의 `env` 섹션에서 Profile 변경

---

## 7. Feature Flag 패턴 상세

### 7.1 @ConfigurationProperties 바인딩

```kotlin
// cfp-service/src/main/kotlin/com/conference/cfp/config/FeatureFlags.kt

// @ConfigurationProperties: application.yml의 "features.*" 속성을 이 클래스에 바인딩
// prefix = "features" → features.new-voting-algorithm → newVotingAlgorithm (camelCase 자동 변환)
@ConfigurationProperties(prefix = "features")
data class FeatureFlags(
    val newVotingAlgorithm: Boolean = false,  // 기본값 false: 플래그 없으면 기존 동작
    val detailedLogging: Boolean = false
)
```

`application.yml` 설정:

```yaml
features:
  new-voting-algorithm: true   # YAML의 kebab-case → Kotlin의 camelCase로 자동 변환
  detailed-logging: false
```

`@ConfigurationProperties`는 단순한 `@Value("${features.newVotingAlgorithm}")`보다 강력합니다. 타입 안전성이 보장되고, 여러 관련 플래그를 하나의 클래스로 묶어 관리할 수 있습니다. `data class`이므로 테스트에서 쉽게 복사/수정할 수 있습니다.

### 7.2 가중 투표 알고리즘 분기

```kotlin
// cfp-service/src/main/kotlin/com/conference/cfp/controller/CfpController.kt

@GetMapping("/{id}/votes")
fun getVotes(@PathVariable id: Int): ResponseEntity<VoteSummaryResponse> {
    proposalStore.getProposal(id)  // 존재 여부 검증 (없으면 404)
    val votes = voteStore.getVotesByProposal(id).map { VoteResponse.from(it) }

    // Feature Flag로 알고리즘 분기
    val averageScore = if (featureFlags.newVotingAlgorithm) {
        // 신규 알고리즘: 최근 투표에 더 높은 가중치 부여
        // 또는 특정 참석자 등급에 따른 가중치 부여
        voteStore.getWeightedAverageScore(id)
    } else {
        // 기존 알고리즘: 단순 산술 평균
        voteStore.getAverageScore(id)
    }

    return ResponseEntity.ok(VoteSummaryResponse(votes = votes, averageScore = averageScore))
}
```

이 패턴의 장점은 두 알고리즘이 **동일한 코드베이스에 공존**한다는 점입니다. 알고리즘 전환이 배포 없이 설정 변경만으로 가능합니다. 문제가 발생하면 플래그를 `false`로 되돌려 즉시 롤백할 수 있습니다.

### 7.3 Feature Flag 확장 시나리오

이 프로젝트의 Feature Flag는 정적 설정 파일 기반입니다. 실제 프로덕션에서는 동적으로 관리됩니다.

**LaunchDarkly 전환 예시:**

```kotlin
// FeatureFlags를 인터페이스로 추상화
interface FeatureFlagService {
    fun isNewVotingAlgorithmEnabled(): Boolean
}

// 현재: @ConfigurationProperties 구현체
@Primary
class StaticFeatureFlagService(private val flags: FeatureFlags) : FeatureFlagService {
    override fun isNewVotingAlgorithmEnabled() = flags.newVotingAlgorithm
}

// 확장: LaunchDarkly 구현체
class LaunchDarklyFeatureFlagService(private val ldClient: LDClient) : FeatureFlagService {
    override fun isNewVotingAlgorithmEnabled() =
        ldClient.boolVariation("new-voting-algorithm", LDUser.Builder(userId).build(), false)
}
```

Branch by Abstraction과 동일한 패턴을 Feature Flag 서비스에도 적용할 수 있습니다. 인터페이스만 유지하면 구현체를 교체해도 비즈니스 로직(`CfpController`)은 변경이 없습니다.

### 7.4 이 패턴에서 배울 수 있는 것

- **런타임 전환 vs 배포 전환의 차이**: Feature Flag는 서비스 재시작 없이 런타임에 특정 사용자나 요청에 대해 새 동작을 활성화할 수 있다. 이 프로젝트에서 `newVotingAlgorithm` 플래그를 `true`로 바꾸면 배포 없이 즉시 새 알고리즘이 적용된다.
- **@ConfigurationProperties의 타입 안전성**: `application.yml`의 `features.new-voting-algorithm: true` 값이 Kotlin `Boolean` 타입으로 자동 바인딩된다. 문자열 파싱 오류나 오타로 인한 런타임 예외가 애플리케이션 시작 시점에 바로 잡힌다.
- **Feature Flag 서비스의 인터페이스 추상화**: 이 프로젝트의 `FeatureFlagService` 인터페이스를 통해 정적 설정 파일 기반 구현을 LaunchDarkly나 Unleash 같은 외부 서비스로 교체할 수 있다. 비즈니스 로직(`CfpController`)은 인터페이스만 의존하므로 전환 비용이 거의 없다.
- **기술 부채 관리 — 플래그 제거 시점**: Feature Flag는 임시 도구다. 새 알고리즘이 안정화되면 `newVotingAlgorithm` 플래그와 구 알고리즘 코드를 제거해야 한다. 플래그가 누적되면 조합 폭발(2^n 경우의 수)로 테스트가 불가능해진다. 플래그 생성 시 제거 일정을 함께 계획하라.
- **안티패턴 주의**: 플래그로 제어하는 코드가 복잡한 조건 분기 중첩으로 이어지면 오히려 가독성이 떨어진다. 플래그는 분기가 단순하고 명확한 경우에만 사용하고, 복잡한 변환은 Branch by Abstraction으로 처리하라.

### 7.5 활용할 수 있는 사용 패턴

- **@ConfigurationProperties 바인딩 패턴**: YAML의 `features.*` 값을 `FeatureFlags` data class로 타입 안전하게 바인딩. `@EnableConfigurationProperties`로 활성화

```kotlin
// Feature Flag 설정 템플릿
@ConfigurationProperties(prefix = "features")
data class FeatureFlags(
    val newVotingAlgorithm: Boolean = false,
    val detailedLogging: Boolean = false
)

// 사용 (Controller에서 주입받아 분기)
if (featureFlags.newVotingAlgorithm) {
    voteStore.getWeightedAverageScore(id)   // 새 알고리즘
} else {
    voteStore.getAverageScore(id)           // 기존 알고리즘
}
```

- **플래그별 테스트 패턴**: `@SpringBootTest(properties = ["features.new-voting-algorithm=true"])`로 플래그 활성화 상태를 테스트. 기본값 테스트와 활성화 테스트를 분리

### 7.6 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `cfp-service/src/.../config/FeatureFlags.kt` | 플래그 정의 (data class) | 새 플래그 추가 시 |
| `cfp-service/src/main/resources/application.yml` | 플래그 기본값 설정 | 플래그 ON/OFF 변경 시 |
| `cfp-service/src/.../controller/CfpController.kt` | 플래그 기반 분기 로직 | 분기 대상 비즈니스 로직 변경 시 |
| `cfp-service/src/test/.../config/FeatureFlagTest.kt` | 플래그 바인딩 검증 | 플래그 추가 시 |

**새 Feature Flag 추가 시 3단계**:
1. `FeatureFlags.kt`에 새 프로퍼티 추가: `val newSearchAlgorithm: Boolean = false`
2. `application.yml`에 기본값 설정: `features.new-search-algorithm: false`
3. Controller/Service에서 `featureFlags.newSearchAlgorithm`으로 분기

---

## 8. DTO 분리 패턴 상세

### 8.1 왜 엔티티를 직접 노출하면 안 되는가

JPA Entity나 도메인 모델을 API 응답으로 직접 사용하는 것은 여러 문제를 야기합니다.

**문제 1: 보안 노출**

Entity에는 `passwordHash`, `internalStatus` 같은 클라이언트에 노출되면 안 되는 필드가 있을 수 있습니다.

**문제 2: API 안정성 훼손**

데이터베이스 스키마 변경(컬럼 이름 변경, 컬럼 추가)이 즉시 API 응답 형식 변경으로 이어집니다. 클라이언트 코드가 깨집니다.

**문제 3: 역방향 의존성**

비즈니스 로직의 필요에 따라 Entity 구조를 변경하면 API가 바뀌고, API를 유지하려면 Entity를 변경할 수 없습니다. 두 관심사가 충돌합니다.

**문제 4: 순환 참조**

JPA Entity는 양방향 관계(예: `Session ↔ Speaker`)를 가질 수 있습니다. Jackson이 직렬화할 때 무한 재귀가 발생합니다.

### 8.2 Create/Update/Response DTO 3분법

```kotlin
// session-service/src/main/kotlin/com/conference/session/dto/SessionDto.kt

// 생성 요청: 클라이언트가 보내는 데이터
// id는 서버가 생성하므로 포함하지 않음
data class CreateSessionRequest(
    @field:NotBlank(message = "title은 필수입니다")   // Jakarta Validation
    val title: String,

    @field:NotBlank(message = "speaker는 필수입니다")
    val speaker: String,

    val description: String? = null,               // nullable: 선택 필드
    val dateTime: LocalDateTime? = null
) {
    // 도메인 모델로 변환하는 팩토리 메서드
    fun toDomain() = Session(title = title, speaker = speaker, description = description, dateTime = dateTime)
}

// 수정 요청: 수정 가능한 필드만 포함
// 일부 시스템에서는 PATCH를 위해 nullable 필드를 사용하기도 함
data class UpdateSessionRequest(
    @field:NotBlank val title: String,
    @field:NotBlank val speaker: String,
    val description: String?,
    val dateTime: LocalDateTime?
)

// 응답: 클라이언트에게 반환하는 데이터
// id를 포함 (생성 후 클라이언트가 리소스를 식별하기 위해)
data class SessionResponse(
    val id: Int,
    val title: String,
    val speaker: String,
    val description: String?,
    val dateTime: LocalDateTime?
) {
    companion object {
        // 도메인 모델에서 응답 DTO를 생성하는 정적 팩토리
        fun from(session: Session) = SessionResponse(
            id = session.id,
            title = session.title,
            speaker = session.speaker,
            description = session.description,
            dateTime = session.dateTime
        )
    }
}
```

**V2 확장 패턴:**

```kotlin
// V2Response: V1Response의 모든 필드 + 새 필드 추가
// 기존 V1 클라이언트에게 하위 호환성 유지
data class SessionV2Response(
    val id: Int,
    val title: String,
    val speaker: String,
    val description: String?,
    val dateTime: LocalDateTime?,
    val tags: List<String> = emptyList(),  // V2에 추가된 필드
    val capacity: Int? = null              // V2에 추가된 필드
)
```

### 8.3 @Valid + Jakarta Validation

`@Valid` 어노테이션은 Controller 메서드 파라미터에 붙어서 요청 바디를 자동으로 검증합니다.

```kotlin
@PostMapping
fun createSession(@Valid @RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
    // 이 시점에서 request는 이미 검증 통과
    // title과 speaker가 blank라면 MethodArgumentNotValidException 발생
    // → GlobalExceptionHandler가 400 ProblemDetail로 변환
```

Jakarta Validation은 Spring MVC의 데이터 바인딩 과정에서 자동으로 실행됩니다. 개발자가 null 체크나 빈 문자열 체크 코드를 작성할 필요가 없습니다. 검증 로직이 DTO 클래스에 선언적으로 표현됩니다.

### 8.4 이 패턴에서 배울 수 있는 것

- **도메인 모델과 API 계약의 분리가 주는 자유도**: `Session` 엔티티에 필드를 추가하거나 이름을 바꿔도 `SessionResponse` DTO가 이를 필터링하므로 API 계약이 깨지지 않는다. 이 프로젝트에서 DB 스키마 변경이 클라이언트 코드에 영향을 미치지 않는 이유다.
- **입력 DTO vs 출력 DTO의 목적 차이**: `CreateSessionRequest`는 클라이언트가 제공하는 데이터만 포함하고, `SessionResponse`는 서버가 계산하거나 추가한 데이터(id, createdAt 등)를 포함한다. 같은 DTO를 입출력에 재사용하면 보안 필드 노출이나 불필요한 필드 수신이라는 양방향 문제가 생긴다.
- **Jakarta Validation의 선언적 검증**: `@NotBlank`, `@Size`, `@Email` 같은 어노테이션으로 검증 규칙을 DTO에 선언하면 Controller 메서드에 null 체크 코드가 없어진다. `@Valid`와 `GlobalExceptionHandler`의 조합으로 검증 실패가 자동으로 RFC 7807 형식의 400 응답으로 변환된다.
- **API 버전 관리와 DTO의 관계**: 이 프로젝트의 `SessionV1Response`와 `SessionV2Response`처럼 버전별 DTO를 분리하면 V1 클라이언트는 기존 응답을 유지하면서 V2에 새 필드를 추가할 수 있다. DTO 없이 엔티티를 직접 노출하면 버전 관리가 불가능하다.
- **안티패턴 주의**: 모든 필드가 동일한 DTO를 요청/응답/DB 모두에 재사용하는 "만능 DTO"는 단기적으로 편리하지만 장기적으로 변경 비용이 폭발한다. 각 경계(입력/출력/저장)마다 독립된 타입을 유지하라.

### 8.5 활용할 수 있는 사용 패턴

- **입력 DTO + Jakarta Validation 패턴**: `@NotBlank`, `@Size`, `@Min`, `@Max` 어노테이션으로 선언적 검증. `@Valid` + `GlobalExceptionHandler`로 자동 400 응답

```kotlin
// 입력 DTO 템플릿
data class CreateEntityRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val name: String,

    @field:Size(max = 500, message = "설명은 500자 이하여야 합니다")
    val description: String? = null
) {
    fun toDomain() = Entity(name = name, description = description)
}
```

- **출력 DTO + 팩토리 메서드 패턴**: `companion object`의 `from()` 메서드로 Domain → DTO 변환. 도메인 모델의 내부 필드를 선택적으로 노출

```kotlin
// 출력 DTO 템플릿
data class EntityResponse(val id: Int, val name: String) {
    companion object {
        fun from(entity: Entity) = EntityResponse(id = entity.id!!, name = entity.name)
    }
}
```

- **API 버전별 DTO 분리 패턴**: 같은 도메인 모델에서 `V1Response`와 `V2Response`로 서로 다른 필드를 반환. V2에 `tags`, `capacity` 등 확장 필드 추가
- **ApiResponse<T> 래퍼 패턴**: 목록 응답에 `data`와 `total` 필드를 일관되게 포함

### 8.6 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `session-service/src/.../dto/SessionDto.kt` | 세션 입력/출력 DTO | 세션 필드 추가/변경 시 |
| `session-service/src/.../dto/SessionV2Dto.kt` | V2 확장 응답 DTO | V2 API 필드 변경 시 |
| `cfp-service/src/.../dto/CfpDto.kt` | 제안서/투표 입력/출력 DTO | CFP 필드 추가/변경 시 |
| `common/src/.../model/ApiResponse.kt` | 목록 응답 래퍼 | 페이지네이션 등 메타 정보 추가 시 |

**새 API 엔드포인트의 DTO 설계 순서**:
1. 입력 DTO 정의 + `@field:` Validation 어노테이션 + `toDomain()` 메서드
2. 출력 DTO 정의 + `companion object { fun from(domain) }` 팩토리 메서드
3. Controller에서 `@Valid @RequestBody request: CreateXRequest` / `XResponse.from(domain)` 사용
4. 버전 관리 필요 시 `V1Response`, `V2Response` 분리

---

## 9. 테스트 피라미드 상세

### 9.1 각 테스트 레벨의 목적

```
             /‾‾‾‾‾‾‾‾‾‾‾\
            /  E2E / 수동  \        비용 높음, 느림 → 최소화
           /_______________\
          /                 \
         /  Contract Tests   \      API 호환성 보장, 서비스 독립 배포
        /_____________________\
       /                       \
      /  Component Tests        \     서비스 전체 스택 검증 (HTTP부터 저장소까지)
     /___________________________\
    /                             \
   /  Integration Tests            \  외부 시스템(DB) 연동 검증
  /_________________________________\
 /                                   \
/    Unit Tests (가장 많음)             \  빠름, 격리됨, 비즈니스 로직 집중
\____________________________________/
```

**Unit Test**: 단일 클래스/함수를 격리하여 테스트합니다. `MockK`로 의존성을 Mock합니다. 실행 시간이 밀리초 단위입니다.

**Component Test**: 서비스의 HTTP 레이어부터 저장소까지 전체 스택을 테스트하지만, 외부 서비스는 Mock합니다. `@SpringBootTest` + REST-Assured를 사용합니다.

**Integration Test**: 실제 데이터베이스(Testcontainers로 기동한 PostgreSQL)와 연동하여 JPA 쿼리, 트랜잭션을 검증합니다.

**Contract Test**: 서비스 간 API 호환성을 검증합니다. 전체 시스템 없이 계약 파일만으로 독립적으로 실행됩니다.

### 9.2 이 프로젝트의 테스트 분포

| 클래스 | 레벨 | 주요 검증 내용 |
|--------|------|-------------|
| `SessionControllerTest` | Unit | Controller 메서드별 MockK 기반 비즈니스 로직 |
| `CfpControllerTest` | Unit | CfpController + Feature Flag 분기 |
| `FeatureFlagTest` | Unit | @ConfigurationProperties 바인딩 |
| `SessionApiComponentTest` | Component | GET/POST/PUT/DELETE 전체 HTTP 흐름 |
| `AttendeeApiComponentTest` | Component | attendee-service HTTP 흐름 |
| `CfpApiComponentTest` | Component | cfp-service HTTP 흐름 + 발표자 검증 |
| `SessionRepositoryIntegrationTest` | Integration | JPA + Testcontainers PostgreSQL |
| `SessionServiceConsumerPactTest` (cfp) | Contract | cfp → session API 계약 |
| `SessionServiceConsumerPactTest` (attendee) | Contract | attendee → session API 계약 |
| `AttendeeServiceConsumerPactTest` | Contract | cfp → attendee API 계약 |
| `SessionServiceProviderPactTest` | Contract | session Provider 검증 |
| `AttendeeServiceProviderPactTest` | Contract | attendee Provider 검증 |
| `RouteTest` | Component | Gateway 라우팅 규칙 |

### 9.3 @SpringBootTest vs @WebMvcTest

```kotlin
// Component Test: 전체 Spring Context 로드
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionApiComponentTest {
    // 실제 빈이 모두 주입됨: Controller, Store, Config 등
    // 장점: 실제 동작과 가장 유사
    // 단점: 시작 시간이 오래 걸림
}

// Unit Test: 웹 레이어만 로드 (Controller, Filter, Interceptor)
@WebMvcTest(SessionController::class)
class SessionControllerTest {
    // Store는 @MockkBean으로 대체
    @MockkBean lateinit var sessionStore: SessionStoreInterface
    // 장점: 빠른 시작, Controller 로직에 집중
    // 단점: 통합 설정 이슈 발견 불가
}
```

**언제 무엇을 쓰는가?**

Controller 메서드의 비즈니스 로직을 검증할 때는 `@WebMvcTest`가 적합합니다. 빠르고 의존성을 완전히 제어할 수 있습니다. 서비스의 전체 HTTP 흐름(요청 파싱, 검증, 응답 직렬화, 에러 처리)을 검증하고 싶다면 `@SpringBootTest`를 사용합니다.

### 9.4 MockK와 Spring MockK

Kotlin에서는 Mockito 대신 MockK를 권장합니다. Kotlin의 기본 클래스가 `final`이기 때문에 Mockito가 서브클래스 기반 Mock을 만들지 못할 수 있습니다. MockK는 Kotlin 언어 수준에서 동작합니다.

```kotlin
// MockK 기본 사용
val sessionStore = mockk<SessionStoreInterface>()
every { sessionStore.getSession(1) } returns Session(...)
verify { sessionStore.getSession(1) }

// SpringMockK: @SpringBootTest 컨텍스트에서의 MockK
@MockkBean
lateinit var sessionStore: SessionStoreInterface
// Spring의 @MockBean과 동일하지만 MockK로 Mock을 생성
```

### 9.5 이 패턴에서 배울 수 있는 것

- **각 레벨의 비용 대비 가치**: 단위 테스트는 빠르고 저렴하지만 통합 이슈를 발견하지 못한다. E2E 테스트는 확실하지만 느리고 환경 구성 비용이 높다. 피라미드 모양대로 단위 테스트를 많이, E2E를 최소화하는 전략이 실제 팀 속도를 높인다.
- **@WebMvcTest vs @SpringBootTest 선택 기준**: Controller 메서드의 로직과 요청 매핑, 검증을 빠르게 검증하려면 `@WebMvcTest`를 사용하라. HTTP부터 저장소까지 전체 스택의 통합을 검증해야 할 때는 `@SpringBootTest`를 사용하라. 이 프로젝트의 `SessionApiComponentTest`가 후자의 예다.
- **MockK가 Kotlin에서 필수인 이유**: Kotlin 클래스는 기본적으로 `final`이라 Mockito의 서브클래스 기반 Mock 생성이 실패한다. `mockk<>()`, `every {}`, `verify {}` 문법은 Kotlin 관용 표현과 자연스럽게 어울리며 suspend 함수 Mock도 지원한다.
- **Pact 테스트가 피라미드에서 차지하는 위치**: CDC 테스트는 단위 테스트보다 느리지만 E2E보다 훨씬 빠르다. 실제 서버를 띄우지 않으면서 서비스 간 계약을 검증하므로 통합 테스트와 E2E 테스트의 중간 레이어로 이해하면 된다.
- **안티패턴 주의**: 테스트를 빠르게 만들려고 `@SpringBootTest`를 남발하면 전체 테스트 시간이 폭발적으로 늘어난다. 새 테스트를 작성할 때마다 "이것이 단위 테스트인가, 통합 테스트인가?"를 먼저 분류하고 적절한 어노테이션을 선택하라.

### 9.6 활용할 수 있는 사용 패턴

- **@WebMvcTest + MockK 단위 테스트 패턴**: 웹 레이어만 로드하여 빠른 Controller 테스트. `@MockkBean`으로 의존성 Mock, MockMvc DSL로 요청/응답 검증

```kotlin
// 단위 테스트 템플릿
@WebMvcTest(MyController::class, GlobalExceptionHandler::class)
class MyControllerTest {
    @Autowired private lateinit var mockMvc: MockMvc
    @MockkBean private lateinit var myStore: MyStoreInterface

    @Test
    fun `GET - 200 OK`() {
        every { myStore.findAll() } returns listOf(sampleEntity)
        mockMvc.get("/my-path") { accept = MediaType.APPLICATION_JSON }
            .andExpect {
                status { isOk() }
                jsonPath("$.data[0].id") { value(1) }
            }
    }
}
```

- **@SpringBootTest + RestAssured 컴포넌트 테스트 패턴**: 전체 애플리케이션 컨텍스트 + 랜덤 포트로 실제 HTTP 통신 검증. Given-When-Then BDD 스타일

```kotlin
// 컴포넌트 테스트 템플릿
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MyApiComponentTest {
    @LocalServerPort private var port: Int = 0

    @BeforeEach
    fun setUp() { RestAssured.port = port }

    @Test
    fun `목록 조회 시 ApiResponse 형식으로 반환한다`() {
        Given { accept(ContentType.JSON) }
        When { get("/my-path") }
        Then { statusCode(200); body("data", hasSize<Any>(1)) }
    }
}
```

- **Testcontainers 통합 테스트 패턴**: `@Container` + `PostgreSQLContainer` + `@DynamicPropertySource`로 실제 DB 환경에서 JPA 저장소 검증

### 9.7 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `session-service/src/test/.../controller/SessionControllerTest.kt` | 단위 테스트 (@WebMvcTest) | Controller 로직 변경 시 |
| `session-service/src/test/.../component/SessionApiComponentTest.kt` | 컴포넌트 테스트 (@SpringBootTest) | API 통합 동작 변경 시 |
| `session-service/src/test/.../integration/SessionRepositoryIntegrationTest.kt` | JPA 통합 테스트 (Testcontainers) | DB 스키마 변경 시 |
| `session-service/src/test/.../controller/SessionVersioningTest.kt` | API 버전 테스트 | V1/V2 응답 구조 변경 시 |
| `cfp-service/src/test/.../config/FeatureFlagTest.kt` | Feature Flag 테스트 | 플래그 추가 시 |

**새 Controller 테스트 작성 순서**:
1. `@WebMvcTest`로 단위 테스트 작성 — Controller 로직과 Validation 검증
2. `@SpringBootTest`로 컴포넌트 테스트 작성 — 실제 HTTP 통신, 에러 핸들링 통합 검증
3. JPA 관련이면 Testcontainers로 통합 테스트 추가
4. CDC 계약이 있으면 Pact Consumer/Provider 테스트 추가

---

## 10. Observability 패턴 상세

### 10.1 왜 Observability가 필요한가

마이크로서비스는 분산 시스템입니다. 단일 요청이 여러 서비스를 거칩니다. 문제가 발생했을 때 어느 서비스에서 발생했는지, 얼마나 자주 발생하는지, 언제부터 발생했는지를 알아야 합니다. 이를 위해 각 서비스는 자신의 상태를 외부에서 관찰할 수 있도록 데이터를 노출해야 합니다.

**세 가지 Observability 기둥:**

- **Metrics**: 시계열 숫자 데이터 (요청 수, 응답 시간, 에러율)
- **Logs**: 이벤트의 텍스트 기록 (에러 스택 트레이스, 비즈니스 이벤트)
- **Traces**: 요청이 여러 서비스를 거치는 흐름 추적

이 프로젝트는 **Metrics**에 집중합니다(Micrometer + Prometheus + Grafana).

### 10.2 Micrometer Prometheus 메트릭 수집 표준

Micrometer는 메트릭 수집의 **Facade** 라이브러리입니다. Micrometer API로 코드를 작성하면, 백엔드(Prometheus, Datadog, CloudWatch 등)를 바꾸더라도 코드 변경이 없습니다. 이것도 **Bridge by Abstraction** 패턴의 일종입니다.

```
비즈니스 코드
     ↓
Micrometer API (MeterRegistry)
     ↓
Micrometer Prometheus (구현체)
     ↓
/actuator/prometheus (HTTP 엔드포인트)
     ↓
Prometheus (15초마다 scrape)
     ↓
Grafana (쿼리 및 시각화)
```

### 10.3 커스텀 비즈니스 메트릭

```kotlin
// session-service/config/MetricsConfig.kt

@Configuration
class MetricsConfig(meterRegistry: MeterRegistry, sessionStore: SessionStoreInterface) {
    init {
        // Gauge: 현재 값을 반환하는 측정값 (온도계처럼)
        // Counter와 달리 증감이 모두 가능
        Gauge.builder("conference.sessions.total") {
            // 람다: Prometheus가 scrape할 때마다 호출됨
            // 저장소에서 실시간으로 현재 세션 수를 읽음
            sessionStore.getSessions().size.toDouble()
        }
        .description("Total number of sessions")
        .register(meterRegistry)
    }
}
```

왜 비즈니스 메트릭이 중요한가? JVM 힙 사용량이나 GC 횟수는 기술적 지표입니다. "현재 컨퍼런스에 제안된 발표 수가 몇 개인가?"는 비즈니스 지표입니다. 비즈니스 지표는 운영팀과 비개발자도 이해하고 활용할 수 있습니다.

### 10.4 Spring Actuator 엔드포인트

각 서비스는 `/actuator/prometheus` 엔드포인트를 통해 메트릭을 노출합니다.

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

Prometheus는 이 엔드포인트를 주기적으로 **Pull**(당겨 가져가는 방식)합니다. Push 방식(서비스가 메트릭을 보냄)과 달리, Pull 방식은 Prometheus가 서비스의 상태를 통제합니다. 서비스가 다운되면 Prometheus가 scrape에 실패하고, 이 자체가 "서비스 다운" 신호가 됩니다.

```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 15s  # 모든 scrape_config의 기본 수집 주기

scrape_configs:
  - job_name: 'session-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8082']
        labels:
          service: 'session-service'  # Grafana에서 서비스별 필터링에 사용
```

`host.docker.internal`은 Docker 컨테이너(Prometheus)에서 호스트 머신(서비스)으로 접근하는 특수 DNS입니다. 로컬 개발 환경에서 서비스들은 호스트 네트워크에, Prometheus는 Docker 네트워크에 있으므로 이 주소가 필요합니다.

### 10.5 Grafana 대시보드 패널 구성

18개 패널은 크게 5개 카테고리로 나뉩니다:

**서비스 상태 (Status)**: `up` 메트릭(1=정상, 0=다운)을 Stat 패널로 표시. Kubernetes의 `readinessProbe`가 `/actuator/health`를 확인하는 것과 연계됩니다.

**비즈니스 메트릭**: `conference.sessions.total`, `conference.attendees.total`, `conference.proposals.total`, `conference.votes.total`을 Gauge 패널로 표시.

**HTTP 성능**: `http_server_requests_seconds_count`(요청 수), `http_server_requests_seconds_sum`(총 소요 시간)으로 평균 응답 시간과 RPS(Requests Per Second)를 계산.

**에러 추적**: HTTP 상태 코드별 요청 수. PromQL 예시:
```promql
sum by (status) (rate(http_server_requests_seconds_count{status=~"4.."}[5m]))
```

**JVM 상태**: `jvm_memory_used_bytes`, `jvm_gc_pause_seconds_count` 등 JVM 내부 상태.

### 10.6 이 패턴에서 배울 수 있는 것

- **Metrics, Logging, Tracing의 3대 축**: 이 프로젝트는 Metrics에 집중하지만, 실무에서는 세 가지가 모두 필요하다. Metrics로 "무엇이 이상한가"를 감지하고, Logs로 "왜 이상한가"를 파악하며, Traces로 "어느 서비스에서 시작됐는가"를 추적한다.
- **Micrometer + Prometheus + Grafana 스택의 연동 원리**: 서비스는 Micrometer API로 메트릭을 기록하고, `/actuator/prometheus` 엔드포인트로 노출한다. Prometheus가 15초마다 각 서비스를 스크레이핑하여 시계열 DB에 저장하고, Grafana가 PromQL로 조회하여 시각화한다. 각 계층이 독립적으로 교체 가능하다.
- **커스텀 비즈니스 메트릭의 가치**: JVM 메모리나 CPU 같은 기술 메트릭은 자동으로 수집되지만, "현재 등록된 세션 수", "오늘 제출된 발표 제안 수" 같은 비즈니스 메트릭은 개발자가 직접 `Gauge`나 `Counter`로 등록해야 한다. 이 프로젝트의 `MetricsConfig`가 `conference.sessions.total`을 등록하는 방식이 그 예다.
- **PromQL 기본 패턴 활용**: `rate(http_server_requests_seconds_count[5m])`은 5분 평균 RPS, `sum by (status)`는 상태 코드별 집계다. 이 두 패턴으로 서비스 부하와 에러율 대시보드의 80%를 구성할 수 있다.
- **안티패턴 주의**: 모든 것을 메트릭으로 만들면 Prometheus의 cardinality 문제가 발생한다. 특히 사용자 ID나 세션 ID 같은 고유값을 메트릭 레이블로 사용하면 시계열 수가 폭발하여 메모리 부족으로 이어진다. 레이블은 유한하고 낮은 카디널리티 값만 사용하라.

### 10.7 활용할 수 있는 사용 패턴

- **Gauge 메트릭 등록 패턴**: `Gauge.builder("metric.name") { supplier }` 로 현재값 스냅샷 메트릭 등록. Prometheus가 스크레이핑할 때마다 supplier 함수 실행

```kotlin
// 비즈니스 메트릭 등록 템플릿
@Configuration
class MetricsConfig(meterRegistry: MeterRegistry, dataStore: DataStoreInterface) {
    init {
        Gauge.builder("myapp.entities.total") { dataStore.findAll().size.toDouble() }
            .description("Total number of entities")
            .register(meterRegistry)
    }
}
```

- **Counter 메트릭 패턴**: 이벤트 발생 횟수를 누적 카운트. API 호출 수, 에러 발생 수 등에 적합

```kotlin
// Counter 사용 예시
val requestCounter = Counter.builder("myapp.requests.total")
    .tag("endpoint", "/sessions")
    .register(meterRegistry)
requestCounter.increment()
```

- **Actuator 엔드포인트 활용**: `/actuator/health`(헬스체크), `/actuator/metrics/{name}`(개별 메트릭), `/actuator/prometheus`(Prometheus 형식 전체 메트릭)

### 10.8 모듈 사용법

| 파일 | 역할 | 수정 시점 |
|------|------|----------|
| `session-service/src/.../config/MetricsConfig.kt` | 세션 메트릭 등록 | 새 비즈니스 메트릭 추가 시 |
| `cfp-service/src/.../config/MetricsConfig.kt` | 제안서/투표 메트릭 등록 | 새 비즈니스 메트릭 추가 시 |
| `*/src/main/resources/application.yml` | Actuator 엔드포인트 노출 설정 | 노출 범위 변경 시 |
| `monitoring/prometheus.yml` | Prometheus 스크레이핑 대상 설정 | 새 서비스 추가 시 |
| `monitoring/grafana/dashboards/*.json` | Grafana 대시보드 정의 | 시각화 패널 추가/변경 시 |

**새 비즈니스 메트릭 추가 시 3단계**:
1. 해당 서비스의 `MetricsConfig.kt`에 `Gauge.builder()` 또는 `Counter.builder()` 추가
2. `/actuator/prometheus` 에서 메트릭 노출 확인: `curl http://localhost:808x/actuator/prometheus | grep myapp`
3. Grafana 대시보드에 PromQL 쿼리로 패널 추가: `conference_sessions_total` → Stat 패널

---

## 11. 참고 문헌

### 이 프로젝트와 관련된 책의 챕터 매핑

| 챕터 | 주제 | 이 프로젝트의 구현 |
|------|------|----------------|
| Chapter 2 | API Gateway 패턴 | `gateway/ProxyController.kt`, `JwtAuthFilter.kt` |
| Chapter 4 | API 설계 (REST, OpenAPI) | `GlobalExceptionHandler.kt` (RFC 7807), DTO 패턴 |
| Chapter 5 | API 테스팅 전략 | 테스트 피라미드, `SessionApiComponentTest.kt` |
| Chapter 6 | Consumer-Driven Contract | `SessionServiceConsumerPactTest.kt`, `SessionServiceProviderPactTest.kt` |
| Chapter 7 | API 변경 관리 | `SessionV1Controller.kt`, `SessionV2Controller.kt` |
| Chapter 8 | 보안 | `JwtUtil.kt`, `RoleCheckInterceptor.kt` |
| Chapter 9 | 관찰가능성 | `MetricsConfig.kt`, `monitoring/prometheus.yml` |
| Chapter 11 | 클라우드 마이그레이션 | `SessionStoreInterface.kt` (Branch by Abstraction) |

### Martin Fowler 블로그

- [**StranglerFigApplication**](https://martinfowler.com/bliki/StranglerFigApplication.html): 레거시 시스템을 점진적으로 교체하는 패턴. 이 프로젝트의 Gateway가 Strangler Fig 패턴의 Facade 역할.

- [**BranchByAbstraction**](https://martinfowler.com/bliki/BranchByAbstraction.html): 장기 브랜치 없이 코드를 점진적으로 변경하는 기법. `SessionStore` → `JpaSessionStore` 마이그레이션의 이론적 근거.

- [**Feature Flag / Feature Toggle**](https://martinfowler.com/articles/feature-toggles.html): 런타임에 기능을 활성화/비활성화하는 패턴. `FeatureFlags.kt`의 이론적 근거.

### Pact 공식 문서

- [**Pact Documentation**](https://docs.pact.io): Consumer-Driven Contract 테스트 프레임워크의 공식 문서. Getting Started, Best Practices, Pact Broker 사용법 포함.
- [**Pact JVM**](https://github.com/pact-foundation/pact-jvm): 이 프로젝트에서 사용하는 JVM 구현체. Kotlin 지원 포함.
- [**can-i-deploy**](https://docs.pact.io/pact_broker/can_i_deploy): 배포 게이트로서의 계약 검증 도구.

### 표준 문서

- [**RFC 7807**](https://www.rfc-editor.org/rfc/rfc7807): Problem Details for HTTP APIs. `GlobalExceptionHandler.kt`에서 구현한 에러 응답 표준.
- [**RFC 7519**](https://www.rfc-editor.org/rfc/rfc7519): JSON Web Token (JWT). `JwtUtil.kt`의 토큰 구조 표준.
- [**RFC 2616 Section 13.5.1**](https://www.rfc-editor.org/rfc/rfc2616#section-13.5.1): hop-by-hop 헤더 정의. `ProxyController.kt`의 `hopByHopHeaders` 처리 근거.

### Spring 공식 문서

- [**Spring Cloud Gateway MVC**](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-mvc): ProxyExchange 기반 프로그래밍 방식 게이트웨이.
- [**Spring Security OAuth2**](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html): 프로덕션 수준의 인증 대안. 이 프로젝트의 자체 JWT 구현을 대체할 수 있음.
- [**Micrometer Documentation**](https://micrometer.io/docs): 메트릭 수집 Facade 라이브러리. Gauge, Counter, Timer 등 메트릭 타입 설명.

---

> 이 문서는 Phase 1~4 전체 구현을 기준으로 작성되었습니다.
> 각 패턴의 구체적인 코드는 해당 소스 파일을 직접 참조하십시오.
> 아키텍처 결정의 배경은 `docs/adr/` 디렉토리의 ADR 문서를 참조하십시오.

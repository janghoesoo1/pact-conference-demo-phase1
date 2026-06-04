package com.conference.session.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.conference.common.model.Session
import com.conference.session.store.SessionStoreInterface
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.LocalDateTime

// 로컬 개발: @PactFolder 사용 (attendee-service, cfp-service pacts를 build/pacts로 collectPacts task가 수집)
// CI/CD 환경: @PactFolder 대신 아래 사용
// @PactBroker(url = "http://localhost:9292", authentication = @PactBrokerAuth(username = "pact", password = "pact"))
@Provider("SessionService")
@PactFolder("build/pacts")
@IgnoreNoPactsToVerify
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionServiceProviderPactTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sessionStore: SessionStoreInterface

    @BeforeEach
    fun setUp(context: PactVerificationContext) {
        context.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @State("세션 ID 1이 존재함")
    fun sessionWithId1Exists() {
        sessionStore.clear()
        sessionStore.addSession(Session(
            title = "gRPC로 마이크로서비스 구축하기",
            speaker = "장호",
            description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
            dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
        ))
    }

    @State("세션 목록이 존재함")
    fun sessionsExist() {
        sessionStore.clear()
        sessionStore.addSession(Session(
            title = "gRPC로 마이크로서비스 구축하기",
            speaker = "장호",
            description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
            dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
        ))
        sessionStore.addSession(Session(
            title = "API 게이트웨이 패턴",
            speaker = "김현수",
            description = "API 게이트웨이의 다양한 패턴과 구현 전략을 살펴봅니다.",
            dateTime = LocalDateTime.of(2024, 9, 15, 14, 0)
        ))
        sessionStore.addSession(Session(
            title = "계약 테스트 실전",
            speaker = "이서연",
            description = "Pact 프레임워크를 사용한 CDC 테스트 실전 사례를 공유합니다.",
            dateTime = LocalDateTime.of(2024, 9, 16, 10, 0)
        ))
    }

    @State("세션 ID 2가 설명과 일시 없이 존재함")
    fun sessionWithId2WithNullFields() {
        sessionStore.clear()
        sessionStore.addSession(Session(
            title = "Placeholder",
            speaker = "Placeholder"
        ))
        sessionStore.addSession(Session(
            title = "API 게이트웨이 패턴",
            speaker = "김현수",
            description = null,
            dateTime = null
        ))
    }

    @State("세션 ID 999가 존재하지 않음")
    fun sessionWithId999DoesNotExist() {
        sessionStore.clear()
    }
}

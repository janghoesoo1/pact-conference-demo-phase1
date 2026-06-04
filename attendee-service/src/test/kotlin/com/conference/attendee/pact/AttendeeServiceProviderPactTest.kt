package com.conference.attendee.pact

import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import com.conference.attendee.store.AttendeeStore
import com.conference.common.model.Attendee
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

// 로컬 개발: @PactFolder 사용
// CI/CD 환경: @PactFolder 대신 아래 사용
// @PactBroker(url = "http://localhost:9292", authentication = @PactBrokerAuth(username = "pact", password = "pact"))
@Provider("AttendeeService")
@PactFolder("../cfp-service/build/pacts")
@IgnoreNoPactsToVerify
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttendeeServiceProviderPactTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var attendeeStore: AttendeeStore

    @BeforeEach
    fun setUp(context: PactVerificationContext?) {
        context?.target = HttpTestTarget("localhost", port)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext?) {
        context?.verifyInteraction()
    }

    @State("참석자 ID 1이 존재함")
    fun attendeeWithId1Exists() {
        attendeeStore.clear()
        attendeeStore.addAttendee(Attendee(
            givenName = "Jim",
            surname = "Gough",
            email = "jim@conference.com"
        ))
    }

    @State("참석자 ID 999가 존재하지 않음")
    fun attendeeWithId999DoesNotExist() {
        attendeeStore.clear()
    }
}

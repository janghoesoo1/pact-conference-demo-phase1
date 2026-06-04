package com.conference.cfp.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import com.conference.cfp.client.AttendeeClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "AttendeeService", pactVersion = PactSpecVersion.V3)
class AttendeeServiceConsumerPactTest {

    @Pact(consumer = "CfpService")
    fun getAttendeePact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("참석자 ID 1이 존재함")
            .uponReceiving("참석자 조회 요청")
            .path("/attendees/1")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(200)
            .headers(mapOf("Content-Type" to "application/json"))
            .body(
                PactDslJsonBody()
                    .integerType("id", 1L)
                    .stringType("givenName", "Jim")
                    .stringType("surname", "Gough")
                    .stringType("email", "gough@mail.com")
            )
            .toPact()
    }

    @Pact(consumer = "CfpService")
    fun getAttendeeNotFoundPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("참석자 ID 999가 존재하지 않음")
            .uponReceiving("존재하지 않는 참석자 조회")
            .path("/attendees/999")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(404)
            .toPact()
    }

    @Test
    @PactTestFor(pactMethod = "getAttendeePact")
    fun `참석자 단건 조회 시 올바른 참석자 정보를 반환한다`(mockServer: MockServer) {
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = AttendeeClient(restClient)

        val attendee = client.getAttendee(1)

        assertThat(attendee).isNotNull
        assertThat(attendee!!.id).isEqualTo(1)
        assertThat(attendee.givenName).isEqualTo("Jim")
        assertThat(attendee.surname).isEqualTo("Gough")
        assertThat(attendee.email).isEqualTo("gough@mail.com")
    }

    @Test
    @PactTestFor(pactMethod = "getAttendeeNotFoundPact")
    fun `존재하지 않는 참석자 조회 시 null을 반환한다`(mockServer: MockServer) {
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = AttendeeClient(restClient)

        val attendee = client.getAttendee(999)

        assertThat(attendee).isNull()
    }
}

package com.conference.attendee.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import com.conference.attendee.client.SessionClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)
class SessionServiceConsumerPactTest {

    @Pact(consumer = "AttendeeService")
    fun getSessionPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("세션 ID 1이 존재함")
            .uponReceiving("세션 1 조회 요청")
            .path("/sessions/1")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(200)
            .headers(mapOf("Content-Type" to "application/json"))
            .body(
                PactDslJsonBody()
                    .integerType("id", 1L)
                    .stringType("title", "gRPC로 마이크로서비스 구축하기")
                    .stringType("speaker", "장호")
                    .stringType("description", "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.")
                    .stringType("dateTime", "2024-09-15T10:00:00")
            )
            .toPact()
    }

    @Pact(consumer = "AttendeeService")
    fun getSessionsPact(builder: PactDslWithProvider): RequestResponsePact {
        val responseBody = PactDslJsonBody()
            .integerType("total")
            .eachLike("data", 3)
                .integerType("id")
                .stringType("title", "gRPC로 마이크로서비스 구축하기")
                .stringType("speaker", "장호")
                .closeArray()!! as PactDslJsonBody

        return builder
            .given("세션 목록이 존재함")
            .uponReceiving("전체 세션 목록 조회 요청")
            .path("/sessions")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(200)
            .headers(mapOf("Content-Type" to "application/json"))
            .body(responseBody)
            .toPact()
    }

    @Pact(consumer = "AttendeeService")
    fun getSessionNotFoundPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("세션 ID 999가 존재하지 않음")
            .uponReceiving("존재하지 않는 세션 조회 요청")
            .path("/sessions/999")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(404)
            .toPact()
    }

    @Pact(consumer = "AttendeeService")
    fun getSessionWithNullFieldsPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("세션 ID 2가 설명과 일시 없이 존재함")
            .uponReceiving("nullable 필드가 있는 세션 조회 요청")
            .path("/sessions/2")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(200)
            .headers(mapOf("Content-Type" to "application/json"))
            .body(
                PactDslJsonBody()
                    .integerType("id", 2L)
                    .stringType("title", "API 게이트웨이 패턴")
                    .stringType("speaker", "김현수")
                    .nullValue("description")
                    .nullValue("dateTime")
            )
            .toPact()
    }

    @Test
    @PactTestFor(pactMethod = "getSessionPact")
    fun `세션 단건 조회 시 올바른 세션 정보를 반환한다`(mockServer: MockServer) {
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = SessionClient(restClient)

        val session = client.getSession(1)

        assertThat(session).isNotNull
        assertThat(session!!.id).isEqualTo(1)
        assertThat(session.title).isEqualTo("gRPC로 마이크로서비스 구축하기")
        assertThat(session.speaker).isEqualTo("장호")
    }

    @Test
    @PactTestFor(pactMethod = "getSessionsPact")
    fun `세션 목록 조회 시 세션 리스트를 반환한다`(mockServer: MockServer) {
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = SessionClient(restClient)

        val sessions = client.getSessions()

        assertThat(sessions).isNotEmpty
        assertThat(sessions).hasSize(3)
    }

    @Test
    @PactTestFor(pactMethod = "getSessionNotFoundPact")
    fun `존재하지 않는 세션 조회 시 null을 반환한다`(mockServer: MockServer) {
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = SessionClient(restClient)

        val session = client.getSession(999)

        assertThat(session).isNull()
    }

    @Test
    @PactTestFor(pactMethod = "getSessionWithNullFieldsPact")
    fun `nullable 필드가 있는 세션 조회 시 description과 dateTime이 null인 세션을 반환한다`(mockServer: MockServer) {
        val restClient = RestClient.builder()
            .baseUrl(mockServer.getUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
        val client = SessionClient(restClient)

        val session = client.getSession(2)

        assertThat(session).isNotNull
        assertThat(session!!.id).isEqualTo(2)
        assertThat(session.title).isEqualTo("API 게이트웨이 패턴")
        assertThat(session.speaker).isEqualTo("김현수")
        assertThat(session.description).isNull()
        assertThat(session.dateTime).isNull()
    }
}

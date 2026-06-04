package com.conference.cfp.pact

import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import com.conference.cfp.client.SessionClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "SessionService", pactVersion = PactSpecVersion.V3)
class SessionServiceConsumerPactTest {

    @Pact(consumer = "CfpService")
    fun getSessionPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("세션 ID 1이 존재함")
            .uponReceiving("세션 조회 요청")
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
            )
            .toPact()
    }

    @Pact(consumer = "CfpService")
    fun getSessionNotFoundPact(builder: PactDslWithProvider): RequestResponsePact {
        return builder
            .given("세션 ID 999가 존재하지 않음")
            .uponReceiving("존재하지 않는 세션 조회")
            .path("/sessions/999")
            .method("GET")
            .headers("Accept", "application/json")
            .willRespondWith()
            .status(404)
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
}

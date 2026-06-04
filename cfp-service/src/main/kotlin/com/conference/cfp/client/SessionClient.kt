package com.conference.cfp.client

import com.conference.common.model.Session
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class SessionClient(private val sessionRestClient: RestClient) {

    fun getSession(id: Int): Session? {
        return try {
            sessionRestClient.get()
                .uri("/sessions/{id}", id)
                .retrieve()
                .body(Session::class.java)
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) null else throw e
        } catch (e: Exception) {
            null
        }
    }
}

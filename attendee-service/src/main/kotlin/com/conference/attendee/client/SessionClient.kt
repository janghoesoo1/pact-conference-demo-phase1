package com.conference.attendee.client

import com.conference.common.model.ApiResponse
import com.conference.common.model.Session
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class SessionClient(private val sessionRestClient: RestClient) {

    fun getSessions(): List<Session> {
        return try {
            val response = sessionRestClient.get()
                .uri("/sessions")
                .retrieve()
                .body(object : ParameterizedTypeReference<ApiResponse<Session>>() {})
            response?.data ?: emptyList()
        } catch (e: RestClientResponseException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSession(id: Int): Session? {
        return try {
            sessionRestClient.get()
                .uri("/sessions/{id}", id)
                .retrieve()
                .body(Session::class.java)
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) null else throw e
        }
    }
}

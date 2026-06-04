package com.conference.cfp.client

import com.conference.common.model.Attendee
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class AttendeeClient(private val attendeeRestClient: RestClient) {

    fun getAttendee(id: Int): Attendee? {
        return try {
            attendeeRestClient.get()
                .uri("/attendees/{id}", id)
                .retrieve()
                .body(Attendee::class.java)
        } catch (e: RestClientResponseException) {
            if (e.statusCode.value() == 404) null else throw e
        } catch (e: Exception) {
            null
        }
    }
}

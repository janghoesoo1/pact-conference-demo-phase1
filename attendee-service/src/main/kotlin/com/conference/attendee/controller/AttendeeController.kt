package com.conference.attendee.controller

import com.conference.attendee.client.SessionClient
import com.conference.attendee.dto.AttendeeResponse
import com.conference.attendee.dto.CreateAttendeeRequest
import com.conference.attendee.dto.UpdateAttendeeRequest
import com.conference.attendee.store.AttendeeStore
import com.conference.common.model.ApiResponse
import com.conference.common.model.Attendee
import com.conference.common.model.Session
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.net.URI

@RestController
@RequestMapping("/attendees")
class AttendeeController(
    private val attendeeStore: AttendeeStore,
    private val sessionClient: SessionClient
) {

    @GetMapping
    fun getAttendees(): ApiResponse<AttendeeResponse> {
        val attendees = attendeeStore.getAttendees().map { AttendeeResponse.from(it) }
        return ApiResponse(data = attendees)
    }

    @GetMapping("/{id}")
    fun getAttendee(@PathVariable id: Int): ResponseEntity<AttendeeResponse> {
        return ResponseEntity.ok(AttendeeResponse.from(attendeeStore.getAttendee(id)))
    }

    @GetMapping("/{id}/sessions")
    fun getAttendeeSessions(@PathVariable id: Int): ResponseEntity<ApiResponse<Session>> {
        attendeeStore.getAttendee(id)
        val sessions = sessionClient.getSessions()
        return ResponseEntity.ok(ApiResponse(data = sessions))
    }

    @PostMapping
    fun addAttendee(@Valid @RequestBody request: CreateAttendeeRequest): ResponseEntity<AttendeeResponse> {
        val saved = attendeeStore.addAttendee(request.toDomain())
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.id)
            .toUri()
        return ResponseEntity.created(location).body(AttendeeResponse.from(saved))
    }

    @PutMapping("/{id}")
    fun updateAttendee(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateAttendeeRequest
    ): ResponseEntity<Void> {
        val attendee = Attendee(
            givenName = request.givenName,
            surname = request.surname,
            email = request.email
        )
        attendeeStore.updateAttendee(id, attendee)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun removeAttendee(@PathVariable id: Int): ResponseEntity<Void> {
        attendeeStore.removeAttendee(id)
        return ResponseEntity.ok().build()
    }
}

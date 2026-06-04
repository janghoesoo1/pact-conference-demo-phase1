package com.conference.attendee.controller

import com.conference.attendee.client.SessionClient
import com.conference.attendee.store.AttendeeStore
import com.conference.common.model.ApiResponse
import com.conference.common.model.Attendee
import com.conference.common.model.Session
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
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

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/attendees")
class AttendeeController(
    private val attendeeStore: AttendeeStore,
    private val sessionClient: SessionClient
) {

    @GetMapping
    fun getAttendees(): ApiResponse<Attendee> {
        val attendees = attendeeStore.getAttendees()
        return ApiResponse(data = attendees)
    }

    @GetMapping("/{id}")
    fun getAttendee(@PathVariable id: Int): ResponseEntity<Attendee> {
        return ResponseEntity.ok(attendeeStore.getAttendee(id))
    }

    @GetMapping("/{id}/sessions")
    fun getAttendeeSessions(@PathVariable id: Int): ResponseEntity<ApiResponse<Session>> {
        attendeeStore.getAttendee(id)
        val sessions = sessionClient.getSessions()
        return ResponseEntity.ok(ApiResponse(data = sessions))
    }

    @PostMapping
    fun addAttendee(@RequestBody attendee: Attendee): ResponseEntity<Attendee> {
        val saved = attendeeStore.addAttendee(attendee)
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.id)
            .toUri()
        return ResponseEntity.created(location).body(saved)
    }

    @PutMapping("/{id}")
    fun updateAttendee(
        @PathVariable id: Int,
        @RequestBody attendee: Attendee
    ): ResponseEntity<Void> {
        attendeeStore.updateAttendee(id, attendee)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun removeAttendee(@PathVariable id: Int): ResponseEntity<Void> {
        attendeeStore.removeAttendee(id)
        return ResponseEntity.ok().build()
    }
}

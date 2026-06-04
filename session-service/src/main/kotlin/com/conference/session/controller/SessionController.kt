package com.conference.session.controller

import com.conference.common.model.ApiResponse
import com.conference.common.model.Session
import com.conference.session.dto.CreateSessionRequest
import com.conference.session.dto.SessionResponse
import com.conference.session.dto.UpdateSessionRequest
import com.conference.session.store.SessionStoreInterface
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
@RequestMapping("/sessions")
class SessionController(private val sessionStore: SessionStoreInterface) {

    @GetMapping
    fun getSessions(): ResponseEntity<ApiResponse<SessionResponse>> {
        val sessions = sessionStore.getSessions().map { SessionResponse.from(it) }
        return ResponseEntity.ok(ApiResponse(data = sessions))
    }

    @GetMapping("/{id}")
    fun getSession(@PathVariable id: Int): ResponseEntity<SessionResponse> {
        val session = sessionStore.getSession(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(SessionResponse.from(session))
    }

    @PostMapping
    fun createSession(@Valid @RequestBody request: CreateSessionRequest): ResponseEntity<SessionResponse> {
        val created = sessionStore.addSession(request.toDomain())
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()
        return ResponseEntity.created(location).body(SessionResponse.from(created))
    }

    @PutMapping("/{id}")
    fun updateSession(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateSessionRequest
    ): ResponseEntity<Void> {
        val session = Session(
            title = request.title,
            speaker = request.speaker,
            description = request.description,
            dateTime = request.dateTime
        )
        sessionStore.updateSession(id, session)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun deleteSession(@PathVariable id: Int): ResponseEntity<Void> {
        if (!sessionStore.removeSession(id)) return ResponseEntity.notFound().build()
        return ResponseEntity.ok().build()
    }
}

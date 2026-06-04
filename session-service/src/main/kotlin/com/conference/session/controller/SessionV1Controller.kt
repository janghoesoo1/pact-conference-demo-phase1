package com.conference.session.controller

import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.ApiResponse
import com.conference.session.dto.SessionResponse
import com.conference.session.store.SessionStoreInterface
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/sessions")
class SessionV1Controller(
    private val sessionStore: SessionStoreInterface
) {
    @GetMapping
    fun getAllSessions(): ResponseEntity<ApiResponse<SessionResponse>> {
        val sessions = sessionStore.getSessions().map { SessionResponse.from(it) }
        return ResponseEntity.ok(ApiResponse(data = sessions))
    }

    @GetMapping("/{id}")
    fun getSession(@PathVariable id: Int): ResponseEntity<SessionResponse> {
        val session = sessionStore.getSession(id)
            ?: throw ResourceNotFoundException("ID ${id}인 세션을 찾을 수 없습니다")
        return ResponseEntity.ok(SessionResponse.from(session))
    }
}

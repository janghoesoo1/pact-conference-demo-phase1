package com.conference.session.controller

import com.conference.common.exception.GlobalExceptionHandler
import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.Session
import com.conference.session.store.SessionStore
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDateTime

@WebMvcTest(SessionController::class, GlobalExceptionHandler::class)
class SessionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var sessionStore: SessionStore

    private val sampleSession = Session(
        id = 1,
        title = "gRPC로 마이크로서비스 구축하기",
        speaker = "장호",
        description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
        dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
    )

    @Test
    fun `GET sessions - 200 OK with list`() {
        every { sessionStore.getSessions() } returns listOf(sampleSession)

        mockMvc.get("/sessions") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].title") { value("gRPC로 마이크로서비스 구축하기") }
            jsonPath("$.total") { value(1) }
        }
    }

    @Test
    fun `GET sessions-id - 200 OK with single session`() {
        every { sessionStore.getSession(1) } returns sampleSession

        mockMvc.get("/sessions/1") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.title") { value("gRPC로 마이크로서비스 구축하기") }
            jsonPath("$.speaker") { value("장호") }
        }
    }

    @Test
    fun `GET sessions-id - 404 when not found`() {
        every { sessionStore.getSession(999) } throws ResourceNotFoundException("Session with id 999 not found")

        mockMvc.get("/sessions/999") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Session with id 999 not found") }
        }
    }

    @Test
    fun `POST sessions - 201 Created`() {
        val newSession = Session(
            title = "새로운 세션",
            speaker = "박지수",
            description = "새로운 세션 설명"
        )
        val createdSession = newSession.copy(id = 4)

        every { sessionStore.addSession(any()) } returns createdSession

        mockMvc.post("/sessions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"새로운 세션","speaker":"박지수","description":"새로운 세션 설명"}"""
        }.andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.id") { value(4) }
            jsonPath("$.title") { value("새로운 세션") }
        }
    }

    @Test
    fun `PUT sessions-id - 204 No Content`() {
        every { sessionStore.updateSession(1, any()) } returns sampleSession

        mockMvc.put("/sessions/1") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"수정된 세션","speaker":"장호","description":"수정된 설명"}"""
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE sessions-id - 200 OK`() {
        justRun { sessionStore.removeSession(1) }

        mockMvc.delete("/sessions/1").andExpect {
            status { isOk() }
        }
    }
}

package com.conference.attendee.component

import com.conference.attendee.client.SessionClient
import com.conference.attendee.store.AttendeeStore
import com.conference.common.model.Attendee
import com.conference.common.model.Session
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AttendeeApiComponentTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var attendeeStore: AttendeeStore

    @MockkBean
    private lateinit var sessionClient: SessionClient

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = ""
        attendeeStore.clear()
        attendeeStore.addAttendee(Attendee(givenName = "Test", surname = "User", email = "test@test.com"))
    }

    @Test
    fun `참가자 목록 조회 시 ApiResponse 형식으로 반환한다`() {
        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees")
        } Then {
            statusCode(200)
            body("data", hasSize<Any>(1))
            body("total", equalTo(1))
        }
    }

    @Test
    fun `존재하는 참가자 조회 시 200과 참가자 정보를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees/$createdId")
        } Then {
            statusCode(200)
            body("givenName", equalTo("Test"))
            body("surname", equalTo("User"))
            body("email", equalTo("test@test.com"))
        }
    }

    @Test
    fun `존재하지 않는 참가자 조회 시 404를 반환한다`() {
        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees/9999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `참가자 생성 시 201과 Location 헤더를 반환한다`() {
        val body = """{"givenName":"New","surname":"Attendee","email":"new@test.com"}"""

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
            body(body)
        } When {
            post("/attendees")
        } Then {
            statusCode(201)
            header("Location", notNullValue())
            body("givenName", equalTo("New"))
            body("surname", equalTo("Attendee"))
        }
    }

    @Test
    fun `참가자 수정 시 204를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees")
        } Extract {
            path<Int>("data[0].id")
        }

        val updateBody = """{"givenName":"Updated","surname":"User","email":"updated@test.com"}"""

        Given {
            contentType(ContentType.JSON)
            body(updateBody)
        } When {
            put("/attendees/$createdId")
        } Then {
            statusCode(204)
        }
    }

    @Test
    fun `참가자 삭제 시 200을 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
        } When {
            delete("/attendees/$createdId")
        } Then {
            statusCode(200)
        }
    }

    @Test
    fun `참가자 세션 조회 시 세션 목록을 반환한다`() {
        every { sessionClient.getSessions() } returns listOf(
            Session(id = 1, title = "Test Session", speaker = "Speaker 1", description = "Desc")
        )

        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/attendees/$createdId/sessions")
        } Then {
            statusCode(200)
            body("data", hasSize<Any>(1))
            body("data[0].title", equalTo("Test Session"))
        }
    }
}

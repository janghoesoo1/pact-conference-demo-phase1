package com.conference.session.component

import com.conference.common.model.Session
import com.conference.session.store.SessionStoreInterface
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
class SessionApiComponentTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sessionStore: SessionStoreInterface

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = ""
        sessionStore.clear()
        sessionStore.addSession(Session(title = "Test Session", speaker = "Speaker 1", description = "Test Description"))
    }

    @Test
    fun `세션 목록 조회 시 ApiResponse 형식으로 반환한다`() {
        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/sessions")
        } Then {
            statusCode(200)
            body("data", hasSize<Any>(1))
            body("total", equalTo(1))
        }
    }

    @Test
    fun `존재하는 세션 조회 시 200과 세션 정보를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/sessions")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/sessions/$createdId")
        } Then {
            statusCode(200)
            body("title", equalTo("Test Session"))
            body("speaker", equalTo("Speaker 1"))
        }
    }

    @Test
    fun `존재하지 않는 세션 조회 시 404를 반환한다`() {
        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/sessions/9999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `세션 생성 시 201과 Location 헤더를 반환한다`() {
        val body = """{"title":"New Session","speaker":"New Speaker","description":"New Description"}"""

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
            body(body)
        } When {
            post("/sessions")
        } Then {
            statusCode(201)
            header("Location", notNullValue())
            body("title", equalTo("New Session"))
            body("speaker", equalTo("New Speaker"))
        }
    }

    @Test
    fun `세션 수정 시 204를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/sessions")
        } Extract {
            path<Int>("data[0].id")
        }

        val updateBody = """{"title":"Updated Session","speaker":"Updated Speaker","description":"Updated Description"}"""

        Given {
            contentType(ContentType.JSON)
            body(updateBody)
        } When {
            put("/sessions/$createdId")
        } Then {
            statusCode(204)
        }
    }

    @Test
    fun `세션 삭제 시 200을 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/sessions")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
        } When {
            delete("/sessions/$createdId")
        } Then {
            statusCode(200)
        }
    }
}

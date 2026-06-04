package com.conference.session.controller

import com.conference.common.model.Session
import com.conference.session.store.SessionStoreInterface
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.hasKey
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionVersioningTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var sessionStore: SessionStoreInterface

    private var sessionId: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = ""
        sessionStore.clear()
        val created = sessionStore.addSession(
            Session(title = "Kotlin Spring REST API", speaker = "발표자", description = "API 버전 관리 데모")
        )
        sessionId = created.id!!
    }

    @Test
    fun `v1 세션 조회 시 기본 필드만 반환한다`() {
        Given {
            accept(ContentType.JSON)
        } When {
            get("/v1/sessions/$sessionId")
        } Then {
            statusCode(200)
            body("id", equalTo(sessionId))
            body("title", notNullValue())
            body("speaker", notNullValue())
            body("$", not(hasKey("tags")))
            body("$", not(hasKey("capacity")))
        }
    }

    @Test
    fun `v2 세션 조회 시 확장 필드를 포함한다`() {
        Given {
            accept(ContentType.JSON)
        } When {
            get("/v2/sessions/$sessionId")
        } Then {
            statusCode(200)
            body("id", equalTo(sessionId))
            body("title", notNullValue())
            body("speaker", notNullValue())
            body("tags", notNullValue())
            body("tags.size()", greaterThan(0))
            body("capacity", equalTo(100))
            body("registeredCount", equalTo(0))
        }
    }

    @Test
    fun `v1과 v2 목록 조회 모두 동작한다`() {
        Given {
            accept(ContentType.JSON)
        } When {
            get("/v1/sessions")
        } Then {
            statusCode(200)
            body("data", hasSize<Any>(greaterThan(0)))
        }

        Given {
            accept(ContentType.JSON)
        } When {
            get("/v2/sessions")
        } Then {
            statusCode(200)
            body("data", hasSize<Any>(greaterThan(0)))
            body("data[0].tags", notNullValue())
        }
    }
}

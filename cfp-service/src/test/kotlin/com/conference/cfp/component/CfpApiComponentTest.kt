package com.conference.cfp.component

import com.conference.cfp.client.AttendeeClient
import com.conference.cfp.client.SessionClient
import com.conference.cfp.model.Proposal
import com.conference.cfp.store.ProposalStore
import com.conference.cfp.store.VoteStore
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
class CfpApiComponentTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var proposalStore: ProposalStore

    @Autowired
    private lateinit var voteStore: VoteStore

    @MockkBean
    private lateinit var sessionClient: SessionClient

    @MockkBean
    private lateinit var attendeeClient: AttendeeClient

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
        RestAssured.basePath = ""
        proposalStore.clear()
        voteStore.clear()
        proposalStore.addProposal(Proposal(title = "Test Talk", abstract = "Abstract", speakerId = 1))

        every { attendeeClient.getAttendee(any()) } returns Attendee(
            id = 1,
            givenName = "Test",
            surname = "User",
            email = "test@test.com"
        )
        every { sessionClient.getSession(any()) } returns Session(
            id = 1,
            title = "Session",
            speaker = "Speaker"
        )
    }

    @Test
    fun `제안서 목록 조회 시 ApiResponse 형식으로 반환한다`() {
        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals")
        } Then {
            statusCode(200)
            body("data", hasSize<Any>(1))
            body("total", equalTo(1))
        }
    }

    @Test
    fun `존재하는 제안서 조회 시 200과 제안서 정보를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals/$createdId")
        } Then {
            statusCode(200)
            body("title", equalTo("Test Talk"))
            body("abstract", equalTo("Abstract"))
        }
    }

    @Test
    fun `존재하지 않는 제안서 조회 시 404를 반환한다`() {
        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals/9999")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun `제안서 생성 시 201과 Location 헤더를 반환한다`() {
        val body = """{"title":"New Talk","abstract":"New Abstract","speakerId":1}"""

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
            body(body)
        } When {
            post("/proposals")
        } Then {
            statusCode(201)
            header("Location", notNullValue())
            body("title", equalTo("New Talk"))
            body("abstract", equalTo("New Abstract"))
        }
    }

    @Test
    fun `제안서 수정 시 204를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals")
        } Extract {
            path<Int>("data[0].id")
        }

        val updateBody = """{"title":"Updated Talk","abstract":"Updated Abstract","speakerId":1}"""

        Given {
            contentType(ContentType.JSON)
            body(updateBody)
        } When {
            put("/proposals/$createdId")
        } Then {
            statusCode(204)
        }
    }

    @Test
    fun `제안서 삭제 시 200을 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals")
        } Extract {
            path<Int>("data[0].id")
        }

        Given {
            contentType(ContentType.JSON)
        } When {
            delete("/proposals/$createdId")
        } Then {
            statusCode(200)
        }
    }

    @Test
    fun `제안서에 투표 추가 시 201과 Location 헤더를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals")
        } Extract {
            path<Int>("data[0].id")
        }

        val voteBody = """{"proposalId":$createdId,"attendeeId":1,"score":5}"""

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
            body(voteBody)
        } When {
            post("/proposals/$createdId/votes")
        } Then {
            statusCode(201)
            header("Location", notNullValue())
            body("score", equalTo(5))
            body("attendeeId", equalTo(1))
        }
    }

    @Test
    fun `제안서 투표 목록 조회 시 투표와 평균점수를 반환한다`() {
        val createdId = Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals")
        } Extract {
            path<Int>("data[0].id")
        }

        val voteBody = """{"proposalId":$createdId,"attendeeId":1,"score":4}"""

        Given {
            contentType(ContentType.JSON)
            body(voteBody)
        } When {
            post("/proposals/$createdId/votes")
        }

        Given {
            contentType(ContentType.JSON)
            accept(ContentType.JSON)
        } When {
            get("/proposals/$createdId/votes")
        } Then {
            statusCode(200)
            body("votes", hasSize<Any>(1))
            body("averageScore", equalTo(4.0f))
        }
    }
}

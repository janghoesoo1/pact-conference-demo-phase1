package com.conference.cfp.controller

import com.conference.cfp.client.AttendeeClient
import com.conference.cfp.config.FeatureFlags
import com.conference.cfp.model.Proposal
import com.conference.cfp.model.ProposalStatus
import com.conference.cfp.model.Vote
import com.conference.cfp.store.ProposalStore
import com.conference.cfp.store.VoteStore
import com.conference.common.exception.GlobalExceptionHandler
import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.Attendee
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

@WebMvcTest(CfpController::class, GlobalExceptionHandler::class)
class CfpControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var proposalStore: ProposalStore

    @MockkBean
    private lateinit var voteStore: VoteStore

    @MockkBean
    private lateinit var attendeeClient: AttendeeClient

    @MockkBean
    private lateinit var featureFlags: FeatureFlags

    private val sampleProposal = Proposal(
        id = 1,
        title = "Kotlin Coroutines 심화",
        abstract = "Kotlin Coroutines의 심화 개념과 실전 패턴을 소개합니다.",
        speakerId = 1,
        status = ProposalStatus.SUBMITTED
    )

    private val sampleAttendee = Attendee(
        id = 1,
        givenName = "Jim",
        surname = "Gough",
        email = "gough@mail.com"
    )

    private val sampleVote = Vote(
        id = 1,
        proposalId = 1,
        attendeeId = 1,
        score = 5
    )

    @Test
    fun `GET proposals - 200 OK with list`() {
        every { proposalStore.getProposals() } returns listOf(sampleProposal)

        mockMvc.get("/proposals") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { isArray() }
            jsonPath("$.data[0].id") { value(1) }
            jsonPath("$.data[0].title") { value("Kotlin Coroutines 심화") }
            jsonPath("$.total") { value(1) }
        }
    }

    @Test
    fun `GET proposals-id - 200 OK with single proposal`() {
        every { proposalStore.getProposal(1) } returns sampleProposal

        mockMvc.get("/proposals/1") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value(1) }
            jsonPath("$.title") { value("Kotlin Coroutines 심화") }
            jsonPath("$.speakerId") { value(1) }
        }
    }

    @Test
    fun `GET proposals-id - 404 when not found`() {
        every { proposalStore.getProposal(999) } throws ResourceNotFoundException("Proposal with id 999 not found")

        mockMvc.get("/proposals/999") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { value("Proposal with id 999 not found") }
        }
    }

    @Test
    fun `POST proposals - 201 Created`() {
        val newProposal = Proposal(
            title = "새로운 프로포절",
            abstract = "새로운 프로포절 설명",
            speakerId = 1
        )
        val createdProposal = newProposal.copy(id = 4)

        every { attendeeClient.getAttendee(1) } returns sampleAttendee
        every { proposalStore.addProposal(any()) } returns createdProposal

        mockMvc.post("/proposals") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"새로운 프로포절","abstract":"새로운 프로포절 설명","speakerId":1}"""
        }.andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.id") { value(4) }
            jsonPath("$.title") { value("새로운 프로포절") }
        }
    }

    @Test
    fun `POST proposals - 404 when speaker not found`() {
        every { attendeeClient.getAttendee(999) } returns null

        mockMvc.post("/proposals") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"새로운 프로포절","abstract":"설명","speakerId":999}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PUT proposals-id - 204 No Content`() {
        every { proposalStore.updateProposal(1, any()) } returns sampleProposal

        mockMvc.put("/proposals/1") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"title":"수정된 프로포절","abstract":"수정된 설명","speakerId":1}"""
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `DELETE proposals-id - 200 OK`() {
        justRun { proposalStore.removeProposal(1) }

        mockMvc.delete("/proposals/1").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `POST proposals-id-votes - 201 Created`() {
        every { proposalStore.getProposal(1) } returns sampleProposal
        every { attendeeClient.getAttendee(1) } returns sampleAttendee
        every { voteStore.addVote(any()) } returns sampleVote

        mockMvc.post("/proposals/1/votes") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"proposalId":1,"attendeeId":1,"score":5}"""
        }.andExpect {
            status { isCreated() }
            header { exists("Location") }
            jsonPath("$.id") { value(1) }
            jsonPath("$.score") { value(5) }
        }
    }

    @Test
    fun `GET proposals-id-votes - 200 OK`() {
        every { proposalStore.getProposal(1) } returns sampleProposal
        every { voteStore.getVotesByProposal(1) } returns listOf(sampleVote)
        every { voteStore.getAverageScore(1) } returns 5.0
        every { featureFlags.newVotingAlgorithm } returns false

        mockMvc.get("/proposals/1/votes") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
            jsonPath("$.votes") { isArray() }
            jsonPath("$.votes[0].score") { value(5) }
            jsonPath("$.averageScore") { value(5.0) }
        }
    }
}

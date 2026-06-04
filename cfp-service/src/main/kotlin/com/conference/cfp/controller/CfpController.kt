package com.conference.cfp.controller

import com.conference.cfp.client.AttendeeClient
import com.conference.cfp.dto.CreateProposalRequest
import com.conference.cfp.dto.CreateVoteRequest
import com.conference.cfp.dto.ProposalResponse
import com.conference.cfp.dto.UpdateProposalRequest
import com.conference.cfp.dto.VoteResponse
import com.conference.cfp.dto.VoteSummaryResponse
import com.conference.cfp.model.Proposal
import com.conference.cfp.model.ProposalStatus
import com.conference.cfp.model.Vote
import com.conference.cfp.store.ProposalStore
import com.conference.cfp.store.VoteStore
import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.ApiResponse
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
@RequestMapping("/proposals")
class CfpController(
    private val proposalStore: ProposalStore,
    private val voteStore: VoteStore,
    private val attendeeClient: AttendeeClient
) {

    @GetMapping
    fun getProposals(): ResponseEntity<ApiResponse<ProposalResponse>> {
        val proposals = proposalStore.getProposals().map { ProposalResponse.from(it) }
        return ResponseEntity.ok(ApiResponse(data = proposals))
    }

    @GetMapping("/{id}")
    fun getProposal(@PathVariable id: Int): ResponseEntity<ProposalResponse> {
        val proposal = proposalStore.getProposal(id)
        return ResponseEntity.ok(ProposalResponse.from(proposal))
    }

    @PostMapping
    fun createProposal(@Valid @RequestBody request: CreateProposalRequest): ResponseEntity<ProposalResponse> {
        attendeeClient.getAttendee(request.speakerId)
            ?: throw ResourceNotFoundException("Attendee with id ${request.speakerId} not found")
        val created = proposalStore.addProposal(request.toDomain())
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()
        return ResponseEntity.created(location).body(ProposalResponse.from(created))
    }

    @PutMapping("/{id}")
    fun updateProposal(
        @PathVariable id: Int,
        @Valid @RequestBody request: UpdateProposalRequest
    ): ResponseEntity<Void> {
        val proposal = Proposal(
            title = request.title,
            abstract = request.abstract,
            speakerId = 0,
            status = request.status ?: ProposalStatus.SUBMITTED,
            sessionId = request.sessionId
        )
        proposalStore.updateProposal(id, proposal)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun deleteProposal(@PathVariable id: Int): ResponseEntity<Void> {
        proposalStore.removeProposal(id)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/{id}/votes")
    fun addVote(
        @PathVariable id: Int,
        @Valid @RequestBody request: CreateVoteRequest
    ): ResponseEntity<VoteResponse> {
        proposalStore.getProposal(id)
        attendeeClient.getAttendee(request.attendeeId)
            ?: throw ResourceNotFoundException("Attendee with id ${request.attendeeId} not found")
        val vote = Vote(proposalId = id, attendeeId = request.attendeeId, score = request.score)
        val created = voteStore.addVote(vote)
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()
        return ResponseEntity.created(location).body(VoteResponse.from(created))
    }

    @GetMapping("/{id}/votes")
    fun getVotes(@PathVariable id: Int): ResponseEntity<VoteSummaryResponse> {
        proposalStore.getProposal(id)
        val votes = voteStore.getVotesByProposal(id).map { VoteResponse.from(it) }
        val averageScore = voteStore.getAverageScore(id)
        return ResponseEntity.ok(VoteSummaryResponse(votes = votes, averageScore = averageScore))
    }
}

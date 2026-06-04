package com.conference.cfp.controller

import com.conference.cfp.client.AttendeeClient
import com.conference.cfp.model.Proposal
import com.conference.cfp.model.Vote
import com.conference.cfp.store.ProposalStore
import com.conference.cfp.store.VoteStore
import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.ApiResponse
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

data class VoteSummary(
    val votes: List<Vote>,
    val averageScore: Double
)

@CrossOrigin(origins = ["*"])
@RestController
@RequestMapping("/proposals")
class CfpController(
    private val proposalStore: ProposalStore,
    private val voteStore: VoteStore,
    private val attendeeClient: AttendeeClient
) {

    @GetMapping
    fun getProposals(): ResponseEntity<ApiResponse<Proposal>> {
        val proposals = proposalStore.getProposals()
        return ResponseEntity.ok(ApiResponse(data = proposals))
    }

    @GetMapping("/{id}")
    fun getProposal(@PathVariable id: Int): ResponseEntity<Proposal> {
        val proposal = proposalStore.getProposal(id)
        return ResponseEntity.ok(proposal)
    }

    @PostMapping
    fun createProposal(@RequestBody proposal: Proposal): ResponseEntity<Proposal> {
        attendeeClient.getAttendee(proposal.speakerId)
            ?: throw ResourceNotFoundException("Attendee with id ${proposal.speakerId} not found")
        val created = proposalStore.addProposal(proposal)
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()
        return ResponseEntity.created(location).body(created)
    }

    @PutMapping("/{id}")
    fun updateProposal(
        @PathVariable id: Int,
        @RequestBody proposal: Proposal
    ): ResponseEntity<Void> {
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
        @RequestBody vote: Vote
    ): ResponseEntity<Vote> {
        proposalStore.getProposal(id)
        attendeeClient.getAttendee(vote.attendeeId)
            ?: throw ResourceNotFoundException("Attendee with id ${vote.attendeeId} not found")
        val voteWithProposalId = vote.copy(proposalId = id)
        val created = voteStore.addVote(voteWithProposalId)
        val location: URI = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id)
            .toUri()
        return ResponseEntity.created(location).body(created)
    }

    @GetMapping("/{id}/votes")
    fun getVotes(@PathVariable id: Int): ResponseEntity<VoteSummary> {
        proposalStore.getProposal(id)
        val votes = voteStore.getVotesByProposal(id)
        val averageScore = voteStore.getAverageScore(id)
        return ResponseEntity.ok(VoteSummary(votes = votes, averageScore = averageScore))
    }
}

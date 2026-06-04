package com.conference.cfp.dto

import com.conference.cfp.model.Proposal
import com.conference.cfp.model.ProposalStatus
import com.conference.cfp.model.Vote
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateProposalRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "초록은 필수입니다")
    @field:Size(max = 1000, message = "초록은 1000자 이하여야 합니다")
    val abstract: String,

    @field:NotNull(message = "발표자 ID는 필수입니다")
    val speakerId: Int
) {
    fun toDomain() = Proposal(
        title = title,
        abstract = abstract,
        speakerId = speakerId
    )
}

data class UpdateProposalRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "초록은 필수입니다")
    @field:Size(max = 1000, message = "초록은 1000자 이하여야 합니다")
    val abstract: String,

    val status: ProposalStatus? = null,
    val sessionId: Int? = null
)

data class CreateVoteRequest(
    @field:NotNull(message = "참석자 ID는 필수입니다")
    val attendeeId: Int,

    @field:Min(value = 1, message = "점수는 1 이상이어야 합니다")
    @field:Max(value = 5, message = "점수는 5 이하여야 합니다")
    val score: Int
)

data class ProposalResponse(
    val id: Int,
    val title: String,
    val abstract: String,
    val speakerId: Int,
    val status: ProposalStatus,
    val sessionId: Int?
) {
    companion object {
        fun from(proposal: Proposal) = ProposalResponse(
            id = proposal.id!!,
            title = proposal.title,
            abstract = proposal.abstract,
            speakerId = proposal.speakerId,
            status = proposal.status,
            sessionId = proposal.sessionId
        )
    }
}

data class VoteResponse(
    val id: Int,
    val proposalId: Int,
    val attendeeId: Int,
    val score: Int
) {
    companion object {
        fun from(vote: Vote) = VoteResponse(
            id = vote.id!!,
            proposalId = vote.proposalId,
            attendeeId = vote.attendeeId,
            score = vote.score
        )
    }
}

data class VoteSummaryResponse(
    val votes: List<VoteResponse>,
    val averageScore: Double
)

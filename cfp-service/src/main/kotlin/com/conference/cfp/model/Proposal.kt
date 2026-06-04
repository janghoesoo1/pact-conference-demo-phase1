package com.conference.cfp.model

data class Proposal(
    val id: Int? = null,
    val title: String,
    val abstract: String,
    val speakerId: Int,
    val sessionId: Int? = null,
    val status: ProposalStatus = ProposalStatus.SUBMITTED
)

enum class ProposalStatus {
    SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
}

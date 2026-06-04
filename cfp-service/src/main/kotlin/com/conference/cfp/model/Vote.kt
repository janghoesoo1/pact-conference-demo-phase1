package com.conference.cfp.model

data class Vote(
    val id: Int? = null,
    val proposalId: Int,
    val attendeeId: Int,
    val score: Int
)

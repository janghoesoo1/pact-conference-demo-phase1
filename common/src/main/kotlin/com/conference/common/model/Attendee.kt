package com.conference.common.model

data class Attendee(
    val id: Int? = null,
    val givenName: String,
    val surname: String,
    val email: String
)

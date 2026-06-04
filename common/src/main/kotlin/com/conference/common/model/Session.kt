package com.conference.common.model

import java.time.LocalDateTime

data class Session(
    val id: Int? = null,
    val title: String,
    val speaker: String,
    val description: String? = null,
    val dateTime: LocalDateTime? = null
)

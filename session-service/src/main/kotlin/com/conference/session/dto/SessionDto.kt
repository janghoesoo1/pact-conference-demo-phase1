package com.conference.session.dto

import com.conference.common.model.Session
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CreateSessionRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "발표자는 필수입니다")
    val speaker: String,

    @field:Size(max = 500, message = "설명은 500자 이하여야 합니다")
    val description: String? = null,

    val dateTime: LocalDateTime? = null
) {
    fun toDomain() = Session(
        title = title,
        speaker = speaker,
        description = description,
        dateTime = dateTime
    )
}

data class UpdateSessionRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "발표자는 필수입니다")
    val speaker: String,

    @field:Size(max = 500, message = "설명은 500자 이하여야 합니다")
    val description: String? = null,

    val dateTime: LocalDateTime? = null
)

data class SessionResponse(
    val id: Int,
    val title: String,
    val speaker: String,
    val description: String?,
    val dateTime: LocalDateTime?
) {
    companion object {
        fun from(session: Session) = SessionResponse(
            id = session.id!!,
            title = session.title,
            speaker = session.speaker,
            description = session.description,
            dateTime = session.dateTime
        )
    }
}

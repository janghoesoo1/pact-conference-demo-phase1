package com.conference.attendee.dto

import com.conference.common.model.Attendee
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class CreateAttendeeRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val givenName: String,

    @field:NotBlank(message = "성은 필수입니다")
    val surname: String,

    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    val email: String
) {
    fun toDomain() = Attendee(
        givenName = givenName,
        surname = surname,
        email = email
    )
}

data class UpdateAttendeeRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    val givenName: String,

    @field:NotBlank(message = "성은 필수입니다")
    val surname: String,

    @field:NotBlank(message = "이메일은 필수입니다")
    @field:Email(message = "올바른 이메일 형식이어야 합니다")
    val email: String
)

data class AttendeeResponse(
    val id: Int,
    val givenName: String,
    val surname: String,
    val email: String
) {
    companion object {
        fun from(attendee: Attendee) = AttendeeResponse(
            id = attendee.id!!,
            givenName = attendee.givenName,
            surname = attendee.surname,
            email = attendee.email
        )
    }
}

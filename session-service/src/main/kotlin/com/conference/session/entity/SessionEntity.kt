package com.conference.session.entity

import com.conference.common.model.Session
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sessions")
class SessionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var speaker: String,

    var description: String? = null,

    var dateTime: LocalDateTime? = null
) {
    fun toDomain(): Session = Session(
        id = id,
        title = title,
        speaker = speaker,
        description = description,
        dateTime = dateTime
    )

    companion object {
        fun fromDomain(session: Session): SessionEntity = SessionEntity(
            id = session.id,
            title = session.title,
            speaker = session.speaker,
            description = session.description,
            dateTime = session.dateTime
        )
    }
}

package com.conference.session.dto

import com.conference.common.model.Session
import java.time.LocalDateTime

data class SessionV2Response(
    val id: Int,
    val title: String,
    val speaker: String,
    val description: String?,
    val dateTime: LocalDateTime?,
    val tags: List<String>,
    val capacity: Int,
    val registeredCount: Int
) {
    companion object {
        fun from(session: Session) = SessionV2Response(
            id = session.id!!,
            title = session.title,
            speaker = session.speaker,
            description = session.description,
            dateTime = session.dateTime,
            tags = inferTags(session.title),
            capacity = 100,
            registeredCount = 0
        )

        private fun inferTags(title: String): List<String> {
            val tags = mutableListOf<String>()
            val lower = title.lowercase()
            if (lower.contains("grpc") || lower.contains("api") || lower.contains("rest")) tags.add("api")
            if (lower.contains("테스트") || lower.contains("test") || lower.contains("pact")) tags.add("testing")
            if (lower.contains("마이크로") || lower.contains("micro")) tags.add("microservices")
            if (lower.contains("게이트웨이") || lower.contains("gateway")) tags.add("infrastructure")
            if (lower.contains("kotlin") || lower.contains("spring")) tags.add("framework")
            if (tags.isEmpty()) tags.add("general")
            return tags
        }
    }
}

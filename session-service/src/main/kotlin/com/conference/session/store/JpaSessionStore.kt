package com.conference.session.store

import com.conference.common.model.Session
import com.conference.session.entity.SessionEntity
import com.conference.session.repository.SessionJpaRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("jpa")
class JpaSessionStore(
    private val repository: SessionJpaRepository
) : SessionStoreInterface {

    override fun getSessions(): List<Session> =
        repository.findAll().map { it.toDomain() }

    override fun getSession(id: Int): Session? =
        repository.findById(id).orElse(null)?.toDomain()

    override fun addSession(session: Session): Session {
        val entity = SessionEntity.fromDomain(session.copy(id = null))
        return repository.save(entity).toDomain()
    }

    override fun updateSession(id: Int, session: Session): Session? {
        val existing = repository.findById(id).orElse(null) ?: return null
        existing.title = session.title
        existing.speaker = session.speaker
        existing.description = session.description
        existing.dateTime = session.dateTime
        return repository.save(existing).toDomain()
    }

    override fun removeSession(id: Int): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }

    override fun clear() {
        repository.deleteAll()
    }
}

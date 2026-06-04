package com.conference.session.store

import com.conference.common.exception.ResourceNotFoundException
import com.conference.common.model.Session
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
@Profile("default")
class SessionStore : SessionStoreInterface {

    private val counter = AtomicInteger(3)
    private val store: ConcurrentHashMap<Int, Session> = ConcurrentHashMap<Int, Session>().apply {
        put(1, Session(
            id = 1,
            title = "gRPC로 마이크로서비스 구축하기",
            speaker = "장호",
            description = "gRPC를 활용한 고성능 마이크로서비스 통신 방법을 소개합니다.",
            dateTime = LocalDateTime.of(2024, 9, 15, 10, 0)
        ))
        put(2, Session(
            id = 2,
            title = "API 게이트웨이 패턴",
            speaker = "김현수",
            description = "API 게이트웨이의 다양한 패턴과 구현 전략을 살펴봅니다.",
            dateTime = LocalDateTime.of(2024, 9, 15, 14, 0)
        ))
        put(3, Session(
            id = 3,
            title = "계약 테스트 실전",
            speaker = "이서연",
            description = "Pact 프레임워크를 사용한 CDC 테스트 실전 사례를 공유합니다.",
            dateTime = LocalDateTime.of(2024, 9, 16, 10, 0)
        ))
    }

    override fun getSessions(): List<Session> = store.values.toList()

    override fun getSession(id: Int): Session? = store[id]

    override fun addSession(session: Session): Session {
        val id = counter.incrementAndGet()
        val newSession = session.copy(id = id)
        store[id] = newSession
        return newSession
    }

    override fun updateSession(id: Int, session: Session): Session? {
        if (!store.containsKey(id)) return null
        val updated = session.copy(id = id)
        store[id] = updated
        return updated
    }

    override fun removeSession(id: Int): Boolean {
        if (!store.containsKey(id)) return false
        store.remove(id)
        return true
    }

    override fun clear() {
        store.clear()
        counter.set(0)
    }
}

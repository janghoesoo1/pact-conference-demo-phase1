package com.conference.cfp.store

import com.conference.cfp.model.Proposal
import com.conference.cfp.model.ProposalStatus
import com.conference.common.exception.ResourceNotFoundException
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class ProposalStore {

    private val counter = AtomicInteger(3)
    private val store: ConcurrentHashMap<Int, Proposal> = ConcurrentHashMap<Int, Proposal>().apply {
        put(1, Proposal(
            id = 1,
            title = "Kotlin Coroutines 심화",
            abstract = "Kotlin Coroutines의 심화 개념과 실전 패턴을 소개합니다.",
            speakerId = 1,
            status = ProposalStatus.SUBMITTED
        ))
        put(2, Proposal(
            id = 2,
            title = "Spring Security 6 실전",
            abstract = "Spring Security 6의 주요 변경사항과 실전 적용 방법을 살펴봅니다.",
            speakerId = 2,
            status = ProposalStatus.UNDER_REVIEW
        ))
        put(3, Proposal(
            id = 3,
            title = "GraphQL vs REST",
            abstract = "GraphQL과 REST의 장단점을 비교하고 선택 기준을 제시합니다.",
            speakerId = 3,
            sessionId = 1,
            status = ProposalStatus.APPROVED
        ))
    }

    fun getProposals(): List<Proposal> = store.values.toList()

    fun getProposal(id: Int): Proposal =
        store[id] ?: throw ResourceNotFoundException("Proposal with id $id not found")

    fun addProposal(proposal: Proposal): Proposal {
        val id = counter.incrementAndGet()
        val newProposal = proposal.copy(id = id)
        store[id] = newProposal
        return newProposal
    }

    fun updateProposal(id: Int, proposal: Proposal): Proposal {
        if (!store.containsKey(id)) {
            throw ResourceNotFoundException("Proposal with id $id not found")
        }
        val updated = proposal.copy(id = id)
        store[id] = updated
        return updated
    }

    fun removeProposal(id: Int) {
        if (!store.containsKey(id)) {
            throw ResourceNotFoundException("Proposal with id $id not found")
        }
        store.remove(id)
    }

    fun clear() {
        store.clear()
        counter.set(0)
    }
}

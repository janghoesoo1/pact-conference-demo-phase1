package com.conference.cfp.store

import com.conference.cfp.model.Vote
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Component
class VoteStore {

    private val counter = AtomicInteger(0)
    private val store: ConcurrentHashMap<Int, Vote> = ConcurrentHashMap()

    fun getVotesByProposal(proposalId: Int): List<Vote> =
        store.values.filter { it.proposalId == proposalId }

    fun addVote(vote: Vote): Vote {
        val id = counter.incrementAndGet()
        val newVote = vote.copy(id = id)
        store[id] = newVote
        return newVote
    }

    fun getAverageScore(proposalId: Int): Double {
        val votes = getVotesByProposal(proposalId)
        return if (votes.isEmpty()) 0.0 else votes.map { it.score }.average()
    }

    fun clear() {
        store.clear()
        counter.set(0)
    }
}

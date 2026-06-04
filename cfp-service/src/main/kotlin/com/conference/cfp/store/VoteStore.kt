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

    fun getWeightedAverageScore(proposalId: Int): Double {
        val votes = getVotesByProposal(proposalId)
        if (votes.isEmpty()) return 0.0
        val recentCount = 3
        val recentVotes = votes.takeLast(recentCount)
        val olderVotes = votes.dropLast(recentCount)
        val weightedSum = recentVotes.sumOf { it.score * 2.0 } + olderVotes.sumOf { it.score * 1.0 }
        val totalWeight = recentVotes.size * 2.0 + olderVotes.size * 1.0
        return weightedSum / totalWeight
    }

    fun getAllVotes(): List<Vote> = store.values.toList()

    fun clear() {
        store.clear()
        counter.set(0)
    }
}

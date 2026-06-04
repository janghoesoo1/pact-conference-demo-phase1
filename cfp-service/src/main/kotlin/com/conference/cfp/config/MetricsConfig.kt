package com.conference.cfp.config

import com.conference.cfp.store.ProposalStore
import com.conference.cfp.store.VoteStore
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig(
    meterRegistry: MeterRegistry,
    proposalStore: ProposalStore,
    voteStore: VoteStore
) {
    init {
        Gauge.builder("conference.proposals.total") { proposalStore.getProposals().size.toDouble() }
            .description("Total number of proposals")
            .register(meterRegistry)

        Gauge.builder("conference.votes.total") { voteStore.getAllVotes().size.toDouble() }
            .description("Total number of votes")
            .register(meterRegistry)
    }
}

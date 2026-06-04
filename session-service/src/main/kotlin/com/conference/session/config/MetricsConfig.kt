package com.conference.session.config

import com.conference.session.store.SessionStoreInterface
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig(
    meterRegistry: MeterRegistry,
    sessionStore: SessionStoreInterface
) {
    init {
        Gauge.builder("conference.sessions.total") { sessionStore.getSessions().size.toDouble() }
            .description("Total number of sessions")
            .register(meterRegistry)
    }
}

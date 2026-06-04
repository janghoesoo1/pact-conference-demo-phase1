package com.conference.attendee.config

import com.conference.attendee.store.AttendeeStore
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Configuration

@Configuration
class MetricsConfig(
    meterRegistry: MeterRegistry,
    attendeeStore: AttendeeStore
) {
    init {
        Gauge.builder("conference.attendees.total") { attendeeStore.getAttendees().size.toDouble() }
            .description("Total number of registered attendees")
            .register(meterRegistry)
    }
}

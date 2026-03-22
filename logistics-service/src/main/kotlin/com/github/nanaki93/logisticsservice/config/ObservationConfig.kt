package com.github.nanaki93.logisticsservice.config

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.aop.ObservedAspect
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
class ObservationConfig {
    @Bean
    fun observedAspect(observationRegistry: ObservationRegistry): ObservedAspect = ObservedAspect(observationRegistry)
}

@Component
class EventMetrics(private val registry: MeterRegistry){

    fun incrementProcessed(outcome: String) {
        Counter.builder("logistics_events_processed_total")
            .description("Total number of processed logistics events")
            .tag("outcome", outcome)
            .register(registry)
            .increment()
    }

    fun incrementStatusTransition(fromStatus: String, toStatus: String) {
        Counter.builder("logistics_status_transitions_total")
            .description("Total number of parcel status transitions")
            .tag("from_status", fromStatus)
            .tag("to_status", toStatus)
            .register(registry)
            .increment()
    }

    fun recordRouteDeviationMeters(deviationMeters: Double) {
        DistributionSummary.builder("logistics_route_deviation_meters")
            .description("Route deviation in meters")
            .baseUnit("meters")
            .register(registry)
            .record(deviationMeters)
    }
}


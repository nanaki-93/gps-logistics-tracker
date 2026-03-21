package com.github.nanaki93.logisticsservice.domain.telemetryevent

import io.micrometer.observation.annotation.Observed
import io.opentelemetry.api.trace.Span
import org.springframework.amqp.AmqpRejectAndDontRequeueException
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class TelemetryEventConsumer(
    private val telemetryEventService: TelemetryEventService,
) {
    @RabbitListener(queues = [$$"${rabbitmq.queue}"])
    @Observed(
        name = "telemetry.event.consume",
        contextualName = "consume-telemetry-event",
    )
    fun consumeTelemetryEvent(event: TelemetryEventPlainDto) {
        try {
            println("Processing message in trace: ${Span.current().spanContext.traceId}")
            telemetryEventService.processTelemetryEvent(event)
        } catch (e: Exception) {
            // Log the error and continue processing other events
            println("Error processing telemetry event: ${e.message}")
            // split the logic in recoverable and non-recoverable errors
            if (e is IllegalArgumentException) {
                // Log the error and continue processing other events
                println("Recoverable error processing telemetry event: ${e.message}")
                throw AmqpRejectAndDontRequeueException(e)
            } else {
                // Log the error and stop processing other events
                println("Transient error processing telemetry event, requeing: ${e.message}")
                throw e
            }
        }
    }
}

package com.github.nanaki93.logisticsservice.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RabbitMQConfigTest {
    @Test
    fun telemetryEventQueueRoutesDeadLettersToDlq() {
        val props =
            RabbitMQProperties(
                exchange = "telemetry.exchange",
                queue = "telemetry.queue",
                routingKey = "telemetry.#",
                dlq = "telemetry.dlq",
            )
        val config = RabbitMQConfig(props)

        val queue = config.telemetryEventQueue()

        assertEquals(props.queue, queue.name)
        assertTrue(queue.isDurable)
        assertEquals("", queue.arguments["x-dead-letter-exchange"])
        assertEquals(props.dlq, queue.arguments["x-dead-letter-routing-key"])
    }
}

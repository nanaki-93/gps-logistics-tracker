package com.github.nanaki93.logisticsservice.domain.telemetryevent

import java.time.Instant
import java.util.UUID

data class TelemetryEvent(
    val telemetryEventUid: UUID = UUID.randomUUID(),
    val driverUid: UUID,
    val coordinates: String,
    val recordedAt: Instant,
)

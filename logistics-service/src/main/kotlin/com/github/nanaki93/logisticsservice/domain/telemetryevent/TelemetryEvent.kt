package com.github.nanaki93.logisticsservice.domain.telemetryevent

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "telemetry_event")
class TelemetryEvent(
    @Id
    val telemetryEventUid: UUID = UUID.randomUUID(),
    val driverUid: UUID,
    @Column(columnDefinition = "geography(POINT,4326)")
    val coordinates: String,
    val recordedAt: Instant,
)

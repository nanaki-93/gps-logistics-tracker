package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleDto
import java.time.Instant

data class TelemetryEventDto(
    val vehicle: VehicleDto,
    val coordinates: CoordinatesDto,
    val recordedAt: Instant,
)

package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto
import java.time.Instant

data class TelemetryEventDto(
    val driver: DriverDto,
    val coordinates: CoordinatesDto,
    val recordedAt: Instant,
)

data class TelemetryEventInsertDto(
    val driverUid: String,
    val longitude: Double,
    val latitude: Double,
    val recordedAt: Instant,
)

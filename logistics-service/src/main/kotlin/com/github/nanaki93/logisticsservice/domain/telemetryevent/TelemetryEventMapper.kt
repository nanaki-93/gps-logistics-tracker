package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinates
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto
import java.util.UUID

object TelemetryEventMapper {
    fun toDto(
        telemetryEvent: TelemetryEvent,
        driverDto: DriverDto,
    ): TelemetryEventDto =
        TelemetryEventDto(
            driver = driverDto,
            coordinates = telemetryEvent.coordinates.toCoordinatesDto(),
            recordedAt = telemetryEvent.recordedAt,
        )

    fun toEntity(
        telemetryEventDto: TelemetryEventDto,
        driverUid: UUID,
    ): TelemetryEvent =
        TelemetryEvent(
            driverUid = driverUid,
            coordinates = telemetryEventDto.coordinates.toCoordinates(),
            recordedAt = telemetryEventDto.recordedAt,
        )
}

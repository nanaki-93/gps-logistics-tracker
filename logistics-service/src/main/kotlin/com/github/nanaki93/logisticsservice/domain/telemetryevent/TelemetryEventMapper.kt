package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toJtsPoint
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto
import com.github.nanaki93.logisticsservice.domain.util.toUuid

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

    fun toEntity(telemetryEventDto: TelemetryEventPlainDto): TelemetryEvent =
        TelemetryEvent(
            driverUid = telemetryEventDto.driverUid.toUuid(),
            coordinates = Pair(telemetryEventDto.lat,telemetryEventDto.lng ).toJtsPoint(),
            recordedAt = telemetryEventDto.recordedAt,
        )
}

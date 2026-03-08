package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinates
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleDto
import java.util.UUID

object TelemetryEventMapper {
    fun toDto(
        telemetryEvent: TelemetryEvent,
        vehicleDto: VehicleDto,
    ): TelemetryEventDto =
        TelemetryEventDto(
            vehicle = vehicleDto,
            coordinates = telemetryEvent.coordinates.toCoordinatesDto(),
            recordedAt = telemetryEvent.recordedAt,
        )

    fun toEntity(
        telemetryEventDto: TelemetryEventDto,
        vehicleUid: UUID,
    ): TelemetryEvent =
        TelemetryEvent(
            vehicleUid = vehicleUid,
            coordinates = telemetryEventDto.coordinates.toCoordinates(),
            recordedAt = telemetryEventDto.recordedAt,
        )
}

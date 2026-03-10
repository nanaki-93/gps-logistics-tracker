package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.parcel.ParcelService
import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto
import com.github.nanaki93.logisticsservice.domain.util.toUuid
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class TelemetryEventService(
    val telemetryEventRepository: TelemetryEventRepository,
    val locationCacheRepository: LocationCacheRepository,
    val parcelService: ParcelService,
) {
    fun processTelemetryEvent(event: TelemetryEventInsertDto) {
        val lastTelemetryEvent = getLastTelemetryEventByDriver(event.driverUid)
        if (shouldSkip(lastTelemetryEvent.recordedAt, event.recordedAt)) {
            println(
                "Skipping telemetry event for driver ${event.driverUid} recorded at ${event.recordedAt} as it's too close to the last recorded event at ${lastTelemetryEvent.recordedAt}",
            )
            return
        }
        save(event)

        locationCacheRepository.set(event.driverUid, CoordinatesDto(event.lng, event.lat))

        parcelService.evaluateAll(event)

        println("TelemetryEvent received: ${event.driverUid} - lng : ${event.lng} - lat: ${event.lat} at ${event.recordedAt}")
    }

    fun getLastTelemetryEventByDriver(driverUid: String): TelemetryEvent =
        telemetryEventRepository.findLastEventByDriverUid(driverUid.toUuid()).orElseThrow {
            IllegalArgumentException("No telemetry event found for driver $driverUid")
        }

    fun shouldSkip(
        lastRecordedAt: Instant,
        newRecordedAt: Instant,
    ): Boolean = lastRecordedAt.plusSeconds(1).isAfter(newRecordedAt)

    fun save(event: TelemetryEventInsertDto) {
        telemetryEventRepository.save(TelemetryEventMapper.toEntity(event))
    }
}

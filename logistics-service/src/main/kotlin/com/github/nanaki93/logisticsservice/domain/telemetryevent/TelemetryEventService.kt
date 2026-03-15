package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.parcel.ParcelService
import com.github.nanaki93.logisticsservice.domain.telemetryevent.websocket.TelemetryEventWebSocketHandler
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
    val webSocketHandler: TelemetryEventWebSocketHandler,
) {
    fun processTelemetryEvent(event: TelemetryEventPlainDto) {
        getLastTelemetryEventByDriver(event.driverUid)?.let { lastTelemetryEvent ->
            if (shouldSkip(lastTelemetryEvent.recordedAt, event.recordedAt)) {
                println(
                    "Skipping telemetry event for driver ${event.driverUid} recorded at ${event.recordedAt} as it's too close to the last recorded event at ${lastTelemetryEvent.recordedAt}",
                )
                return
            }
        }

        telemetryEventRepository.insert(TelemetryEventMapper.toEntity(event))

        locationCacheRepository.set(event.driverUid, CoordinatesDto(event.lng, event.lat))

        webSocketHandler.broadcast(event.driverUid, event)
        parcelService.evaluateAll(event)

        println("TelemetryEvent received: ${event.driverUid} - lng : ${event.lng} - lat: ${event.lat} at ${event.recordedAt}")
    }

    fun getLastTelemetryEventByDriver(driverUid: String): TelemetryEvent? =
        telemetryEventRepository.findLatestByDriver(driverUid.toUuid()).orElse(null)

    fun shouldSkip(
        lastRecordedAt: Instant,
        newRecordedAt: Instant,
    ): Boolean = lastRecordedAt.plusSeconds(1).isAfter(newRecordedAt)
}

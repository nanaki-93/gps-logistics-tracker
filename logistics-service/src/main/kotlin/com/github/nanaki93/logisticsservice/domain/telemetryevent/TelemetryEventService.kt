package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.config.EventMetrics
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
    val eventMetrics: EventMetrics
) {
    fun processTelemetryEvent(event: TelemetryEventPlainDto) {

        getLastTelemetryEventByDriver(event.driverUid)?.let { lastTelemetryEvent ->
            if (shouldSkip(lastTelemetryEvent.recordedAt, event.recordedAt)) {
                eventMetrics.incrementProcessed("Skipped")
                println(
                    "Skipping telemetry event for driver ${event.driverUid} recorded at ${event.recordedAt} as it's too close to the last recorded event at ${lastTelemetryEvent.recordedAt}",
                )
                return
            }
        }

        telemetryEventRepository.insert(TelemetryEventMapper.toEntity(event))

        locationCacheRepository.set(event.driverUid, event)

        webSocketHandler.broadcast(event.driverUid, event)

        parcelService.evaluateAll(event)

        eventMetrics.incrementProcessed("Success")

    }

    fun getLastTelemetryEventByDriver(driverUid: String): TelemetryEventPlainDto? =
        locationCacheRepository.get(driverUid.toUuid())

    fun shouldSkip(
        lastRecordedAt: Instant,
        newRecordedAt: Instant,
    ): Boolean = lastRecordedAt.plusSeconds(1).isAfter(newRecordedAt)
}

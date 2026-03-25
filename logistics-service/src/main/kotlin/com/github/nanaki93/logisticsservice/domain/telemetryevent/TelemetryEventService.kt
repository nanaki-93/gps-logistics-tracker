package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.config.EventMetrics
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelService
import com.github.nanaki93.logisticsservice.domain.telemetryevent.websocket.TelemetryEventWebSocketHandler
import com.github.nanaki93.logisticsservice.domain.util.logger
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
    val eventMetrics: EventMetrics,
) {
    val log = logger()

    fun processTelemetryEvent(event: TelemetryEventPlainDto) {
        getLastTelemetryEventByDriver(event.driverUid)?.let { lastTelemetryEvent ->
            if (shouldSkip(lastTelemetryEvent.recordedAt, event.recordedAt)) {
                eventMetrics.incrementProcessed("Skipped")
                log.info(
                    "Skipping telemetry event for driver ${event.driverUid} recorded at ${event.recordedAt} as it's too close to the last recorded event at ${lastTelemetryEvent.recordedAt}",
                )
                return
            }
        }

        log.info("Processing telemetry event for driver ${event.driverUid} recorded at ${event.recordedAt}")
        telemetryEventRepository.insert(TelemetryEventMapper.toEntity(event))
        log.info("Telemetry event for driver ${event.driverUid} recorded at ${event.recordedAt} saved")
        locationCacheRepository.set(event.driverUid, event)
        log.info("Location cache for driver ${event.driverUid} updated")
        webSocketHandler.broadcast(event.driverUid, event)
        log.info("Telemetry event for driver ${event.driverUid} broadcasted")
        parcelService.evaluateAll(event)
        log.info("Parcels evaluated")

        eventMetrics.incrementProcessed("Success")
    }

    fun getLastTelemetryEventByDriver(driverUid: String): TelemetryEventPlainDto? = locationCacheRepository.get(driverUid.toUuid())

    fun shouldSkip(
        lastRecordedAt: Instant,
        newRecordedAt: Instant,
    ): Boolean = lastRecordedAt.plusSeconds(1).isAfter(newRecordedAt)
}

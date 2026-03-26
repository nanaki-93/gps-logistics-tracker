package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.config.EventMetrics
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelDistanceSummary
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelService
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelStatusUpdate
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelStatusWebSocketHandler
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
    val mapWsHandler: MapWebSocketHandler,
    val parcelStatusWsHandler: ParcelStatusWebSocketHandler,
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
        mapWsHandler.broadcastMapPosition(event)
        log.info("Telemetry event for driver ${event.driverUid} broadcasted")
        val parcelDistances = distanceToDelivery(event)
        parcelService.evaluateAll(parcelDistances)
        log.info("Parcels evaluated")

        if (parcelStatusWsHandler.hasActiveSubscribers(event.driverUid)) {
            parcelStatusWsHandler.broadcastParcelUpdate(
                event.driverUid,
                ParcelStatusUpdate(
                    driverUid = event.driverUid.toUuid(),
                    parcels =
                        parcelService.getByDriverId(event.driverUid.toUuid()).map {
                            ParcelDistanceSummary(
                                parcelUid = it.parcelUid,
                                status = it.status.name,
                                distanceMetres = parcelDistances.getOrDefault(it.parcelUid, 9999.9),
                            )
                        },
                    updatedAt = Instant.now(),
                ),
            )
        }

        eventMetrics.incrementProcessed("Success")
    }

    fun getLastTelemetryEventByDriver(driverUid: String): TelemetryEventPlainDto? = locationCacheRepository.get(driverUid.toUuid())

    fun distanceToDelivery(event: TelemetryEventPlainDto) = telemetryEventRepository.distanceToDelivery(event)

    fun shouldSkip(
        lastRecordedAt: Instant,
        newRecordedAt: Instant,
    ): Boolean = lastRecordedAt.plusSeconds(1).isAfter(newRecordedAt)
}

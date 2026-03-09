package com.github.nanaki93.logisticsservice.domain.telemetryevent

import org.springframework.stereotype.Service

@Service
class TelemetryEventService {
    fun processTelemetryEvent(event: TelemetryEventInsertDto) {
        // 1. Idempotency check
        // 2. Persist event
        // 3. Update Redis cache
        // 4. Evaluate parcel status
        println("TelemetryEvent received: ${event.driverUid} - lng : ${event.longitude} - lat: ${event.latitude} at ${event.recordedAt}")
    }
}

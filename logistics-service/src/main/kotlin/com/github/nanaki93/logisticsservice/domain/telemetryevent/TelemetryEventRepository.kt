package com.github.nanaki93.logisticsservice.domain.telemetryevent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface TelemetryEventRepository : JpaRepository<TelemetryEvent, UUID> {
    fun findByDriverUidOrderByRecordedAtDesc(driverUid: UUID): List<TelemetryEvent>

    @Query("SELECT t FROM TelemetryEvent t WHERE t.driverUid = :driverUid ORDER BY t.recordedAt DESC LIMIT 1")
    fun findLastEventByDriverUid(driverUid: UUID): Optional<TelemetryEvent>
}

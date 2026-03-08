package com.github.nanaki93.logisticsservice.domain.telemetryevent

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TelemetryEventRepository : JpaRepository<TelemetryEvent, UUID>

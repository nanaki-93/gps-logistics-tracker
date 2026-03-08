package com.github.nanaki93.logisticsservice.domain.parcel

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StatusHistoryRepository : JpaRepository<StatusHistory, UUID>

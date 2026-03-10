package com.github.nanaki93.logisticsservice.domain.parcel

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ParcelRepository : JpaRepository<Parcel, UUID> {
    fun findByDriverUid(driverUid: UUID): List<Parcel>
}

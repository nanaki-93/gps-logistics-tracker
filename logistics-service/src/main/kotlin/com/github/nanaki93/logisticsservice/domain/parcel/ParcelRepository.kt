package com.github.nanaki93.logisticsservice.domain.parcel

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ParcelRepository : JpaRepository<Parcel, UUID> {
    fun findByDriverUid(driverUid: UUID): List<Parcel>

    @Modifying(flushAutomatically = true)
    @Query("UPDATE Parcel p SET p.status = :status WHERE p.parcelUid = :parcelUid")
    fun updateStatus(
        parcelUid: UUID,
        status: ParcelStatus,
    ): Parcel

    fun findAllByStatus(
        status: ParcelStatus,
        pageable: Pageable,
    ): Page<Parcel>
}

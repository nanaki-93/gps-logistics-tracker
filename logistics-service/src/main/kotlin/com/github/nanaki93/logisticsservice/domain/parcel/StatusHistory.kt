package com.github.nanaki93.logisticsservice.domain.parcel

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "status_history")
class StatusHistory(
    @Id
    val statusHistoryUid: UUID = UUID.randomUUID(),
    val parcelUid: UUID,
    @Enumerated(EnumType.STRING)
    val status: ParcelStatus,
    @Enumerated(EnumType.STRING)
    val oldStatus: ParcelStatus? = null,
    val tsFrom: Instant,
    val tsTo: Instant? = null,
    val reason: String? = null,
) {
    fun close() =
        StatusHistory(
            statusHistoryUid = statusHistoryUid,
            parcelUid = parcelUid,
            status = status,
            oldStatus = status,
            tsFrom = tsFrom,
            tsTo = Instant.now(),
            reason = reason,
        )
}

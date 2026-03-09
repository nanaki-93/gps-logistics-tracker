package com.github.nanaki93.logisticsservice.domain.parcel

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "parcel")
class Parcel(
    @Id
    val parcelUid: UUID = UUID.randomUUID(),
    val routeUid: UUID,
    val driverUid: UUID? = null,
    val trackingCode: String,
    val senderUid: UUID,
    val receiverUid: UUID,
    @Enumerated(EnumType.STRING)
    val status: ParcelStatus,
    val pickupTime: Instant? = null,
    val dropoffTime: Instant? = null,
)

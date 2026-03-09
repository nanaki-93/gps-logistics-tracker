package com.github.nanaki93.logisticsservice.domain.parcel

import java.time.Instant

data class StatusHistoryDto(
    val parcel: ParcelDto,
    val status: ParcelStatus,
    val oldStatus: ParcelStatus?,
    val tsFrom: Instant,
    val tsTo: Instant?,
    val reason: String?,
)

data class StatusHistoryInsertDto(
    val parcelId: String,
    val status: ParcelStatus,
    val oldStatus: ParcelStatus?,
    val tsFrom: Instant,
    val tsTo: Instant?,
    val reason: String?,
)

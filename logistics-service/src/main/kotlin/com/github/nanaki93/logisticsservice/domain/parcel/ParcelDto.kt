package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.route.RouteDto
import java.time.Instant

data class ParcelDto(
    val route: RouteDto,
    val driver: DriverDto?,
    val trackingCode: String,
    val sender: AddressDto,
    val receiver: AddressDto,
    val status: ParcelStatus,
    val pickupTime: Instant?,
    val dropoffTime: Instant?,
)

data class ParcelInsertDto(
    val routeUid: String,
    val trackingCode: String,
    val senderUid: String,
    val receiverUid: String,
)

data class ParcelAssignDto(
    val parcelUid: String,
    val driverUid: String,
)

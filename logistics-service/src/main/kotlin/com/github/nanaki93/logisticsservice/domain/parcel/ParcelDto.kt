package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.route.RouteDto
import java.time.Instant

data class ParcelDto(
    val parcelUid: String? = null,
    val route: RouteDto,
    val driver: DriverDto?,
    val trackingCode: String,
    val sender: AddressDto,
    val receiver: AddressDto,
    val status: ParcelStatus,
    val pickupTime: Instant?,
    val dropoffTime: Instant?,
)

data class ParcelCreateDto(
    val parcelUid: String? = null,
    val route: RouteDto,
    val trackingCode: String,
    val sender: AddressDto,
    val receiver: AddressDto,
)

data class ParcelAssignDto(
    val parcelUid: String,
    val driverUid: String,
)

package com.github.nanaki93.logisticsservice.domain.address

import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto

data class AddressDto(
    val addressUid: String? = null,
    val fullName: String,
    val coordinates: CoordinatesDto,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val details: String,
)

package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressDto

data class RouteDto(
    val origin: AddressDto,
    val destination: AddressDto,
    val waypoints: Map<String, Any>,
)

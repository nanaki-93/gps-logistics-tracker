package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto

data class RouteDto(
    val routeUid: String? = null,
    val origin: AddressDto,
    val destination: AddressDto,
    val waypoints: List<WaypointDto>,
)

data class WaypointDto(
    val order: Int,
    val coordinates: CoordinatesDto,
    val label: String? = null,
)

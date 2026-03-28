package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.util.toUuid

object RouteMapper {
    fun toDto(
        route: Route,
        origin: AddressDto,
        destination: AddressDto,
    ): RouteDto =
        RouteDto(
            routeUid = route.routeUid.toString(),
            origin = origin,
            destination = destination,
            waypoints = route.waypoints.map { WaypointMapper.toDto(it) },
        )

    fun toEntity(routeDto: RouteDto): Route =
        Route(
            originUid = routeDto.origin.addressUid!!.toUuid(),
            destinationUid = routeDto.destination.addressUid!!.toUuid(),
            waypoints = routeDto.waypoints.map { WaypointMapper.toEntity(it) },
        )
}

object WaypointMapper {
    fun toDto(waypoint: Waypoint): WaypointDto =
        WaypointDto(
            order = waypoint.order,
            coordinates = waypoint.coordinates,
            label = waypoint.label,
        )

    fun toEntity(waypointDto: WaypointDto): Waypoint =
        Waypoint(
            coordinates = waypointDto.coordinates,
            order = waypointDto.order,
            label = waypointDto.label,
        )
}

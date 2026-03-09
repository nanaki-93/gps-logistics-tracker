package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto
import com.github.nanaki93.logisticsservice.domain.util.toUuid

object RouteMapper {
    fun toDto(
        route: Route,
        origin: AddressDto,
        destination: AddressDto,
    ): RouteDto =
        RouteDto(
            origin = origin,
            destination = destination,
            waypoints = route.waypoints.map { WaypointMapper.toDto(it) },
        )

    fun toEntity(routeDto: RouteInsertDto): Route =
        Route(
            originUid = routeDto.originId.toUuid(),
            destinationUid = routeDto.destinationId.toUuid(),
            waypoints = routeDto.waypoints.map { WaypointMapper.toEntity(it) },
        )
}

object WaypointMapper {
    fun toDto(waypoint: Waypoint): WaypointDto =
        WaypointDto(
            order = waypoint.order,
            coordinates = Pair(waypoint.lng, waypoint.lat).toCoordinatesDto(),
            label = waypoint.label,
        )

    fun toEntity(waypointDto: WaypointDto): Waypoint =
        Waypoint(
            lng = waypointDto.coordinates.lng,
            lat = waypointDto.coordinates.lat,
            order = waypointDto.order,
            label = waypointDto.label,
        )
}

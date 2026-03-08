package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import java.util.UUID

object RouteMapper {
    fun toDto(
        route: Route,
        origin: AddressDto,
        destination: AddressDto,
    ): RouteDto =
        RouteDto(
            origin = origin,
            destination = destination,
            waypoints = route.waypoints,
        )

    fun toEntity(
        routeDto: RouteDto,
        originUid: UUID,
        destinationUid: UUID,
    ): Route =
        Route(
            originUid = originUid,
            destinationUid = destinationUid,
            waypoints = routeDto.waypoints,
        )
}

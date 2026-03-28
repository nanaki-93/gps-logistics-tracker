package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RouteService(
    val routeRepository: RouteRepository,
    val addressService: AddressService,
) {
    fun create(routeDto: RouteDto): RouteDto {
        val origin = addressService.create(routeDto.origin)
        val destination = addressService.create(routeDto.destination)

        // todo write right check if origin and destination are different
        if (origin.coordinates == destination.coordinates) throw IllegalArgumentException("Origin and destination cannot be the same")
        return routeRepository
            .save(
                RouteMapper.toEntity(
                    RouteDto(
                        origin = origin,
                        destination = destination,
                        waypoints = routeDto.waypoints,
                    ),
                ),
            ).let { RouteMapper.toDto(it, origin, destination) }
    }

    fun getRouteById(routeUid: UUID): RouteDto {
        val route = routeRepository.findById(routeUid).orElseThrow { IllegalArgumentException("Route not found") }
        val origin = addressService.getByUId(route.originUid)
        val destination = addressService.getByUId(route.destinationUid)
        return RouteMapper.toDto(route, origin, destination)
    }

    fun delete(routeUid: UUID) {
        // todo check if route has no parcels
        routeRepository.deleteById(routeUid)
    }

    fun getDeviation(
        routeUid: UUID,
        currentLng: Double,
        currentLat: Double,
    ): Double {
        val route = routeRepository.findById(routeUid).orElseThrow { IllegalArgumentException("Route not found") }
        val waypoints = route.waypoints
        if (waypoints.isEmpty()) throw IllegalArgumentException("Route has no waypoints")
        // todo ST_Distance native query against the route's planned waypoints, returns deviation in metres
        return 1.0
    }
}

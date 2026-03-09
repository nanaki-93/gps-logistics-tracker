package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.address.AddressMapper
import com.github.nanaki93.logisticsservice.domain.address.AddressRepository
import com.github.nanaki93.logisticsservice.domain.util.toUuid
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RouteService(
    val routeRepository: RouteRepository,
    val addressRepository: AddressRepository,
) {
    fun create(routeDto: RouteInsertDto) {
        val origin =
            addressRepository
                .findById(
                    routeDto.originId.toUuid(),
                ).orElseThrow { IllegalArgumentException("Origin address not found") }
        val destination =
            addressRepository
                .findById(
                    routeDto.destinationId.toUuid(),
                ).orElseThrow { IllegalArgumentException("Destination address not found") }

        // todo write right check if origin and destination are different
        if (origin == destination) throw IllegalArgumentException("Origin and destination cannot be the same")
        routeRepository.save(RouteMapper.toEntity(routeDto))
    }

    fun validate(routeUid: UUID): Boolean = routeRepository.findById(routeUid).isPresent

    fun getRouteById(routeUid: UUID): RouteDto {
        val route = routeRepository.findById(routeUid).orElseThrow { IllegalArgumentException("Route not found") }
        val origin = addressRepository.findById(route.originUid).orElseThrow { IllegalArgumentException("Origin address not found") }
        val destination =
            addressRepository
                .findById(
                    route.destinationUid,
                ).orElseThrow { IllegalArgumentException("Destination address not found") }
        return RouteMapper.toDto(route, AddressMapper.toDto(origin), AddressMapper.toDto(destination))
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

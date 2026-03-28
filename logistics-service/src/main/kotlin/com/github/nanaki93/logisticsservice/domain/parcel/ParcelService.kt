package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressService
import com.github.nanaki93.logisticsservice.domain.driver.DriverService
import com.github.nanaki93.logisticsservice.domain.route.RouteService
import com.github.nanaki93.logisticsservice.domain.util.toUuid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ParcelService(
    val parcelRepository: ParcelRepository,
    val statusHistoryService: StatusHistoryService,
    val routeService: RouteService,
    val addressService: AddressService,
    val driverService: DriverService,
) {
    fun create(parcelDto: ParcelCreateDto) : ParcelDto{
        val sender = addressService.create(parcelDto.sender)
        val receiver = addressService.create(parcelDto.receiver)
        val route = routeService.create(parcelDto.route)

        val parcel =
            ParcelMapper.toInsertEntity(
                ParcelCreateDto(
                    sender = sender,
                    receiver = receiver,
                    route = route,
                    trackingCode = parcelDto.trackingCode
                ),
            )
        parcelRepository.save(parcel)
        statusHistoryService.createHistory(parcel)
        return ParcelMapper.toDto(
            parcel = parcel,
            route = route,
            driver = null,
            sender = sender,
            receiver = receiver,
        )
    }

    fun getAll(pageable : Pageable): Page<ParcelDto> =

        parcelRepository.findAll(pageable).map {
            ParcelMapper.toDto(
                parcel = it,
                route = routeService.getRouteById(it.routeUid),
                driver = it.driverUid?.let { uid -> driverService.getByUId(uid) },
                sender = addressService.getByUId(it.senderUid),
                receiver = addressService.getByUId(it.receiverUid),
            )
        }
    fun getAllByStatus(status: ParcelStatus, pageable: Pageable): Page<ParcelDto> =


        parcelRepository.findAllByStatus(status,pageable).map {
            ParcelMapper.toDto(
                parcel = it,
                route = routeService.getRouteById(it.routeUid),
                driver = it.driverUid?.let { uid -> driverService.getByUId(uid) },
                sender = addressService.getByUId(it.senderUid),
                receiver = addressService.getByUId(it.receiverUid),
            )
        }

    fun getByDriverId(driverUid: UUID): List<Parcel> = parcelRepository.findByDriverUid(driverUid)

    fun getByUid(parcelUid: UUID): ParcelDto =
        parcelRepository
            .findById(parcelUid)
            .orElseThrow { IllegalArgumentException("Parcel not found") }
            .let {
                ParcelMapper.toDto(
                    parcel = it,
                    route = routeService.getRouteById(it.routeUid),
                    driver = it.driverUid?.let { uid -> driverService.getByUId(uid) },
                    sender = addressService.getByUId(it.senderUid),
                    receiver = addressService.getByUId(it.receiverUid),
                )
            }

    fun getEntityById(parcelUid: UUID): Parcel =
        parcelRepository
            .findById(parcelUid)
            .orElseThrow { IllegalArgumentException("Parcel not found") }

    fun assign(parcelAssignDto: ParcelAssignDto) {
        val parcel = getEntityById(parcelAssignDto.parcelUid.toUuid())
        if (parcel.driverUid != null) throw IllegalArgumentException("Parcel is already assigned")
        if (!driverService.isValid(parcelAssignDto.driverUid.toUuid())) throw IllegalArgumentException("Driver not found")

        parcelRepository.save(parcel.assign(parcelAssignDto.driverUid.toUuid()))
    }

    fun evaluateAll(distances: Map<UUID, Double>) {
        val toUpdate = mutableListOf<Parcel>()
        distances.forEach { (parcelUid, distance) ->
            when (distance) {
                in 0.0..<50.0 -> toUpdate.add(getEntityById(parcelUid).withStatus(ParcelStatus.DELIVERED))
                in 50.0..500.0 -> toUpdate.add(getEntityById(parcelUid).withStatus(ParcelStatus.ARRIVING))
                else -> return@forEach
            }
        }
        parcelRepository.saveAll(toUpdate)
    }
}

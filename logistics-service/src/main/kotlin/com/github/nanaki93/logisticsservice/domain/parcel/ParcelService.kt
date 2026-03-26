package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressService
import com.github.nanaki93.logisticsservice.domain.driver.DriverService
import com.github.nanaki93.logisticsservice.domain.route.RouteService
import com.github.nanaki93.logisticsservice.domain.util.toUuid
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
    fun create(parcelDto: ParcelInsertDto) {
        if (!routeService.validate(parcelDto.routeUid.toUuid()) ||
            !addressService.validate(parcelDto.senderUid.toUuid()) ||
            !addressService.validate(parcelDto.receiverUid.toUuid())
        ) {
            throw IllegalArgumentException("Invalid parcel data")
        }

        val parcel = ParcelMapper.toInsertEntity(parcelDto)
        parcelRepository.save(parcel)
        statusHistoryService.createHistory(parcel)
    }

    fun getAll(): List<ParcelDto> =
        parcelRepository.findAll().map {
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

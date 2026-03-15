package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressService
import com.github.nanaki93.logisticsservice.domain.driver.DriverService
import com.github.nanaki93.logisticsservice.domain.route.RouteService
import com.github.nanaki93.logisticsservice.domain.telemetryevent.TelemetryEventPlainDto
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

    fun assign(parcelAssignDto: ParcelAssignDto) {
        val parcel =
            parcelRepository
                .findById(
                    parcelAssignDto.parcelUid.toUuid(),
                ).orElseThrow { IllegalArgumentException("Parcel not found") }
        if (parcel.driverUid != null) throw IllegalArgumentException("Parcel is already assigned")
        // fixme change with just a check on the driver
        driverService.getByUId(parcelAssignDto.driverUid.toUuid())

        parcelRepository.save(parcel.assign(parcelAssignDto.driverUid.toUuid()))
    }

    fun evaluateAll(event: TelemetryEventPlainDto) {
        val parcels = parcelRepository.findByDriverUid(event.driverUid.toUuid())
        // calculate deviation
        parcels.forEach { parcel ->
            val deviation = routeService.getDeviation(parcel.routeUid, event.lng, event.lat)
        }
    }
}

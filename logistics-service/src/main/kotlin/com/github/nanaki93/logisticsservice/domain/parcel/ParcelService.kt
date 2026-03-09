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

        val parcel = ParcelMapper.toEntity(parcelDto)
        parcelRepository.save(parcel)
        statusHistoryService.createHistory(parcel)
    }

    fun assign(
        parcelUid: UUID,
        driverUid: UUID,
    ) {
    }
}

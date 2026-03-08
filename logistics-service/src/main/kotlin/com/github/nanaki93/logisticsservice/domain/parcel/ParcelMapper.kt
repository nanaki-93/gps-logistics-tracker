package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.route.RouteDto
import java.util.UUID

object ParcelMapper {
    fun toDto(
        parcel: Parcel,
        route: RouteDto,
        driverDto: DriverDto,
        sender: AddressDto,
        receiver: AddressDto,
    ): ParcelDto =
        ParcelDto(
            route = route,
            driver = driverDto,
            trackingCode = parcel.trackingCode,
            sender = sender,
            receiver = receiver,
            status = parcel.status,
            pickupTime = parcel.pickupTime,
            dropoffTime = parcel.dropoffTime,
        )

    fun toEntity(
        parcelDto: ParcelDto,
        routeUid: UUID,
        driverUid: UUID?,
        senderUid: UUID,
        receiverUid: UUID,
    ): Parcel =
        Parcel(
            routeUid = routeUid,
            driverUid = driverUid,
            senderUid = senderUid,
            receiverUid = receiverUid,
            trackingCode = parcelDto.trackingCode,
            status = parcelDto.status,
            pickupTime = parcelDto.pickupTime,
            dropoffTime = parcelDto.dropoffTime,
        )
}

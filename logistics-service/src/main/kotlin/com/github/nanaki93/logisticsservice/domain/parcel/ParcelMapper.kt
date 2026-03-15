package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.address.AddressDto
import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.route.RouteDto
import com.github.nanaki93.logisticsservice.domain.util.toUuid

object ParcelMapper {
    fun toDto(
        parcel: Parcel,
        route: RouteDto,
        driver: DriverDto?,
        sender: AddressDto,
        receiver: AddressDto,
    ): ParcelDto =
        ParcelDto(
            route = route,
            driver = driver,
            trackingCode = parcel.trackingCode,
            sender = sender,
            receiver = receiver,
            status = parcel.status,
            pickupTime = parcel.pickupTime,
            dropoffTime = parcel.dropoffTime,
        )

    fun toInsertEntity(parcelDto: ParcelInsertDto): Parcel =
        Parcel(
            routeUid = parcelDto.routeUid.toUuid(),
            senderUid = parcelDto.senderUid.toUuid(),
            receiverUid = parcelDto.receiverUid.toUuid(),
            trackingCode = parcelDto.trackingCode,
            status = parcelDto.status,
        )

    fun toEntity(parcelDto: ParcelDto): Parcel =
        Parcel(
            routeUid = parcelDto.route..toUuid(),
            senderUid = parcelDto.senderUid.toUuid(),
            receiverUid = parcelDto.receiverUid.toUuid(),
            trackingCode = parcelDto.trackingCode,
            status = parcelDto.status,
        )
}

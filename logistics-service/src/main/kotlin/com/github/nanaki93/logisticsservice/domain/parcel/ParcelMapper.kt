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
            parcelUid = parcel.parcelUid.toString(),
            route = route,
            driver = driver,
            trackingCode = parcel.trackingCode,
            sender = sender,
            receiver = receiver,
            status = parcel.status,
            pickupTime = parcel.pickupTime,
            dropoffTime = parcel.dropoffTime,
        )

    fun toInsertEntity(parcelDto: ParcelCreateDto): Parcel =
        Parcel(
            routeUid = parcelDto.route.routeUid!!.toUuid(),
            senderUid = parcelDto.sender.addressUid!!.toUuid(),
            receiverUid = parcelDto.receiver.addressUid!!.toUuid(),
            trackingCode = parcelDto.trackingCode,
            status = ParcelStatus.TO_BE_ASSIGNED,
        )
}

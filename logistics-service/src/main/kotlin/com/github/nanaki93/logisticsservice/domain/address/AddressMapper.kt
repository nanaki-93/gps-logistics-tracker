package com.github.nanaki93.logisticsservice.domain.address

import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toJtsPoint
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto
import java.util.UUID

object AddressMapper {
    fun toDto(address: Address): AddressDto =
        AddressDto(
            addressUid = address.addressUid.toString(),
            fullName = address.fullName,
            coordinates = address.coordinates.toCoordinatesDto(),
            street = address.street,
            city = address.city,
            postalCode = address.postalCode,
            country = address.country,
            details = address.details,
        )

    fun toEntity(
        addressDto: AddressDto,
        uid: UUID? = null,
    ): Address =
        Address(
            addressUid = uid ?: UUID.randomUUID(),
            fullName = addressDto.fullName,
            coordinates = addressDto.coordinates.toJtsPoint(),
            street = addressDto.street,
            city = addressDto.city,
            postalCode = addressDto.postalCode,
            country = addressDto.country,
            details = addressDto.details,
        )
}

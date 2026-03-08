package com.github.nanaki93.logisticsservice.domain.address

import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinates
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto

object AddressMapper {
    fun toDto(address: Address): AddressDto =
        AddressDto(
            fullName = address.fullName,
            coordinates = address.coordinates.toCoordinatesDto(),
            street = address.street,
            city = address.city,
            postalCode = address.postalCode,
            country = address.country,
            details = address.details,
        )

    fun toEntity(addressDto: AddressDto): Address =
        Address(
            fullName = addressDto.fullName,
            coordinates = addressDto.coordinates.toCoordinates(),
            street = addressDto.street,
            city = addressDto.city,
            postalCode = addressDto.postalCode,
            country = addressDto.country,
            details = addressDto.details,
        )
}

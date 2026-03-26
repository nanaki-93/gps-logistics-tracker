package com.github.nanaki93.logisticsservice.domain.address

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AddressService(
    val addressRepository: AddressRepository,
) {
    fun create(addressDto: AddressDto) {
        addressRepository.save(AddressMapper.toEntity(addressDto))
    }

    fun getByUId(uid: UUID): AddressDto {
        val address = addressRepository.findById(uid).orElseThrow { IllegalArgumentException("Address not found") }
        return AddressMapper.toDto(address)
    }

    fun delete(uuid: UUID) {
        addressRepository.deleteById(uuid)
    }

    fun validate(uid: UUID) = addressRepository.existsById(uid)
}

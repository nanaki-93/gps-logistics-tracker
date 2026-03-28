package com.github.nanaki93.logisticsservice.domain.driver

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DriverService(
    val driverRepository: DriverRepository,
) {
    fun create(driverDto: DriverDto): Driver = driverRepository.save(DriverMapper.toEntity(driverDto))

    fun getByUId(uid: UUID): DriverDto {
        val driver = driverRepository.findById(uid).orElseThrow { IllegalArgumentException("Driver not found") }
        return DriverMapper.toDto(driver)
    }

    fun isValid(uid: UUID): Boolean = driverRepository.existsById(uid)

    fun getAll(): List<DriverDto> {
        val drivers = driverRepository.findAll()
        return drivers.map { driver ->
            DriverMapper.toDto(driver)
        }
    }

    fun delete(uid: UUID) {
        val driver = driverRepository.findById(uid).orElseThrow { IllegalArgumentException("Driver not found") }
        driverRepository.delete(driver)
    }
}

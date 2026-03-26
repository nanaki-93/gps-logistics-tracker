package com.github.nanaki93.logisticsservice.domain.driver

import java.util.UUID

object DriverMapper {
    fun toDto(
        driver: Driver,
    ): DriverDto =
        DriverDto(
            fullName = driver.fullname,
            email = driver.email,
            phone = driver.phone,
            licenseNumber = driver.licenseNumber,
        )

    fun toEntity(
        driverDto: DriverDto,
    ): Driver =
        Driver(
            fullname = driverDto.fullName,
            email = driverDto.email,
            phone = driverDto.phone,
            licenseNumber = driverDto.licenseNumber,
        )
}

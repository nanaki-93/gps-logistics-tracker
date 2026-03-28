package com.github.nanaki93.logisticsservice.domain.driver

object DriverMapper {
    fun toDto(driver: Driver): DriverDto =
        DriverDto(
            driverUid = driver.driverUid.toString(),
            fullName = driver.fullname,
            email = driver.email,
            phone = driver.phone,
            licenseNumber = driver.licenseNumber,
        )

    fun toEntity(driverDto: DriverDto): Driver =
        Driver(
            fullname = driverDto.fullName,
            email = driverDto.email,
            phone = driverDto.phone,
            licenseNumber = driverDto.licenseNumber,
        )
}

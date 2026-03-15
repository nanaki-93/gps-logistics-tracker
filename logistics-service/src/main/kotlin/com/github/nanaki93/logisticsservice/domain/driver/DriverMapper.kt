package com.github.nanaki93.logisticsservice.domain.driver

import com.github.nanaki93.logisticsservice.domain.vehicle.Vehicle
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleMapper
import java.util.UUID

object DriverMapper {
    fun toDto(
        driver: Driver,
        vehicle: Vehicle? = null,
    ): DriverDto =
        DriverDto(
            vehicle = vehicle?.let { VehicleMapper.toDto(it) },
            fullName = driver.fullname,
            email = driver.email,
            phone = driver.phone,
            licenseNumber = driver.licenseNumber,
        )

    fun toEntity(
        driverDto: DriverDto,
        vehicleUid: UUID? = null,
    ): Driver =
        Driver(
            vehicleUid = vehicleUid,
            fullname = driverDto.fullName,
            email = driverDto.email,
            phone = driverDto.phone,
            licenseNumber = driverDto.licenseNumber,
        )
}

package com.github.nanaki93.logisticsservice.domain.driver

import com.github.nanaki93.logisticsservice.domain.vehicle.Vehicle
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleMapper
import java.util.UUID

object DriverMapper {
    fun toDto(
        driver: Driver,
        vehicle: Vehicle,
    ): DriverDto =
        DriverDto(
            vechicle = VehicleMapper.toDto(vehicle),
            fullName = driver.fullName,
            email = driver.email,
            phone = driver.phone,
            licenseNumber = driver.licenseNumber,
        )

    fun toEntity(
        driverDto: DriverDto,
        vehicleUid: UUID,
    ): Driver =
        Driver(
            vehicleUid = vehicleUid,
            fullName = driverDto.fullName,
            email = driverDto.email,
            phone = driverDto.phone,
            licenseNumber = driverDto.licenseNumber,
        )
}

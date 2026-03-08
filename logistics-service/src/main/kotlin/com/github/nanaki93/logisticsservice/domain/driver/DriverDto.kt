package com.github.nanaki93.logisticsservice.domain.driver

import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleDto

data class DriverDto(
    val vechicle: VehicleDto,
    val fullName: String,
    val email: String,
    val phone: String,
    val licenseNumber: String,
)

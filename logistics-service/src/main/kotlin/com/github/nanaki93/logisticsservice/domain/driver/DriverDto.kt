package com.github.nanaki93.logisticsservice.domain.driver

data class DriverDto(
    val driverUid: String? = null,
    val fullName: String,
    val email: String,
    val phone: String,
    val licenseNumber: String,
)

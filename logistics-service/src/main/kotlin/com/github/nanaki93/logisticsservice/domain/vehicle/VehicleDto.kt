package com.github.nanaki93.logisticsservice.domain.vehicle

data class VehicleDto(
    val plateNumber: String,
    val model: String?,
    val type: VehicleType,
    val active: Boolean,
)

package com.github.nanaki93.logisticsservice.domain.vehicle

object VehicleMapper {
    fun toDto(vehicle: Vehicle): VehicleDto =
        VehicleDto(
            plateNumber = vehicle.plateNumber,
            model = vehicle.model,
            type = vehicle.type,
        )

    fun toEntity(vehicleDto: VehicleDto): Vehicle =
        Vehicle(
            plateNumber = vehicleDto.plateNumber,
            model = vehicleDto.model,
            type = vehicleDto.type,
        )
}

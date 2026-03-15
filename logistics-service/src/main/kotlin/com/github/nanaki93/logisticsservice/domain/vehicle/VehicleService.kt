package com.github.nanaki93.logisticsservice.domain.vehicle

import com.github.nanaki93.logisticsservice.domain.driver.DriverService
import com.github.nanaki93.logisticsservice.domain.util.toUuid
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VehicleService(
    val vehicleRepository: VehicleRepository,
    val driverService: DriverService,
) {
    fun create(vehicleDto: VehicleDto): VehicleDto {
        val vehicle = VehicleMapper.toEntity(vehicleDto)
        val savedVehicle = vehicleRepository.save(vehicle)
        if (vehicleDto.driverUid != null) driverService.assignVehicle(savedVehicle.vehicleUid, vehicleDto.driverUid.toUuid())
        return VehicleMapper.toDto(savedVehicle)
    }

    fun getById(uid: UUID): VehicleDto {
        val vehicle =
            vehicleRepository.findById(uid).orElseThrow {
                IllegalArgumentException("Vehicle with uid $uid not found")
            }
        return VehicleMapper.toDto(vehicle)
    }

    fun getAll(): List<VehicleDto> = vehicleRepository.findAll().map(VehicleMapper::toDto)

    fun deactivate(uid: UUID) {
        val vehicle =
            vehicleRepository.findById(uid).orElseThrow {
                IllegalArgumentException("Vehicle with uid $uid not found")
            }
        vehicleRepository.save(vehicle.deactivate())
    }

    fun activate(uid: UUID) {
        val vehicle =
            vehicleRepository.findById(uid).orElseThrow {
                IllegalArgumentException("Vehicle with uid $uid not found")
            }
        vehicleRepository.save(vehicle.activate())
    }
}

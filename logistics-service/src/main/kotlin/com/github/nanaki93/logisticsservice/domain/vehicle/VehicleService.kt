package com.github.nanaki93.logisticsservice.domain.vehicle

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VehicleService(
    val vehicleRepository: VehicleRepository,
) {
    fun register(vehicleDto: VehicleDto): VehicleDto {
        val vehicle = VehicleMapper.toEntity(vehicleDto)
        val savedVehicle = vehicleRepository.save(vehicle)
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

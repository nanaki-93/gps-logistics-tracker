package com.github.nanaki93.logisticsservice.domain.driver

import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class DriverService(
    val driverRepository: DriverRepository,
    val vehicleRepository: VehicleRepository,
) {
    fun create(driverDto: DriverDto): Driver = driverRepository.save(DriverMapper.toEntity(driverDto))

    fun getByUId(uid: UUID): DriverDto {
        val driver = driverRepository.findById(uid).orElseThrow { IllegalArgumentException("Driver not found") }
        val vehicle =
            driver.vehicleUid.let {
                vehicleRepository
                    .findById(
                        uid,
                    ).orElseThrow { IllegalArgumentException("Vehicle not found") }
            }
        return DriverMapper.toDto(driver, vehicle)
    }

    fun getAll(): List<DriverDto> {
        val drivers = driverRepository.findAll()
        return drivers.map { driver ->
            val vehicle =
                driver.vehicleUid?.let {
                    vehicleRepository
                        .findById(
                            it,
                        ).orElseThrow { IllegalArgumentException("Vehicle not found") }
                }
            DriverMapper.toDto(driver, vehicle)
        }
    }

    fun delete(uid: UUID) {
        val driver = driverRepository.findById(uid).orElseThrow { IllegalArgumentException("Driver not found") }
        driverRepository.delete(driver)
    }

    fun isVehicleAssigned(vehicleUid: UUID): Boolean {
        val driver = driverRepository.findByVehicleUid(vehicleUid).orElseThrow { IllegalArgumentException("Driver not found") }
        return driver.vehicleUid != null
    }

    fun assignVehicle(
        uid: UUID,
        vehicleUid: UUID,
    ) {
        val driver = driverRepository.findById(uid).orElseThrow { IllegalArgumentException("Driver not found") }
        val vehicle = vehicleRepository.findById(vehicleUid).orElseThrow { IllegalArgumentException("Vehicle not found") }
        if (!vehicle.active) {
            throw IllegalArgumentException("Vehicle is not active")
        }
        if (isVehicleAssigned(vehicleUid)) throw IllegalArgumentException("Vehicle is already assigned")
        driverRepository.save(driver.assign(vehicleUid))
    }

    fun unassignVehicle(uid: UUID) {
        // todo check parcels
        val driver = driverRepository.findById(uid).orElseThrow { IllegalArgumentException("Driver not found") }
        driverRepository.save(driver.unassign())
    }
}

package com.github.nanaki93.logisticsservice.api

import com.github.nanaki93.logisticsservice.domain.driver.Driver
import com.github.nanaki93.logisticsservice.domain.driver.DriverRepository
import com.github.nanaki93.logisticsservice.domain.vehicle.Vehicle
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class DriverController(
    val driverRepository: DriverRepository,
    val vehicleRepository: VehicleRepository,
) {
    @GetMapping("/drivers")
    fun getDrivers(): List<Driver> = driverRepository.findAll()

    @PostMapping("/vehicle")
    fun saveVehicle(
        @RequestBody vehicle: Vehicle,
    ): Vehicle = vehicleRepository.save(vehicle)

    @PostMapping("/driver")
    fun saveDriver(
        @RequestBody driver: Driver,
    ): Driver = driverRepository.save(driver)
}

/**
 * Api:
 * parcel: get all parcels
 * parcel: get a parcel by id
 * parcel: insert a new parcel
 * parcel: change status of a parcel
 *
 *telemetry: get telemetry data for a vehicle
 *
 *
 */

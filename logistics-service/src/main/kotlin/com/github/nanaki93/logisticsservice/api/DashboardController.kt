package com.github.nanaki93.logisticsservice.api

import com.github.nanaki93.logisticsservice.domain.driver.Driver
import com.github.nanaki93.logisticsservice.domain.driver.DriverDto
import com.github.nanaki93.logisticsservice.domain.driver.DriverService
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelAssignDto
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelDto
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelInsertDto
import com.github.nanaki93.logisticsservice.domain.parcel.ParcelService
import com.github.nanaki93.logisticsservice.domain.util.toUuid
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleDto
import com.github.nanaki93.logisticsservice.domain.vehicle.VehicleService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/dashboard")
class DashboardController(
    val driverService: DriverService,
    val vehicleService: VehicleService,
    val parcelService: ParcelService,
) {
    @GetMapping("/test-header")
    fun handle(@RequestHeader headers: Map<String, String>) {
        headers.forEach { (k, v) -> println("$k: $v") }
    }
    @GetMapping("drivers")
    fun getDrivers(): List<DriverDto> = driverService.getAll()

    @GetMapping("driver/{driverUid}")
    fun getDriver(
        @PathVariable driverUid: String,
    ): DriverDto = driverService.getByUId(driverUid.toUuid())

    @PostMapping("driver")
    fun insertDriver(
        @RequestBody driver: DriverDto,
    ) : Driver = driverService.create(driver)

    @DeleteMapping("driver/{driverUid}")
    fun deleteDriver(
        @PathVariable driverUid: String,
    ) : Unit = driverService.delete(driverUid.toUuid())

    @PostMapping("vehicle")
    fun insertVehicle(
        @RequestBody vehicle: VehicleDto,
    ) = vehicleService.create(vehicle)

    @GetMapping("vehicles")
    fun getVehicles(): List<VehicleDto> = vehicleService.getAll()

    @GetMapping("vehicle/{vehicleUid}")
    fun getVehicle(
        @PathVariable vehicleUid: String,
    ): VehicleDto = vehicleService.getById(vehicleUid.toUuid())

    @PostMapping("parcel")
    fun insertParcel(
        @RequestBody parcel: ParcelInsertDto,
    ) = parcelService.create(parcel)

    @GetMapping("parcels")
    fun getParcels(): List<ParcelDto> = parcelService.getAll()

    @GetMapping("parcel/{parcelUid}")
    fun getParcel(
        @PathVariable parcelUid: String,
    ): ParcelDto = parcelService.getByUid(parcelUid.toUuid())

    @PostMapping("parcel/assign")
    fun assignParcel(
        @RequestBody parcelAssignDto: ParcelAssignDto,
    ) = parcelService.assign(parcelAssignDto)
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

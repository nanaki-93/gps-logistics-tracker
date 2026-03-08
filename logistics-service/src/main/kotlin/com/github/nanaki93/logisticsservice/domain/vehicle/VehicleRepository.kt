package com.github.nanaki93.logisticsservice.domain.vehicle

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VehicleRepository : JpaRepository<Vehicle, UUID> {
    fun save(vehicle: Vehicle): Vehicle
}

package com.github.nanaki93.logisticsservice.domain.vehicle

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "vehicle")
class Vehicle(
    @Id
    val vehicleUid: UUID = UUID.randomUUID(),
    val plateNumber: String,
    val model: String?,
    @Enumerated(EnumType.STRING)
    val type: VehicleType,
    val active: Boolean = true,
) {
    fun activate() =
        Vehicle(
            vehicleUid = vehicleUid,
            plateNumber = plateNumber,
            model = model,
            type = type,
            active = true,
        )

    fun deactivate() =
        Vehicle(
            vehicleUid = vehicleUid,
            plateNumber = plateNumber,
            model = model,
            type = type,
            active = false,
        )
}

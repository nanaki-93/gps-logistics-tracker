package com.github.nanaki93.logisticsservice.domain.driver

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "driver")
class Driver(
    @Id
    val uuid: UUID = UUID.randomUUID(),
    val vehicleUid: UUID? = null,
    val fullName: String,
    val email: String,
    val phone: String,
    val licenseNumber: String,
) {
    fun assign(vehicleUid: UUID) =
        Driver(
            uuid = uuid,
            vehicleUid = vehicleUid,
            fullName = fullName,
            email = email,
            phone = phone,
            licenseNumber = licenseNumber,
        )

    fun unassign() =
        Driver(
            uuid = uuid,
            fullName = fullName,
            email = email,
            phone = phone,
            licenseNumber = licenseNumber,
            vehicleUid = null,
        )
}

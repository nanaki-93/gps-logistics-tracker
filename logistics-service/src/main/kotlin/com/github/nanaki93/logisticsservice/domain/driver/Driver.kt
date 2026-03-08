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
    val vehicleUid: UUID,
    val fullName: String,
    val email: String,
    val phone: String,
    val licenseNumber: String,
)

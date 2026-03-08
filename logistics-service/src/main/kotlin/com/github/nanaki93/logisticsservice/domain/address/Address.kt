package com.github.nanaki93.logisticsservice.domain.address

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "address")
class Address(
    @Id
    val addressUid: UUID = UUID.randomUUID(),
    val fullName: String,
    @Column(columnDefinition = "geography(POINT,4326)")
    val coordinates: String,
    val street: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val details: String,
)

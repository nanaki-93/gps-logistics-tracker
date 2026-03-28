package com.github.nanaki93.logisticsservice.domain.driver

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface DriverRepository : JpaRepository<Driver, UUID> {
    fun save(driver: Driver): Driver
}

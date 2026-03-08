package com.github.nanaki93.logisticsservice.domain.route

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RouteRepository : JpaRepository<Route, UUID>

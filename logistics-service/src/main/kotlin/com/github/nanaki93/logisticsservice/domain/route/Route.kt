package com.github.nanaki93.logisticsservice.domain.route

import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

@Entity
@Table(name = "route")
class Route(
    @Id
    val routeUid: UUID = UUID.randomUUID(),
    val originUid: UUID,
    val destinationUid: UUID,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val waypoints: List<Waypoint> = emptyList(),
)

data class Waypoint(
    val order: Int = 0,
    val coordinates: CoordinatesDto = CoordinatesDto(0.0, 0.0),
    val label: String? = null,
)

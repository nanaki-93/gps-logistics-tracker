package com.github.nanaki93.logisticsservice.domain.telemetryevent

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.util.Optional
import java.util.UUID

@Repository
class TelemetryEventRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun insert(event: TelemetryEvent) {
        val sql = """
            INSERT INTO telemetry_event 
                (telemetry_event_uid, driver_uid, coordinates, recorded_at)
            VALUES 
                (:uid, :driverUid, ST_GeogFromText(:wkt), :recordedAt)
        """
        val params =
            MapSqlParameterSource()
                .addValue("uid", event.telemetryEventUid)
                .addValue("driverUid", event.driverUid)
                .addValue("wkt", event.coordinates) // "POINT(139.69 35.68)"
                .addValue("recordedAt", Timestamp.from(event.recordedAt))

        jdbc.update(sql, params)
    }

    fun findLatestByDriver(driverUid: UUID): Optional<TelemetryEvent> {
        val sql = """
            SELECT 
                telemetry_event_uid,
                driver_uid,
                ST_AsText(coordinates) AS coordinates,
                recorded_at
            FROM telemetry_event
            WHERE driver_uid = :driverUid
            ORDER BY recorded_at DESC
            LIMIT 1
        """
        return try {
            jdbc
                .queryForObject(
                    sql,
                    MapSqlParameterSource("driverUid", driverUid),
                ) { rs, _ ->
                    TelemetryEvent(
                        telemetryEventUid = UUID.fromString(rs.getString("telemetry_event_uid")),
                        driverUid = UUID.fromString(rs.getString("driver_uid")),
                        coordinates = rs.getString("coordinates"),
                        recordedAt = rs.getTimestamp("recorded_at").toInstant(),
                    )
                }.let { Optional.of(it) }
        } catch (e: EmptyResultDataAccessException) {
            Optional.empty<TelemetryEvent>() // no events yet for this vehicle
        }
    }
}

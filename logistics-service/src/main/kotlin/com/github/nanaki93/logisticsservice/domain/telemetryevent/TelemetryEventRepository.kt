package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil
import com.github.nanaki93.logisticsservice.domain.util.GeographyUtil.toCoordinatesDto
import com.github.nanaki93.logisticsservice.domain.util.toUuid
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
            Optional.empty<TelemetryEvent>() // no events yet for this driver
        }
    }

    fun distanceToDelivery(event: TelemetryEventPlainDto): Map<UUID, Double> {
        val sql = """
                    SELECT
                        p.parcel_uid,
                        ST_Distance(
                            ST_GeogFromText(:eventWkt),
                            a.coordinates::geography
                        ) AS distance_metres
                    FROM parcel p
                    JOIN address a ON a.address_uid = p.receiver_uid
                    WHERE p.driver_uid = :driverUid
                      AND p.status NOT IN ('DELIVERED', 'CANCELLED')
                    ORDER BY distance_metres ASC
        """

        val params =
            MapSqlParameterSource().apply {
                addValue("driverUid", event.driverUid)
                addValue("eventWkt", GeographyUtil.toWkt(Pair(event.lat, event.lng).toCoordinatesDto()))
            }
        return jdbc
            .query(sql, params) { rs, _ ->
                rs.getString("parcel_uid").toUuid() to rs.getDouble("distance_metres")
            }.toMap()
    }
}

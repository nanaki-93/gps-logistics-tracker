package com.github.nanaki93.logisticsservice.domain.util

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

data class CoordinatesDto(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)

object GeographyUtil {
    fun toWkt(coord: CoordinatesDto) = "POINT(${coord.lng} ${coord.lat})"

    fun fromWkt(wkt: String): CoordinatesDto =
        wkt
            .removePrefix("POINT(")
            .removeSuffix(")")
            .trim()
            .split(Regex("\\s+"))
            .let { CoordinatesDto(lat = it[1].toDouble(), lng = it[0].toDouble()) }

    fun CoordinatesDto.toJtsPoint() = GeoFactory.point(lat, lng)

    fun String.toCoordinatesDto() = fromWkt(this)

    fun Point.toCoordinatesDto() = CoordinatesDto(lat = y, lng = x)

    fun Pair<Double, Double>.toJtsPoint() = toWkt(CoordinatesDto(lat = first, lng = second))
}

object GeoFactory {
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    fun point(
        lat: Double,
        lng: Double,
    ): Point {
        val point = geometryFactory.createPoint(Coordinate(lng, lat))
        point.srid = 4326
        return point
    }
}

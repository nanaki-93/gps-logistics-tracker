package com.github.nanaki93.logisticsservice.domain.util

data class CoordinatesDto(
    val lat: Double,
    val lng: Double,
)

object GeographyUtil {
    fun toWkt(coord: CoordinatesDto) = "POINT(${coord.lng} ${coord.lat})"

    fun fromWkt(wkt: String): CoordinatesDto =
        wkt
            .removePrefix("POINT(")
            .removeSuffix(")")
            .trim()
            .split(" ")
            .let { CoordinatesDto(it[1].toDouble(), it[2].toDouble()) }

    fun CoordinatesDto.toCoordinates() = toWkt(this)

    fun String.toCoordinatesDto() = fromWkt(this)
}

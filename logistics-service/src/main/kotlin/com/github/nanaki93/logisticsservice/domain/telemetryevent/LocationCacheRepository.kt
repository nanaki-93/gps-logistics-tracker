package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.util.CoordinatesDto
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.util.UUID

@Repository
class LocationCacheRepository(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val ttl = Duration.ofMinutes(5)
    private val keyPrefix = "last_loc:"

    fun set(
        driverUid: String,
        point: CoordinatesDto,
    ) = redisTemplate.opsForValue().set(
        keyPrefix + driverUid,
        objectMapper.writeValueAsString(point),
        ttl,
    )

    fun get(driverUid: UUID): CoordinatesDto? =
        redisTemplate.opsForValue().get(keyPrefix + driverUid.toString())?.let { objectMapper.readValue(it, CoordinatesDto::class.java) }
}

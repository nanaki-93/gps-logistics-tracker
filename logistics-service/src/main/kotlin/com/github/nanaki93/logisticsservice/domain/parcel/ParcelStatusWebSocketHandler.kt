package com.github.nanaki93.logisticsservice.domain.parcel

import com.github.nanaki93.logisticsservice.domain.util.logger
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Component
class ParcelStatusWebSocketHandler(
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    // driverUid → set of sessions watching that driver's parcels
    private val sessions = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>>()
    private val log = logger()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val driverUid =
            extractDriverUid(session) ?: run {
                session.close(CloseStatus.BAD_DATA)
                return
            }
        sessions.getOrPut(driverUid) { CopyOnWriteArraySet() }.add(session)
        log.debug("Parcel status client connected driverUid={}", driverUid)
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        val driverUid = extractDriverUid(session) ?: return
        sessions[driverUid]?.remove(session)
        // Clean up empty sets to avoid memory leak
        if (sessions[driverUid]?.isEmpty() == true) {
            sessions.remove(driverUid)
        }
        log.debug("Parcel status client disconnected driverUid={}", driverUid)
    }

    // Called by TelemetryEventService when parcel distances are evaluated
    // Only fires if someone is currently watching this driver's parcels
    fun broadcastParcelUpdate(
        driverUid: String,
        update: ParcelStatusUpdate,
    ) {
        val watching = sessions[driverUid]
        if (watching.isNullOrEmpty()) return

        val payload = objectMapper.writeValueAsString(update)
        watching.forEach { session ->
            if (session.isOpen) {
                runCatching {
                    synchronized(session) {
                        session.sendMessage(TextMessage(payload))
                    }
                }.onFailure { log.warn("Failed to send map update to session", it) }
            }
        }
    }

    fun hasActiveSubscribers(driverUid: String): Boolean = sessions[driverUid]?.isNotEmpty() == true

    private fun extractDriverUid(session: WebSocketSession): String? =
        session.uri
            ?.path
            ?.removePrefix("/ws/drivers/")
            ?.removeSuffix("/parcels")
            ?.takeIf { it.isNotBlank() }
}

data class ParcelStatusUpdate(
    val driverUid: UUID,
    val parcels: List<ParcelDistanceSummary>,
    val updatedAt: Instant = Instant.now(),
)

data class LocationUpdate(
    val driverUid: UUID,
    val lat: Double,
    val lng: Double,
    val recordedAt: Instant,
    val parcels: List<ParcelDistanceSummary>,
)

data class ParcelDistanceSummary(
    val parcelUid: UUID,
    val status: String,
    val distanceMetres: Double,
)

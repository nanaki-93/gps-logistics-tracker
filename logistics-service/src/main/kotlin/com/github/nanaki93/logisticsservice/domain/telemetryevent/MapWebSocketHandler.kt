package com.github.nanaki93.logisticsservice.domain.telemetryevent

import com.github.nanaki93.logisticsservice.domain.util.logger
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.CopyOnWriteArraySet

@Component
class MapWebSocketHandler(
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    private val log = logger()

    // driverUid → set of active WebSocket sessions
    private val sessions = CopyOnWriteArraySet<WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions.add(session)
        log.debug("Map client connected total={}", sessions.size)
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        sessions.remove(session)
        log.debug("Map client disconnected total={}", sessions.size)
    }

    // Called by TelemetryService after every GPS event
    fun broadcastMapPosition(update: TelemetryEventPlainDto) {
        if (sessions.isEmpty()) return
        val payload = objectMapper.writeValueAsString(update)
        sessions.forEach { session ->
            if (session.isOpen) {
                runCatching { session.sendMessage(TextMessage(payload)) }
                    .onFailure { log.warn("Failed to send map update to session", it) }
            }
        }
    }
}

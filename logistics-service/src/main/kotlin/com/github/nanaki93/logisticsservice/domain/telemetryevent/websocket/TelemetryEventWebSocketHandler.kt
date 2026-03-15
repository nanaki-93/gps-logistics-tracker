package com.github.nanaki93.logisticsservice.domain.telemetryevent.websocket

import com.github.nanaki93.logisticsservice.domain.telemetryevent.TelemetryEventPlainDto
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Component
class TelemetryEventWebSocketHandler(
    private val objectMapper: ObjectMapper,
) : TextWebSocketHandler() {
    // driverUid → set of active WebSocket sessions
    private val sessions = ConcurrentHashMap<String, CopyOnWriteArraySet<WebSocketSession>>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val driverUid = extractDriverUid(session)
        sessions.getOrPut(driverUid) { CopyOnWriteArraySet() }.add(session)
    }

    override fun afterConnectionClosed(
        session: WebSocketSession,
        status: CloseStatus,
    ) {
        val driverUid = extractDriverUid(session)
        sessions[driverUid]?.remove(session)
    }

    // Called by TelemetryService after every GPS event
    fun broadcast(
        driverUid: String,
        update: TelemetryEventPlainDto,
    ) {
        val payload = objectMapper.writeValueAsString(update)
        sessions[driverUid]?.forEach { session ->
            if (session.isOpen) {
                session.sendMessage(TextMessage(payload))
            }
        }
    }

    private fun extractDriverUid(session: WebSocketSession): String =
        session.uri
            ?.path
            ?.substringAfterLast("/ws/drivers/")
            ?.substringBefore("/live") ?: ""
}

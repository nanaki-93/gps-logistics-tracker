package com.github.nanaki93.logisticsservice.domain.telemetryevent.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class TelemetryEventWebSocketConfig(
    private val trackingWebSocketHandler: TelemetryEventWebSocketHandler,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(trackingWebSocketHandler, "/ws/drivers/{driverUid}/live")
            .setAllowedOrigins("*") // restrict in production
    }
}

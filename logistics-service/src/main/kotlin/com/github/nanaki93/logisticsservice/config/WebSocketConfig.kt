package com.github.nanaki93.logisticsservice.config

import com.github.nanaki93.logisticsservice.domain.parcel.ParcelStatusWebSocketHandler
import com.github.nanaki93.logisticsservice.domain.telemetryevent.MapWebSocketHandler
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val trackingWsHandler: MapWebSocketHandler,
    private val parcelStatusWsHandler: ParcelStatusWebSocketHandler,
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(trackingWsHandler, "/ws/drivers/live")
            .addHandler(parcelStatusWsHandler, "/ws/drivers/{driverUid}/parcels")
            .setAllowedOrigins("*") // restrict in production
    }
}

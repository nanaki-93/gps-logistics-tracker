package handler

import (
	"encoding/json"
	"net/http"

	"com.github.nanaki93/telemetry-service/internal/metrics"
	"com.github.nanaki93/telemetry-service/internal/model"
	"com.github.nanaki93/telemetry-service/internal/worker"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/codes"
	"go.opentelemetry.io/otel/trace"
)

type TelemetryHandler struct {
	pool *worker.Pool
}

func NewTelemetryHandler(pool *worker.Pool) *TelemetryHandler {
	return &TelemetryHandler{
		pool: pool,
	}
}

func (h *TelemetryHandler) HandleTelemetry(w http.ResponseWriter, r *http.Request) {
	span := trace.SpanFromContext(r.Context())

	var event model.GpsEvent
	if err := json.NewDecoder(r.Body).Decode(&event); err != nil {
		span.SetStatus(codes.Error, "invalid request body: "+err.Error()+"")
		http.Error(w, "invalid request body", http.StatusBadRequest)
		return
	}

	if err := event.Validate(); err != nil {
		span.SetStatus(codes.Error, "invalid telemetry data: "+err.Error()+"")
		http.Error(w, err.Error(), http.StatusUnprocessableEntity)
		return
	}

	span.SetAttributes(
		attribute.String("driverUid", event.DriverUid),
		attribute.Float64("gps.lat", event.Lat),
		attribute.Float64("gps.lng", event.Lng),
	)
	if err := h.pool.Submit(r.Context(), event); err != nil {
		span.SetStatus(codes.Error, "failed to submit telemetry data: "+err.Error()+"")
		http.Error(w, err.Error(), http.StatusServiceUnavailable)
		return
	}

	metrics.SetQueueDepth(h.pool.QueueDepth())
	span.SetStatus(codes.Ok, "telemetry data processed successfully")
	w.WriteHeader(http.StatusAccepted)
}

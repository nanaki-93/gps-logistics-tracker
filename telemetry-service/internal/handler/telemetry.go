package handler

import (
	"encoding/json"
	"fmt"
	"net/http"

	"com.github.nanaki93/telemetry-service/internal/model"
	"com.github.nanaki93/telemetry-service/internal/worker"
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

	var event model.GpsEvent
	if err := json.NewDecoder(r.Body).Decode(&event); err != nil {
		http.Error(w, "invalid request body", http.StatusBadRequest)
		return
	}

	if err := event.Validate(); err != nil {
		http.Error(w, err.Error(), http.StatusUnprocessableEntity)
		return
	}

	if err := h.pool.Submit(event); err != nil {
		http.Error(w, err.Error(), http.StatusServiceUnavailable)
		return
	}

	fmt.Printf("Received telemetry data: %v\n", event)
	w.WriteHeader(http.StatusAccepted)
}

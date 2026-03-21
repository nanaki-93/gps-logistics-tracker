package middleware

import (
	"net/http"
	"strconv"
	"time"

	"com.github.nanaki93/telemetry-service/internal/metrics"
	"github.com/go-chi/chi/v5"
)

type statusCapturingResponseWriter struct {
	http.ResponseWriter
	status int
}

func (w *statusCapturingResponseWriter) WriteHeader(status int) {
	w.status = status
	w.ResponseWriter.WriteHeader(status)
}

func RequestMetrics(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		sw := &statusCapturingResponseWriter{
			ResponseWriter: w,
			status:         http.StatusOK,
		}

		start := time.Now()
		next.ServeHTTP(sw, r)

		route := routePattern(r)
		metrics.HTTPRequestsTotal.WithLabelValues(
			r.Method,
			route,
			strconv.Itoa(sw.status),
		).Inc()

		_ = start
	})
}

func routePattern(r *http.Request) string {
	if rc := chi.RouteContext(r.Context()); rc != nil {
		if pattern := rc.RoutePattern(); pattern != "" {
			return pattern
		}
	}
	if path := r.URL.Path; path != "" {
		return path
	}
	return "unknown"
}

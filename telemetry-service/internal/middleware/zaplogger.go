package middleware

import (
	"net/http"
	"time"

	"com.github.nanaki93/telemetry-service/internal/logger"
	"go.uber.org/zap"
)

func ZapLogger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		// Wrap response writer to capture status code
		wrapped := &responseWriter{ResponseWriter: w, status: http.StatusOK}
		next.ServeHTTP(wrapped, r)

		// Log after handler completes — includes trace ID from context
		log := logger.FromContext(r.Context())
		log.Info("request",
			zap.String("method", r.Method),
			zap.String("path", r.URL.Path),
			zap.Int("status", wrapped.status),
			zap.Duration("duration", time.Since(start)),
			zap.String("ip", r.RemoteAddr),
			zap.String("request_id", r.Header.Get("X-Request-Id")),
		)
	})
}

type responseWriter struct {
	http.ResponseWriter
	status int
}

func (rw *responseWriter) WriteHeader(code int) {
	rw.status = code
	rw.ResponseWriter.WriteHeader(code)
}

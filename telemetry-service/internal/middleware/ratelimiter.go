package middleware

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"sync"
	"time"
)

type tokenBucket struct {
	mu         sync.Mutex
	tokens     float64
	maxTokens  float64
	refillRate float64 // tokens per second
	lastRefill time.Time
}

func newTokenBucket(rps int) *tokenBucket {
	return &tokenBucket{
		tokens:     float64(rps),
		maxTokens:  float64(rps),
		refillRate: float64(rps),
		lastRefill: time.Now(),
	}
}

func (tb *tokenBucket) Allow() bool {
	tb.mu.Lock()
	defer tb.mu.Unlock()

	now := time.Now()
	elapsed := now.Sub(tb.lastRefill).Seconds()
	tb.tokens = min(tb.maxTokens, tb.tokens+elapsed*tb.refillRate)
	tb.lastRefill = now

	if tb.tokens < 1 {
		return false
	}
	tb.tokens--
	return true
}

func RateLimiter(rps int) func(http.Handler) http.Handler {
	var (
		mu       sync.Mutex
		limiters = make(map[string]*tokenBucket)
	)

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Read body to extract driver_uid
			body, err := io.ReadAll(r.Body)
			if err != nil {
				http.Error(w, "bad request", http.StatusBadRequest)
				return
			}
			// Restore body so the handler can read it again
			r.Body = io.NopCloser(bytes.NewBuffer(body))

			// Extract driver_uid as rate limit key
			var payload struct {
				DriverUID string `json:"driver_uid"`
			}
			rateLimitKey := r.RemoteAddr // fallback to IP
			if err := json.Unmarshal(body, &payload); err == nil && payload.DriverUID != "" {
				rateLimitKey = payload.DriverUID
			}

			mu.Lock()
			lb, ok := limiters[rateLimitKey]
			if !ok {
				lb = newTokenBucket(rps)
				limiters[rateLimitKey] = lb
			}
			mu.Unlock()

			if !lb.Allow() {
				http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

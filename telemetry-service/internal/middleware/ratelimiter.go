package middleware

import (
	"bytes"
	"encoding/json"
	"io"
	"net"
	"net/http"
	"sync"
	"time"
)

const (
	defaultBucketTTL       = 1 * time.Second
	defaultCleanupInterval = 100 * time.Millisecond
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

func (tb *tokenBucket) isIdle(now time.Time, ttl time.Duration) bool {
	tb.mu.Lock()
	defer tb.mu.Unlock()

	return now.Sub(tb.lastRefill) > ttl
}

type rateLimiter struct {
	mu       sync.Mutex
	limiters map[string]*tokenBucket
	rps      int
	ttl      time.Duration
}

func newRateLimiter(rps int, ttl, cleanupInterval time.Duration) *rateLimiter {
	rl := &rateLimiter{
		limiters: make(map[string]*tokenBucket),
		rps:      rps,
		ttl:      ttl,
	}
	if cleanupInterval > 0 {
		go rl.cleanup(cleanupInterval)
	}
	return rl
}

func (rl *rateLimiter) cleanup(interval time.Duration) {
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for now := range ticker.C {
		rl.evict(now)
	}
}

func (rl *rateLimiter) evict(now time.Time) {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	for key, b := range rl.limiters {
		if b.isIdle(now, rl.ttl) {
			delete(rl.limiters, key)
		}
	}
	if len(rl.limiters) == 0 {
		rl.limiters = make(map[string]*tokenBucket)
	}
}

func (rl *rateLimiter) middleware() func(http.Handler) http.Handler {
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

			key := rateLimitKey(r, body)

			rl.mu.Lock()
			lb, ok := rl.limiters[key]
			if !ok {
				lb = newTokenBucket(rl.rps)
				rl.limiters[key] = lb
			}
			allowed := lb.Allow()
			rl.mu.Unlock()

			if !allowed {
				http.Error(w, "rate limit exceeded", http.StatusTooManyRequests)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

func rateLimitKey(r *http.Request, body []byte) string {
	var payload struct {
		DriverUID string `json:"driver_uid"`
	}
	if err := json.Unmarshal(body, &payload); err == nil && payload.DriverUID != "" {
		return payload.DriverUID
	}
	if host, _, err := net.SplitHostPort(r.RemoteAddr); err == nil && host != "" {
		return host
	}
	return r.RemoteAddr
}

func RateLimiter(rps int) func(http.Handler) http.Handler {
	return newRateLimiter(rps, defaultBucketTTL, defaultCleanupInterval).middleware()
}

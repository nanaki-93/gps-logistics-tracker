package middleware

import (
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"runtime"
	"strings"
	"testing"
	"time"
)

func okHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = io.ReadAll(r.Body)
		w.WriteHeader(http.StatusOK)
	})
}

func TestRateLimiter_RemoteAddrStripsEphemeralPort(t *testing.T) {
	handler := RateLimiter(1)(okHandler())

	for i, addr := range []string{"192.0.2.10:50001", "192.0.2.10:50002", "192.0.2.10:50003"} {
		req := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{}`))
		req.RemoteAddr = addr
		rec := httptest.NewRecorder()
		handler.ServeHTTP(rec, req)

		wantStatus := http.StatusTooManyRequests
		if i == 0 {
			wantStatus = http.StatusOK
		}
		if rec.Code != wantStatus {
			t.Fatalf("request %d (%s): got status %d, want %d", i, addr, rec.Code, wantStatus)
		}
	}
}

func TestRateLimiter_RemoteAddrIPv6StripsEphemeralPort(t *testing.T) {
	handler := RateLimiter(1)(okHandler())

	req1 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{}`))
	req1.RemoteAddr = "[2001:db8::1]:41000"
	rec1 := httptest.NewRecorder()
	handler.ServeHTTP(rec1, req1)
	if rec1.Code != http.StatusOK {
		t.Fatalf("first request: got status %d, want %d", rec1.Code, http.StatusOK)
	}

	req2 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{}`))
	req2.RemoteAddr = "[2001:db8::1]:41001"
	rec2 := httptest.NewRecorder()
	handler.ServeHTTP(rec2, req2)
	if rec2.Code != http.StatusTooManyRequests {
		t.Fatalf("second request from same IPv6 host with new port: got status %d, want %d", rec2.Code, http.StatusTooManyRequests)
	}
}

func TestRateLimiter_EvictsIdleBuckets(t *testing.T) {
	handler := RateLimiter(1)(okHandler())

	runtime.GC()
	runtime.GC()
	var baseline runtime.MemStats
	runtime.ReadMemStats(&baseline)

	padding := strings.Repeat("x", 1024)
	const numDrivers = 10000
	for i := range numDrivers {
		body := fmt.Sprintf(`{"driver_uid":"driver-%d-%s"}`, i, padding)
		req := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(body))
		req.RemoteAddr = fmt.Sprintf("10.0.0.1:%d", 10000+(i%50000))
		rec := httptest.NewRecorder()
		handler.ServeHTTP(rec, req)
		if rec.Code != http.StatusOK {
			t.Fatalf("driver-%d initial request: got status %d, want %d", i, rec.Code, http.StatusOK)
		}
	}

	runtime.GC()
	runtime.GC()
	var peak runtime.MemStats
	runtime.ReadMemStats(&peak)

	time.Sleep(1300 * time.Millisecond)

	runtime.GC()
	runtime.GC()
	var afterEvict runtime.MemStats
	runtime.ReadMemStats(&afterEvict)

	if afterEvict.HeapAlloc >= peak.HeapAlloc/2 {
		t.Fatalf("expected idle buckets to be evicted and freed: baseline=%d peak=%d afterEvict=%d",
			baseline.HeapAlloc, peak.HeapAlloc, afterEvict.HeapAlloc)
	}
	runtime.KeepAlive(handler)
}

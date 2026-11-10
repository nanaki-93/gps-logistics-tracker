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

type countingReader struct {
	remaining int64
	bytesRead int64
}

func (r *countingReader) Read(p []byte) (int, error) {
	if r.remaining <= 0 {
		return 0, io.EOF
	}
	n := int64(len(p))
	if n > r.remaining {
		n = r.remaining
	}
	for i := range int(n) {
		p[i] = 'a'
	}
	r.remaining -= n
	r.bytesRead += n
	return int(n), nil
}

func TestRateLimiter_RejectsOversizedBody(t *testing.T) {
	nextCalled := false
	next := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		nextCalled = true
		w.WriteHeader(http.StatusOK)
	})
	handler := RateLimiter(10)(next)

	const totalBytes = 10 * 1024 * 1024
	const maxAllowedRead = 1024 * 1024
	body := &countingReader{remaining: totalBytes}
	req := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", body)
	req.RemoteAddr = "192.0.2.50:12345"
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("got status %d, want %d", rec.Code, http.StatusRequestEntityTooLarge)
	}
	if nextCalled {
		t.Fatal("expected next handler not to be called for oversized body")
	}
	if body.bytesRead > maxAllowedRead {
		t.Fatalf("read %d bytes from oversized body, want at most %d", body.bytesRead, maxAllowedRead)
	}
}

func TestRateLimiter_LimitsBeforeReadingBody(t *testing.T) {
	handler := RateLimiter(1)(okHandler())

	req1 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{}`))
	req1.Header.Set("X-Driver-UID", "driver-limit-before-read")
	req1.RemoteAddr = "192.0.2.60:10001"
	rec1 := httptest.NewRecorder()
	handler.ServeHTTP(rec1, req1)
	if rec1.Code != http.StatusOK {
		t.Fatalf("first request: got status %d, want %d", rec1.Code, http.StatusOK)
	}

	body := &countingReader{remaining: 10 * 1024 * 1024}
	req2 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", body)
	req2.Header.Set("X-Driver-UID", "driver-limit-before-read")
	req2.RemoteAddr = "192.0.2.60:10002"
	rec2 := httptest.NewRecorder()
	handler.ServeHTTP(rec2, req2)

	if rec2.Code != http.StatusTooManyRequests {
		t.Fatalf("second request: got status %d, want %d", rec2.Code, http.StatusTooManyRequests)
	}
	if body.bytesRead != 0 {
		t.Fatalf("expected 0 bytes read when rate limited before body read, got %d", body.bytesRead)
	}
}

func TestRateLimiter_BodyDriverUIDNotShadowedByHostBucket(t *testing.T) {
	handler := RateLimiter(1)(okHandler())

	req1 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{}`))
	req1.RemoteAddr = "192.0.2.70:10001"
	rec1 := httptest.NewRecorder()
	handler.ServeHTTP(rec1, req1)
	if rec1.Code != http.StatusOK {
		t.Fatalf("keyless request: got status %d, want %d", rec1.Code, http.StatusOK)
	}

	req2 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{"driver_uid":"d1"}`))
	req2.RemoteAddr = "192.0.2.70:10002"
	rec2 := httptest.NewRecorder()
	handler.ServeHTTP(rec2, req2)
	if rec2.Code != http.StatusOK {
		t.Fatalf("body driver_uid request after host bucket exists: got status %d, want %d", rec2.Code, http.StatusOK)
	}

	req3 := httptest.NewRequest(http.MethodPost, "/api/v1/telemetry", strings.NewReader(`{"driver_uid":"d1"}`))
	req3.RemoteAddr = "192.0.2.70:10003"
	rec3 := httptest.NewRecorder()
	handler.ServeHTTP(rec3, req3)
	if rec3.Code != http.StatusTooManyRequests {
		t.Fatalf("second request for d1: got status %d, want %d", rec3.Code, http.StatusTooManyRequests)
	}
}

package metrics

import (
	"strings"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	HTTPRequestsTotal = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Name: "telemetry_service_http_requests_total",
			Help: "Total number of HTTP requests handled by the service.",
		},
		[]string{"method", "route", "status"},
	)

	QueueDepth = promauto.NewGauge(
		prometheus.GaugeOpts{
			Name: "telemetry_service_queue_depth",
			Help: "Current number of jobs waiting in the worker queue.",
		},
	)

	WorkerProcessingDuration = promauto.NewHistogram(
		prometheus.HistogramOpts{
			Name:    "telemetry_service_worker_processing_duration_seconds",
			Help:    "Time spent processing telemetry jobs in workers.",
			Buckets: prometheus.DefBuckets,
		},
	)

	FailedPublishesTotal = promauto.NewCounter(
		prometheus.CounterOpts{
			Name: "telemetry_service_failed_publishes_total",
			Help: "Total number of failed RabbitMQ publish attempts.",
		},
	)
)

func SetQueueDepth(n int) {
	QueueDepth.Set(float64(n))
}

func ObserveWorkerDuration(duration time.Duration) {
	WorkerProcessingDuration.Observe(duration.Seconds())
}

func IncFailedPublishes() {
	FailedPublishesTotal.Inc()
}

func NormalizeRoute(route string) string {
	if route == "" {
		return "unknown"
	}
	return strings.TrimSpace(route)
}

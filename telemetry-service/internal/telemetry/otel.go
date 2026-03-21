package telemetry

import (
	"context"
	"fmt"
	"time"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.21.0"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// Init sets up the OpenTelemetry tracer provider and returns a shutdown function.
// Call shutdown() on SIGTERM to flush remaining spans before exit.
func Init(ctx context.Context, serviceName, collectorURL string) (shutdown func(), err error) {
	// Connect to the OTLP collector (Jaeger in your docker-compose)
	conn, err := grpc.NewClient(
		collectorURL,
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to connect to collector: %w", err)
	}

	exporter, err := otlptracegrpc.New(ctx, otlptracegrpc.WithGRPCConn(conn))
	if err != nil {
		return nil, fmt.Errorf("failed to create exporter: %w", err)
	}

	// Resource describes this service to the backend
	res := resource.NewWithAttributes(
		semconv.SchemaURL,
		semconv.ServiceName(serviceName),
		semconv.ServiceVersion("1.0.0"),
	)

	provider := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(exporter),
		sdktrace.WithResource(res),
		// Sample 100% of traces in dev — reduce to 0.1 in production
		sdktrace.WithSampler(sdktrace.AlwaysSample()),
	)

	// Register as the global tracer provider
	otel.SetTracerProvider(provider)

	// W3C TraceContext + Baggage — standard propagation format
	// This is what gets injected into AMQP headers
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{},
		propagation.Baggage{},
	))

	shutdown = func() {
		ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
		defer cancel()
		err := provider.Shutdown(ctx)
		if err != nil {
			return
		}
	}

	return shutdown, nil
}

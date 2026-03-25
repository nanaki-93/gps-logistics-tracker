package logger

import (
	"context"

	"go.opentelemetry.io/otel/trace"
	"go.uber.org/zap"
)

func FromContext(ctx context.Context) *zap.Logger {
	span := trace.SpanFromContext(ctx)
	if !span.SpanContext().IsValid() {
		return Log
	}
	return Log.With(
		zap.String("traceId", span.SpanContext().TraceID().String()),
		zap.String("spanId", span.SpanContext().SpanID().String()),
	)
}

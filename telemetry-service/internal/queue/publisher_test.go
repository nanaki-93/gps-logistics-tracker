package queue

import (
	"context"
	"encoding/json"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"com.github.nanaki93/telemetry-service/internal/model"
	amqp "github.com/rabbitmq/amqp091-go"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/trace"
)

type mockChannel struct {
	published atomic.Int64
	args      atomic.Value
	lastMsg   atomic.Pointer[amqp.Publishing]
}

func (c *mockChannel) PublishWithContext(_ context.Context, ex, key string, mand, imm bool, msg amqp.Publishing) error {
	c.published.Add(1)
	c.args.Store([4]any{ex, key, mand, imm})
	c.lastMsg.Store(&msg)
	return nil
}

func (c *mockChannel) Close() error { return nil }

func TestPublisherThroughputAndHeaders(t *testing.T) {
	otel.SetTextMapPropagator(propagation.TraceContext{})
	ctx := trace.ContextWithSpanContext(context.Background(), trace.NewSpanContext(trace.SpanContextConfig{TraceID: trace.TraceID{1}, SpanID: trace.SpanID{1}, TraceFlags: trace.FlagsSampled}))
	ch, ev, wg := &mockChannel{}, model.GpsEvent{DriverUid: "d1", Lat: 35.6, Lng: 139.6, RecordedAt: time.Unix(9, 0).UTC()}, sync.WaitGroup{}
	pub := &Publisher{channel: ch, exchangeName: "telemetry", routingKey: "gps"}
	for range 8 {
		wg.Go(func() {
			for range 250 {
				_ = pub.Publish(ctx, ev)
			}
		})
	}
	wg.Wait()
	msg, got := ch.lastMsg.Load(), model.GpsEvent{}
	if ch.published.Load() != 2000 || msg == nil || json.Unmarshal(msg.Body, &got) != nil || got != ev || ch.args.Load() != [4]any{"telemetry", "gps", false, false} ||
		msg.ContentType != "application/json" || msg.DeliveryMode != amqp.Persistent || pub.Close() != nil ||
		amqpHeaderCarrier(msg.Headers).Get("traceparent") != "00-01000000000000000000000000000000-0100000000000000-01" {
		t.Fatalf("published=%d msg=%+v args=%v", ch.published.Load(), msg, ch.args.Load())
	}
}

func BenchmarkPublisher(b *testing.B) {
	otel.SetTextMapPropagator(propagation.TraceContext{})
	ctx := trace.ContextWithSpanContext(context.Background(), trace.NewSpanContext(trace.SpanContextConfig{TraceID: trace.TraceID{1}, SpanID: trace.SpanID{1}, TraceFlags: trace.FlagsSampled}))
	ev, pub := model.GpsEvent{DriverUid: "d1", Lat: 35.6, Lng: 139.6, RecordedAt: time.Now().UTC()}, &Publisher{channel: &mockChannel{}, exchangeName: "telemetry", routingKey: "gps"}
	b.Run("Marshal", func(b *testing.B) {
		for range b.N {
			_, _ = json.Marshal(ev)
		}
	})
	b.Run("Inject", func(b *testing.B) {
		prop, hdrs := otel.GetTextMapPropagator(), amqpHeaderCarrier(amqp.Table{})
		for range b.N {
			prop.Inject(ctx, hdrs)
		}
	})
	b.Run("PublishParallel", func(b *testing.B) {
		b.RunParallel(func(pb *testing.PB) {
			for pb.Next() {
				_ = pub.Publish(ctx, ev)
			}
		})
	})
}

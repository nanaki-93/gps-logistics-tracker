package worker

import (
	"context"
	"runtime"
	"sync"
	"sync/atomic"
	"testing"

	"com.github.nanaki93/telemetry-service/internal/model"
)

type nopPublisher struct{ published atomic.Int64 }

func (p *nopPublisher) Publish(_ context.Context, _ model.GpsEvent) error {
	p.published.Add(1)
	return nil
}

func TestPoolThroughput(t *testing.T) {
	pub, ev, wg := &nopPublisher{}, model.GpsEvent{DriverUid: "d1", Lat: 35.6, Lng: 139.6}, sync.WaitGroup{}
	pool := NewPool(8, 2000, pub)
	pool.Start()
	for range 8 {
		wg.Go(func() {
			for range 250 {
				_ = pool.Submit(context.Background(), ev)
			}
		})
	}
	wg.Wait()
	pool.Stop()
	if pub.published.Load() != 2000 || pool.QueueDepth() != 0 || NewPool(1, 0, pub).Submit(context.Background(), ev) != ErrChannelFull {
		t.Fatalf("published=%d depth=%d", pub.published.Load(), pool.QueueDepth())
	}
}

func BenchmarkPool(b *testing.B) {
	ev, pool := model.GpsEvent{DriverUid: "d1"}, NewPool(8, 1024, &nopPublisher{})
	pool.Start()
	defer pool.Stop()
	b.Run("Sequential", func(b *testing.B) {
		for i := 0; i < b.N; i++ {
			for pool.Submit(context.Background(), ev) != nil {
				runtime.Gosched()
			}
		}
	})
	b.Run("Concurrent", func(b *testing.B) {
		b.RunParallel(func(pb *testing.PB) {
			for pb.Next() {
				for pool.Submit(context.Background(), ev) != nil {
					runtime.Gosched()
				}
			}
		})
	})
}

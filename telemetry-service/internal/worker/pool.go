package worker

import (
	"errors"
	"log"
	"sync"

	"com.github.nanaki93/telemetry-service/internal/model"
	"com.github.nanaki93/telemetry-service/internal/queue"
)

var ErrChannelFull = errors.New("worker channel is full")

type Pool struct {
	jobs        chan model.GpsEvent
	workerCount int
	publisher   *queue.Publisher
	wg          sync.WaitGroup
}

func NewPool(workerCount, bufferSize int, publisher *queue.Publisher) *Pool {
	return &Pool{
		jobs:        make(chan model.GpsEvent, bufferSize),
		workerCount: workerCount,
		publisher:   publisher,
	}
}

func (p *Pool) Start() {
	for i := 0; i < p.workerCount; i++ {
		p.wg.Add(1)
		go p.work()
	}
	log.Printf("worker pool started with %d workers", p.workerCount)
}

func (p *Pool) Submit(event model.GpsEvent) error {
	select {
	case p.jobs <- event:
		return nil
	default:
		return ErrChannelFull
	}
}

func (p *Pool) Stop() {
	log.Println("stopping worker pool")
	close(p.jobs)
	p.wg.Wait()
	log.Println("worker pool stopped")
}

func (p *Pool) work() {
	defer p.wg.Done()
	for event := range p.jobs {
		if err := p.publisher.Publish(event); err != nil {
			log.Printf("failed to publish event: %v", err)
		}
	}
}

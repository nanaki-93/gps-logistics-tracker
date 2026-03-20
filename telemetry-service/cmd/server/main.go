package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"com.github.nanaki93/telemetry-service/config"
	"com.github.nanaki93/telemetry-service/internal/handler"
	"com.github.nanaki93/telemetry-service/internal/middleware"
	"com.github.nanaki93/telemetry-service/internal/queue"
	"com.github.nanaki93/telemetry-service/internal/worker"
	"github.com/go-chi/chi/v5"
	chimdw "github.com/go-chi/chi/v5/middleware"
	"github.com/joho/godotenv"
)

func main() {

	if err := godotenv.Load(); err != nil {
		log.Printf("dotenv load skipped/failed: %v", err)
	}
	cfg, err := config.Load()
	if err != nil {
		log.Fatal(">" + fmt.Sprintf("failed to load config: %v", err))
	}

	publisher, err := queue.NewPublisher(cfg.RabbitMQUrl, cfg.ExchangeName, cfg.RoutingKey)
	if err != nil {
		log.Fatal(">" + fmt.Sprintf("failed to create publisher: %v", err))
	}
	defer func(publisher *queue.Publisher) {
		fmt.Println("closing publisher")
		err := publisher.Close()
		if err != nil {
			fmt.Printf("failed to close publisher: %v", err)
		}
	}(publisher)

	pool := worker.NewPool(cfg.WorkerCount, cfg.ChannelBuffer, publisher)
	pool.Start()

	h := handler.NewTelemetryHandler(pool)
	r := chi.NewRouter()

	r.Use(chimdw.RequestID)
	r.Use(chimdw.RealIP)
	r.Use(chimdw.Logger)
	r.Use(chimdw.Recoverer)
	r.Use(chimdw.Timeout(30 * time.Second))

	//r.Use(middleware.APIKeyAuth(cfg.ApiKeys))
	r.Use(middleware.RateLimiter(cfg.RateLimitRPS))
	r.Post("/api/v1/telemetry", h.HandleTelemetry)

	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	server := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      r,
		ReadTimeout:  cfg.ReadTimeout,
		WriteTimeout: cfg.WriteTimeout,
	}

	go func() {
		log.Printf("telemetry service listening on :%s", cfg.Port)
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			log.Printf("http server failed to start: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("shutdown signal received")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := server.Shutdown(ctx); err != nil {
		log.Printf("http server shutdown failed: %v", err)
	}

	pool.Stop()

	log.Println("telemetry service stopped")
}

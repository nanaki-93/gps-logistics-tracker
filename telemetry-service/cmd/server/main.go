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
	"com.github.nanaki93/telemetry-service/internal/logger"
	"com.github.nanaki93/telemetry-service/internal/middleware"
	"com.github.nanaki93/telemetry-service/internal/queue"
	"com.github.nanaki93/telemetry-service/internal/telemetry"
	"com.github.nanaki93/telemetry-service/internal/worker"
	"github.com/go-chi/chi/v5"
	chimdw "github.com/go-chi/chi/v5/middleware"
	"github.com/joho/godotenv"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
	"go.uber.org/zap"
)

func main() {

	if err := godotenv.Load(); err != nil {
		log.Printf("dotenv load skipped/failed: %v", err)
	}
	cfg, err := config.Load()
	if err != nil {
		log.Fatal(">" + fmt.Sprintf("failed to load config: %v", err))
	}

	logger.Init(cfg.Env)
	defer logger.Sync()

	logger.Log.Info("starting telemetry service",
		zap.String("env", cfg.Env),
		zap.String("port", cfg.Port),
		zap.Int("workerCount", cfg.WorkerCount),
		zap.Int("channelBuffer", cfg.ChannelBuffer),
		zap.Int("rateLimitRPS", cfg.RateLimitRPS))

	shutdown, err := telemetry.Init(
		context.Background(),
		"telemetry-service",
		cfg.OTelCollectorURL,
	)
	if err != nil {
		logger.Log.Fatal("otel init failed: %v", zap.Error(err))
	}
	defer shutdown()

	publisher, err := queue.NewPublisher(cfg.RabbitMQUrl, cfg.ExchangeName, cfg.RoutingKey)
	if err != nil {
		logger.Log.Fatal("> failed to create publisher: %v", zap.Error(err))
	}
	defer func(publisher *queue.Publisher) {
		logger.Log.Info("closing publisher")
		err := publisher.Close()
		if err != nil {
			logger.Log.Fatal("failed to close publisher: %v", zap.Error(err))
		}
	}(publisher)

	pool := worker.NewPool(cfg.WorkerCount, cfg.ChannelBuffer, publisher)
	pool.Start()

	h := handler.NewTelemetryHandler(pool)

	r := chi.NewRouter()

	r.Use(chimdw.RequestID)
	r.Use(chimdw.RealIP)
	r.Use(otelhttp.NewMiddleware("telemetry-service"))
	r.Use(middleware.ZapLogger)
	r.Use(chimdw.Recoverer)
	r.Use(chimdw.Timeout(30 * time.Second))
	r.Use(middleware.RequestMetrics)
	//r.Use(middleware.APIKeyAuth(cfg.ApiKeys))
	r.Use(middleware.RateLimiter(cfg.RateLimitRPS))
	r.Post("/api/v1/telemetry", h.HandleTelemetry)

	r.Get("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	r.Get("/metrics", promhttp.Handler().ServeHTTP)

	server := &http.Server{
		Addr:         ":" + cfg.Port,
		Handler:      r,
		ReadTimeout:  cfg.ReadTimeout,
		WriteTimeout: cfg.WriteTimeout,
	}

	go func() {
		logger.Log.Info("telemetry service listening on ", zap.String("port", cfg.Port))
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			logger.Log.Fatal("http server failed to start: %v", zap.Error(err))
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	logger.Log.Info("shutdown signal received")

	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	if err := server.Shutdown(ctx); err != nil {
		logger.Log.Fatal("http server shutdown failed: %v", zap.Error(err))
	}

	pool.Stop()

	logger.Log.Info("telemetry service stopped")
}

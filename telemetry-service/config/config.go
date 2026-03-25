package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Port         string
	ReadTimeout  time.Duration
	WriteTimeout time.Duration

	RabbitMQUrl  string
	ExchangeName string
	RoutingKey   string

	WorkerCount   int
	ChannelBuffer int

	ApiKeys map[string]struct{}

	RateLimitRPS     int
	OTelCollectorURL string
	Env              string
}

func Load() (*Config, error) {
	cfg := &Config{
		Port:             getEnv("PORT", "8090"),
		ReadTimeout:      getDuration("READ_TIMEOUT", 5*time.Second),
		WriteTimeout:     getDuration("WRITE_TIMEOUT", 10*time.Second),
		RabbitMQUrl:      requiredEnv("RABBITMQ_URL"),
		ExchangeName:     getEnv("RABBITMQ_EXCHANGE", "telemetry.exchange"),
		RoutingKey:       getEnv("RABBITMQ_ROUTING_KEY", "telemetry.#"),
		WorkerCount:      getInt("WORKER_COUNT", 50),
		ChannelBuffer:    getInt("CHANNEL_BUFFER", 10_000),
		RateLimitRPS:     getInt("RATE_LIMIT_RPS", 50),
		OTelCollectorURL: getEnv("OTEL_COLLECTOR_URL", "http://localhost:4318"),
		Env:              getEnv("ENV", "local"),
	}
	rawKeys := requiredEnv("API_KEYS")
	cfg.ApiKeys = parseAPIKeys(rawKeys)
	return cfg, nil
}

func requiredEnv(key string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	panic(fmt.Sprintf("required env %q not set", key))
}

func getEnv(key string, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getInt(key string, defaultValue int) int {
	val := os.Getenv(key)
	if val == "" {
		return defaultValue
	}
	n, err := strconv.Atoi(val)
	if err != nil {
		panic(fmt.Sprintf("invalid int %q: %v", val, err))
	}
	return n
}

func getDuration(key string, defaultVal time.Duration) time.Duration {
	val := os.Getenv(key)
	if val == "" {
		return defaultVal
	}
	duration, err := time.ParseDuration(val)
	if err != nil {
		panic(fmt.Sprintf("invalid duration %q: %v", val, err))
	}
	return duration
}

func parseAPIKeys(raw string) map[string]struct{} {
	if raw == "" {
		return nil
	}
	keys := make(map[string]struct{})
	for _, k := range strings.Split(raw, ",") {
		k = strings.TrimSpace(k)
		if k != "" {
			keys[k] = struct{}{}
		}
	}
	return keys
}

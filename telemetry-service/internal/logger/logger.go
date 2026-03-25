package logger

import (
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

var Log *zap.Logger

// Init sets up the global zap logger.
// Call once at startup in main.go.
func Init(env string) {
	var cfg zap.Config

	if env == "local" {
		// Human-readable for local development
		cfg = zap.NewDevelopmentConfig()
		cfg.EncoderConfig.EncodeLevel = zapcore.CapitalColorLevelEncoder
	} else {
		// JSON for production — parseable by Datadog, Loki, CloudWatch
		cfg = zap.NewProductionConfig()
		cfg.EncoderConfig.TimeKey = "timestamp"
		cfg.EncoderConfig.EncodeTime = zapcore.ISO8601TimeEncoder
	}

	var err error
	Log, err = cfg.Build(
		zap.AddCaller(), // adds file and line number
		zap.AddCallerSkip(0),
		// Always include service name in every log line
		zap.Fields(
			zap.String("service", "telemetry-service"),
		),
	)
	if err != nil {
		panic("failed to init logger: " + err.Error())
	}
}

// Sync flushes any buffered log entries.
// Call defer logger.Sync() in main.go.
func Sync() {
	_ = Log.Sync()
}

# Build stage
FROM golang:1.26.1-alpine AS builder

WORKDIR /app

RUN apk add --no-cache ca-certificates

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o /app/telemetry-service ./cmd/server

# Runtime stage
FROM alpine:3.22

WORKDIR /app

RUN apk add --no-cache ca-certificates tzdata

COPY --from=builder /app/telemetry-service /app/telemetry-service

EXPOSE 8090

ENTRYPOINT ["/app/telemetry-service"]
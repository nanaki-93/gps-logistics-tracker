# GPS Logistics Tracker

**Real-Time Polyglot Fleet Telemetry & Parcel Tracking Backend (`Go` + `Kotlin` + `RabbitMQ` + `PostGIS` + `Redis`)**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Go](https://img.shields.io/badge/Go-1.24-00ADD8?style=flat-square&logo=go&logoColor=white)](https://go.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-AMQP-FF6600?style=flat-square&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com/)
[![PostGIS](https://img.shields.io/badge/PostgreSQL_15-PostGIS-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://postgis.net/)
[![Redis](https://img.shields.io/badge/Redis-7.2-DC382D?style=flat-square&logo=redis&logoColor=white)](https://redis.io/)
[![OpenTelemetry](https://img.shields.io/badge/Observability-OpenTelemetry_%7C_Jaeger_%7C_Prometheus-F46800?style=flat-square&logo=prometheus&logoColor=white)](https://opentelemetry.io/)

---

## Overview

`gps-logistics-tracker` is a distributed, event-driven backend system designed to ingest high-frequency GPS telemetry from delivery fleets and power real-time parcel tracking, geofenced delivery status transitions, and live map streaming.

The system splits responsibilities across two purpose-built microservices:
1. **`telemetry-service` (Go):** A lightweight, low-latency ingestion edge service using a bounded buffered channel and worker pool pattern to absorb bursty GPS pings from thousands of concurrent vehicles and publish them asynchronously to **RabbitMQ**.
2. **`logistics-service` (Kotlin / Spring Boot):** A rich domain service managing drivers, routes, parcels, and spatial queries. It consumes telemetry events from RabbitMQ, caches live vehicle coordinates in **Redis**, persists spatial history in **PostgreSQL/PostGIS**, automatically transitions parcel states (`CREATED` → `PICKED_UP` → `IN_TRANSIT` → `DELIVERED`) via geofence proximity checks, and pushes real-time updates to clients via **WebSockets**.

---

## System Architecture

```mermaid
flowchart TD
    Sim["k6 Fleet Simulator<br/>(Up to 2,000 Concurrent Trucks)"] -->|POST /api/v1/telemetry<br/>HTTP/JSON| Go["telemetry-service (Go)<br/>Chi Router + Rate Limiter + Worker Pool"]
    Go -->|AMQP Publish<br/>telemetry.exchange| MQ[("RabbitMQ<br/>Topic Exchange")]
    MQ -->|AMQP Consumer| KT["logistics-service (Kotlin / Spring Boot)<br/>Domain Engine + Geofence Evaluator"]
    KT -->|Write-Through Latest Position<br/>Sub-ms Reads| Redis[("Redis 7<br/>Location Cache")]
    KT -->|Spatial Persistence & Queries<br/>Hibernate Spatial + Flyway| PG[("PostgreSQL 15 + PostGIS<br/>Geospatial DB")]
    KT -->|Live Position & Parcel Status Push<br/>WebSockets| UI["Live Fleet Dashboard<br/>(Leaflet Map + Status Feed)"]
    Go -.->|OTLP Traces & /metrics| Obs["Observability Stack<br/>Jaeger + Prometheus + Grafana"]
    KT -.->|OTLP Traces & /actuator/prometheus| Obs
```

### Why a Polyglot Architecture (`Go` + `Kotlin`)?

* **Edge Ingestion in Go (`telemetry-service`):** Telemetry ingestion is I/O-bound, high-concurrency, and stateless. Go's small memory footprint, fast startup, and native goroutine worker pool (`internal/worker/pool.go`) allow a single instance to validate payloads, apply token-bucket rate limiting, and buffer spikes in memory before publishing to RabbitMQ with `p95 < 10ms` latency.
* **Core Domain & Spatial Analytics in Kotlin (`logistics-service`):** Fleet management, parcel lifecycle state machines, transactional audit history (`status_history`), and spatial distance calculations benefit from **Spring Boot**, **Hibernate Spatial / JTS**, **Flyway** schema migrations, and Kotlin's expressive type system.

---

## Data Model (Entity-Relationship Diagram)

```mermaid
erDiagram
    DRIVER ||--|| VEHICLE : drives
    DRIVER |o--o{ PARCEL : "assigned to"
    VEHICLE ||--o{ TELEMETRY_EVENT : generates
    ROUTE ||--o{ PARCEL : covers
    PARCEL ||--|{ STATUS_HISTORY : tracks
    PARCEL ||--|| ADDRESS : "sender address"
    PARCEL ||--|| ADDRESS : "recipient address"
    ROUTE ||--|| ADDRESS : origin
    ROUTE ||--|| ADDRESS : destination

    DRIVER {
        uuid driver_uid PK
        uuid vehicle_uid FK
        string fullname
        string license_number
    }

    VEHICLE {
        uuid vehicle_uid PK
        string plate_number
        enum type
        bool active
    }

    ROUTE {
        uuid route_uid PK
        uuid origin_uid FK
        uuid dest_uid FK
        jsonb waypoints
    }

    PARCEL {
        uuid parcel_uid PK
        uuid route_uid FK
        uuid driver_uid FK
        string tracking_code
        uuid sender_uid FK
        uuid recipient_uid FK
        enum status
        timestampz pickup_time
        timestampz dropoff_time
    }

    ADDRESS {
        uuid address_uid PK
        string full_name
        geography coordinates
        string street
        string city
        string postal_code
        string country
        jsonb details
    }

    TELEMETRY_EVENT {
        uuid event_uid PK
        uuid vehicle_uid FK
        geography location
        timestampz recorded_at
    }

    STATUS_HISTORY {
        uuid status_history_uid PK
        uuid parcel_uid FK
        enum status
        enum old_status
        timestampz from
        timestampz to
        string reason
    }
```

---

## Quick Start (Docker Compose)

The entire stack—including both microservices, **PostGIS**, **Redis**, **RabbitMQ**, **Jaeger**, **Prometheus**, and **Grafana**—starts with a single command:

```bash
docker compose up -d --build
```

### Local Endpoints & Dashboards

| Component | URL | Credentials / Notes |
| :--- | :--- | :--- |
| **Live Fleet Map Dashboard** | `http://localhost:8080/dashboard.html` | Real-time WebSocket map & parcel status feed |
| **Logistics Service (Kotlin)** | `http://localhost:8080/api/v1/dashboard/...` | REST API (`drivers`, `parcels`, `parcel/assign`) |
| **Telemetry Service (Go)** | `http://localhost:8090/api/v1/telemetry` | Ingestion endpoint (`/healthz`, `/metrics`) |
| **Grafana Dashboards** | `http://localhost:3000` | Pre-provisioned JVM, Go & throughput dashboards |
| **Jaeger Distributed Tracing** | `http://localhost:16686` | End-to-end OTLP traces (`telemetry-service` → `logistics-service`) |
| **RabbitMQ Management UI** | `http://localhost:15672` | `admin` / `password` |
| **Prometheus UI** | `http://localhost:9090` | Scrapes both Go and Spring Actuator metrics |

---

## Load Testing & Fleet Simulation (k6)

The repository includes [k6](https://k6.io/) load-testing and live simulation scripts under [`telemetry-service/load_test/`](telemetry-service/load_test/) and targets in [`telemetry-service/Makefile`](telemetry-service/Makefile) that provision drivers, routes, and parcels and stream continuous GPS coordinates:

```bash
cd telemetry-service

# Quick live simulation (10 drivers, 10 parcels)
make sim-small

# Full live demo (100 drivers, 100 parcels)
make sim-full

# High-concurrency stress test (500 drivers, 500 parcels)
make sim-stress

# 2,000-driver ingestion benchmark
make load-test-full
```

---

## Documentation & Architecture Decisions

* **OpenAPI 3.0 Specification:** [`docs/api/telemetry-openapi.yaml`](docs/api/telemetry-openapi.yaml)
* **ADR — REST vs. gRPC for Telemetry Ingestion:** [`docs/decisions/grpc-vs-rest.md`](docs/decisions/grpc-vs-rest.md) *(evaluates payload size, throughput trade-offs, and the v2 Protobuf contract)*

---

## Project Structure

```text
gps-logistics-tracker/
├── docker-compose.yml            # Full local stack (Go, Kotlin, PostGIS, Redis, RabbitMQ, Jaeger, Prometheus, Grafana)
├── docs/
│   ├── api/                      # OpenAPI 3.0 specification for telemetry ingestion
│   └── decisions/                # Architecture Decision Records (ADRs)
├── monitoring/                   # Prometheus scrape config & Grafana dashboard provisioning
├── telemetry-service/            # Go 1.24 ingestion microservice
│   ├── cmd/server/               # HTTP server entrypoint & graceful shutdown
│   ├── internal/
│   │   ├── handler/              # REST ingestion handlers
│   │   ├── middleware/           # Rate limiter, API key auth, Zap logger, Prometheus metrics
│   │   ├── queue/                # RabbitMQ AMQP publisher
│   │   ├── telemetry/            # OpenTelemetry OTLP tracer setup
│   │   └── worker/               # Concurrent goroutine worker pool & buffered channel
│   └── load_test/                # k6 fleet simulation & stress-test suites
└── logistics-service/            # Kotlin 2.2 / Spring Boot 4 domain microservice
    └── src/main/
        ├── kotlin/.../           # Domain modules (driver, parcel, route, address, telemetryevent), WebSockets & Redis cache
        └── resources/
            ├── db/migration/     # Flyway SQL/PostGIS migrations
            └── static/           # Live WebSocket fleet map dashboard
```

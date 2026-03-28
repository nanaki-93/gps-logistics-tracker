
# ── Yamato Simulation Makefile ─────────────────────────────────────────────
# Usage:
#   make sim-small    → 10 drivers, 10 parcels (quick test)
#   make sim-medium   → 50 drivers, 50 parcels
#   make sim-full     → 100 drivers, 100 parcels (full demo)
#   make sim-stress   → 500 drivers, 500 parcels (stress test)

TELEMETRY_URL  ?= http://localhost:8090
LOGISTICS_URL  ?= http://localhost:8080
API_KEY        ?= key-truck-001
GPS_INTERVAL   ?= 1

.PHONY: load-test-small load-test-full sim-small sim-medium sim-full sim-stress

load-test-small:
	k6 run \
	  -e BASE_URL=http://localhost:8090 \
	  -e LOGISTICS_URL=http://localhost:8080 \
	  -e API_KEY=key-truck-001 \
	  -e DRIVER_COUNT=50 \
	  load_test/simulation.js

load-test-full:
	k6 run \
	  -e BASE_URL=http://localhost:8090 \
	  -e LOGISTICS_URL=http://localhost:8080 \
	  -e API_KEY=key-truck-001 \
	  -e DRIVER_COUNT=2000 \
	  --out json=load_test/results/$(shell date +%Y%m%d_%H%M%S).json \
	  load_test/simulation.js



sim-small:
	k6 run \
	  -e TELEMETRY_URL=$(TELEMETRY_URL) \
	  -e LOGISTICS_URL=$(LOGISTICS_URL) \
	  -e API_KEY=$(API_KEY) \
	  -e DRIVER_COUNT=10 \
	  -e PARCEL_COUNT=10 \
	  -e GPS_INTERVAL=$(GPS_INTERVAL) \
	  load_test/simulation-live.js

sim-medium:
	k6 run \
	  -e TELEMETRY_URL=$(TELEMETRY_URL) \
	  -e LOGISTICS_URL=$(LOGISTICS_URL) \
	  -e API_KEY=$(API_KEY) \
	  -e DRIVER_COUNT=50 \
	  -e PARCEL_COUNT=50 \
	  -e GPS_INTERVAL=$(GPS_INTERVAL) \
	  load_test/simulation-live.js

sim-full:
	k6 run \
	  -e TELEMETRY_URL=$(TELEMETRY_URL) \
	  -e LOGISTICS_URL=$(LOGISTICS_URL) \
	  -e API_KEY=$(API_KEY) \
	  -e DRIVER_COUNT=100 \
	  -e PARCEL_COUNT=100 \
	  -e GPS_INTERVAL=$(GPS_INTERVAL) \
	  --out json=load_test/results/sim-$(shell date +%Y%m%d_%H%M%S).json \
	  load_test/simulation-live.js

sim-stress:
	k6 run \
	  -e TELEMETRY_URL=$(TELEMETRY_URL) \
	  -e LOGISTICS_URL=$(LOGISTICS_URL) \
	  -e API_KEY=$(API_KEY) \
	  -e DRIVER_COUNT=500 \
	  -e PARCEL_COUNT=500 \
	  -e GPS_INTERVAL=$(GPS_INTERVAL) \
	  --out json=load_test/results/stress-$(shell date +%Y%m%d_%H%M%S).json \
		load_test/simulation-live.js

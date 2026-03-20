# Makefile at repo root

.PHONY: load-test-small load-test-full

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
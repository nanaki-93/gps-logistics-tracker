import http from 'k6/http';
import { sleep } from 'k6';
import { Counter, Gauge } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import { parcels, zones, driverNames } from './data/generated-data.js';

// ── Metrics ────────────────────────────────────────────────────────────────
const eventsPublished = new Counter('gps_events_published');
const assignedParcels = new Counter('parcels_assigned');
const createdParcels = new Counter('parcels_created');
const activeDriversG = new Gauge('active_drivers');

// ── Config ─────────────────────────────────────────────────────────────────
const TELEMETRY_URL = __ENV.TELEMETRY_URL || 'http://localhost:8090';
const LOGISTICS_URL = __ENV.LOGISTICS_URL || 'http://localhost:8080';
const API_KEY = __ENV.API_KEY || 'key-truck-001';
const DRIVER_COUNT = parseInt(__ENV.DRIVER_COUNT || '100');
const PARCEL_COUNT = parseInt(__ENV.PARCEL_COUNT || String(parcels.length));
const GPS_INTERVAL_S = parseFloat(__ENV.GPS_INTERVAL || '1');

const DASHBOARD = `${LOGISTICS_URL}/api/v1/dashboard`;
const TELEMETRY = `${TELEMETRY_URL}/api/v1/telemetry`;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
    setupTimeout: '10m',
    scenarios: {
        drivers: {
            executor: 'constant-vus',
            vus: DRIVER_COUNT,
            duration: '60m',
            gracefulStop: '30s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        http_req_duration: ['p(95)<2000'],
    },
};

// ── HTTP helpers ───────────────────────────────────────────────────────────
function dashPost(path, body) {
  return http.post(`${DASHBOARD}${path}`, JSON.stringify(body), { headers: JSON_HEADERS });
}

function dashGet(path) {
  return http.get(`${DASHBOARD}${path}`, { headers: JSON_HEADERS });
}

function sendGpsPing(driverUid, lat, lng) {
  if (lat == null || lng == null) return false;

  const res = http.post(
    TELEMETRY,
    JSON.stringify({
      driverUid,
      lat: parseFloat(Number(lat).toFixed(6)),
      lng: parseFloat(Number(lng).toFixed(6)),
      recordedAt: new Date().toISOString(),
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': API_KEY,
      },
    },
  );

  if (res.status === 202) eventsPublished.add(1);
  return res.status === 202;
}

function createDriver(index) {
  const name = `${driverNames[index % driverNames.length]} ${index}`;
  const zone = zones[index % zones.length];

  const res = dashPost('/driver', {
    fullName: name,
    email: `driver${index}@yamato.co.jp`,
    phone: `090-${String(index).padStart(4, '0')}-${String((index * 7) % 10000).padStart(4, '0')}`,
    licenseNumber: `T${String(index).padStart(7, '0')}`,
  });

  if (res.status === 200 || res.status === 201) {
    const body = JSON.parse(res.body);
    return {
      driverUid: body.driverUid,
      name,
      startLat: zone.lat + (Math.random() - 0.5) * 0.002,
      startLng: zone.lng + (Math.random() - 0.5) * 0.002,
    };
  }

  console.error(`Failed to create driver ${name}: ${res.status} ${res.body}`);
  return null;
}

function makeAddress(fullName, zone) {
  return {
    fullName,
    coordinates: {
      lat: zone.lat,
      lng: zone.lng,
    },
    street: `${zone.name} St.`,
    city: 'Tokyo',
    postalCode: `${String(randomIntBetween(1000000, 9999999))}`,
    country: 'JP',
    details: `${zone.name} delivery point`,
  };
}

function createParcelPayload(parcel) {
  return {
    trackingCode: parcel.trackingCode,
    sender: makeAddress(`Sender ${parcel.index}`, parcel.senderZone),
    receiver: makeAddress(`Receiver ${parcel.index}`, parcel.receiverZone),
    route: {
      origin: makeAddress(`Route Origin ${parcel.index}`, parcel.senderZone),
      destination: makeAddress(`Route Destination ${parcel.index}`, parcel.receiverZone),
      waypoints: parcel.steps.map((step, idx) => ({
        order: idx + 1,
        coordinates: {
          lat: step.lat,
          lng: step.lng,
        },
        label: step.label || `step-${idx + 1}`,
      })),
    },
  };
}

function createParcel(parcel) {
  const res = dashPost('/parcel', createParcelPayload(parcel));

  if (res.status === 200 || res.status === 201) {
    createdParcels.add(1);
    return res.body && res.body.trim().length > 0 ? JSON.parse(res.body) : null;
  }

  console.error(`Failed to create parcel ${parcel.index}: ${res.status} ${res.body}`);
  return null;
}

function assignParcelToDriver(parcelUid, driverUid) {
  const res = dashPost('/parcel/assign', { parcelUid, driverUid });
  const ok = res.status === 200 || res.status === 201;
  if (ok) assignedParcels.add(1);
  return ok;
}

function fetchUnassignedParcels() {
  const res = dashGet('/parcels');
  if (res.status !== 200) return [];

  const parsed = JSON.parse(res.body);
  const body = parsed.content || [];

  return body
    .filter(p => !p.driver && p.status === 'TO_BE_ASSIGNED')
    .map(p => ({
      parcelUid: p.parcelUid,
    }));
}

function pickRandomIndex(length) {
  return length <= 1 ? 0 : randomIntBetween(0, length - 1);
}

function normalizeSteps(parcel) {
  return parcel.steps.map(step => ({
    lat: step.lat,
    lng: step.lng,
  }));
}

// ── Setup ──────────────────────────────────────────────────────────────────
export function setup() {
  console.log(`\n🚚 Yamato Live Simulation`);
  console.log(`   Drivers:   ${DRIVER_COUNT}`);
  console.log(`   Parcels:   ${PARCEL_COUNT}`);
  console.log(`   Interval:  ${GPS_INTERVAL_S}s per GPS ping`);
  console.log(`   Data:      precomputed tiny-step routes\n`);

  console.log('Creating parcels from generated data...');
  const created = [];

  for (let i = 0; i < PARCEL_COUNT; i++) {
    const parcel = parcels[i];
    if (!parcel) break;

    const response = createParcel(parcel);
    if (response) {
      created.push({
        parcelUid: response.parcelUid || parcel.parcelUid,
        source: parcel,
      });
    }

    if (i % 100 === 0) console.log(`  ${i}/${PARCEL_COUNT} parcels created`);
    sleep(0.005);
  }

  console.log(`  ✓ ${created.length} parcels created\n`);

  console.log('Creating drivers...');
  const drivers = [];
  for (let i = 0; i < DRIVER_COUNT; i++) {
    const driver = createDriver(i);
    if (driver) drivers.push(driver);

    if (i % 20 === 0) console.log(`  ${i}/${DRIVER_COUNT} drivers created`);
    sleep(0.01);
  }

  console.log(`  ✓ ${drivers.length} drivers created\n`);

  const available = fetchUnassignedParcels();
  console.log(`  ✓ ${available.length} unassigned parcels found in the system`);

  const parcelPool = created.map(p => ({
    parcelUid: p.parcelUid,
    source: p.source,
  }));

  const assignments = [];
  const activeCount = Math.min(drivers.length, available.length, parcelPool.length);

  for (let i = 0; i < activeCount; i++) {
    const driver = drivers[i];
    const parcelIndex = pickRandomIndex(parcelPool.length);
    const parcel = parcelPool.splice(parcelIndex, 1)[0];

    if (!parcel) continue;

    const ok = assignParcelToDriver(available[i].parcelUid, driver.driverUid);
    if (ok) {
      assignments.push({
        driverUid: driver.driverUid,
        name: driver.name,
        currentLat: driver.startLat,
        currentLng: driver.startLng,
        parcelUid: available[i].parcelUid,
        steps: normalizeSteps(parcel.source),
        stepIndex: 0,
      });
    }
  }

  activeDriversG.add(assignments.length);

  console.log(`  ✓ ${assignments.length} parcels assigned to drivers`);
  console.log(`\n✅ Ready — simulation starting\n`);

  return {
    drivers,
    assignments,
    parcelPool,
  };
}

// ── Main loop ──────────────────────────────────────────────────────────────
function takeNextParcel(state, data) {
  if (data.parcelPool.length === 0) return false;

  const idx = pickRandomIndex(data.parcelPool.length);
  const next = data.parcelPool.splice(idx, 1)[0];
  if (!next) return false;

  state.parcelUid = next.parcelUid;
  state.steps = normalizeSteps(next.source);
  state.stepIndex = 0;
  state.delivered = false;
  return true;
}

export default function (data) {
  const driver = data.assignments[(__VU - 1) % data.assignments.length];
  if (!driver) {
    sleep(GPS_INTERVAL_S);
    return;
  }

  if (!globalThis._state) {
    globalThis._state = {
      currentLat: driver.currentLat,
      currentLng: driver.currentLng,
      parcelUid: driver.parcelUid,
      steps: driver.steps || [],
      stepIndex: driver.stepIndex || 0,
      delivered: false,
    };
  }

  const s = globalThis._state;

  if (!s.steps || s.stepIndex >= s.steps.length) {
    s.delivered = true;
  }

  if (s.delivered) {
    if (!takeNextParcel(s, data)) {
      sleep(GPS_INTERVAL_S);
      return;
    }
  }

  const step = s.steps[s.stepIndex];
  if (!step) {
    s.delivered = true;
    sleep(GPS_INTERVAL_S);
    return;
  }

  s.currentLat = step.lat;
  s.currentLng = step.lng;

  sendGpsPing(driver.driverUid, s.currentLat, s.currentLng);
  s.stepIndex++;

  if (s.stepIndex >= s.steps.length) {
    s.delivered = true;
    console.log(`✅ ${driver.name} delivery complete for parcel ${s.parcelUid}`);
  }

  sleep(GPS_INTERVAL_S);
}

// ── Teardown ───────────────────────────────────────────────────────────────
export function teardown(data) {
  console.log('\n📊 Simulation ended');
  console.log(`   ${data.assignments.length} drivers were active`);
  console.log('   Dashboard: http://localhost:8081/dashboard.html');
  console.log('   Jaeger:    http://localhost:16686\n');
}

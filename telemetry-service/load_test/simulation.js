import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ── Metrics ────────────────────────────────────────────────────────────────
const acceptedEvents  = new Counter('accepted_events');
const rateLimited     = new Counter('rate_limited');
const queueFull       = new Counter('queue_full');
const rejectedEvents  = new Counter('rejected_events');
const publishDuration = new Trend('publish_duration_ms', true);
const errorRate       = new Rate('error_rate');

// ── Config ─────────────────────────────────────────────────────────────────
const BASE_URL      = __ENV.BASE_URL      || 'http://localhost:8090';
const LOGISTICS_URL = __ENV.LOGISTICS_URL || 'http://localhost:8080';
const API_KEY       = __ENV.API_KEY       || 'key-truck-001';
const DRIVER_COUNT  = parseInt(__ENV.DRIVER_COUNT || '200');

export const options = {
    scenarios: {
        trucks: {
            executor:     'constant-vus',
            vus:          DRIVER_COUNT,   // one VU per driver
            duration:     '5m',
            gracefulStop: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<300'],
        error_rate:        ['rate<0.01'],
        queue_full:        ['count<50'],
    },
};

// ── Tokyo delivery zones ───────────────────────────────────────────────────
const zones = [
    { lat: 35.6895, lng: 139.6917 },  // Shinjuku
    { lat: 35.6580, lng: 139.7016 },  // Shibuya
    { lat: 35.7022, lng: 139.7741 },  // Akihabara
    { lat: 35.6284, lng: 139.7387 },  // Shinagawa
    { lat: 35.7295, lng: 139.7109 },  // Ikebukuro
    { lat: 35.7141, lng: 139.7774 },  // Ueno
    { lat: 35.6702, lng: 139.7027 },  // Harajuku
    { lat: 35.6628, lng: 139.7313 },  // Roppongi
    { lat: 35.7148, lng: 139.7967 },  // Asakusa
    { lat: 35.6197, lng: 139.7754 },  // Odaiba
];

// ── Setup — runs ONCE before the load test ─────────────────────────────────
// Creates all drivers and vehicles via the Kotlin REST API.
// Returns the driver UUID list — k6 passes it to every VU automatically.
export function setup() {
    console.log(`Seeding ${DRIVER_COUNT} drivers and vehicles...`);

    const drivers = [];

    for (let i = 0; i < DRIVER_COUNT; i++) {
        // 1. Create vehicle
        const driverRes = http.post(
            `${LOGISTICS_URL}/api/v1/dashboard/driver`,
            JSON.stringify({
                fullName: `YMT-${String(i).padStart(5, '0')}`,
                email: `YMT-${String(i).padStart(5, '0')}@gmail.com`,
                phone: `+81${String(i).padStart(7, '0')}`,
                licenseNumber: `YMT-LIC-${String(i).padStart(5, '0')}`
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        check(driverRes, { 'driver created (201)': (r) => r.status === 201 });
        const driver = JSON.parse(driverRes.body);

        // Assign starting position from zone
        const zone = zones[i % zones.length];
        drivers.push({
            driverUid: driver.driverUid,
            lat:       zone.lat + (Math.random() - 0.5) * 0.02,
            lng:       zone.lng + (Math.random() - 0.5) * 0.02,
        });

        // Small pause to avoid overwhelming the setup phase
        if (i % 50 === 0) {
            console.log(`  seeded ${i}/${DRIVER_COUNT} drivers`);
            sleep(0.1);
        }
    }

    console.log(`Setup complete — ${drivers.length} drivers ready`);
    return { drivers };  // passed to default() and teardown()
}

// ── Per-VU state ───────────────────────────────────────────────────────────
let currentLat = null;
let currentLng = null;

// ── Main load test — runs repeatedly for each VU ───────────────────────────
export default function (data) {
    // Each VU is assigned one driver by index
    // __VU is 1-based so subtract 1
    const driver = data.drivers[(__VU - 1) % data.drivers.length];

    // Initialise position from setup data on first iteration
    if (currentLat === null) {
        currentLat = driver.lat;
        currentLng = driver.lng;
    }

    // Realistic truck movement (~30km/h)
    currentLat += (Math.random() - 0.5) * 0.0002;
    currentLng += (Math.random() - 0.5) * 0.0002;

    // Stay within Japan bounding box
    currentLat = Math.max(24.0, Math.min(46.0, currentLat));
    currentLng = Math.max(122.0, Math.min(154.0, currentLng));

    const payload = JSON.stringify({
        driverUid: driver.driverUid,   // real UUID from DB
        lat:        parseFloat(currentLat.toFixed(6)),
        lng:        parseFloat(currentLng.toFixed(6)),
        recordedAt:  new Date().toISOString().replace(/\.\d{3}Z$/, 'Z'),
    });

    const start = Date.now();

    const res = http.post(
        `${BASE_URL}/api/v1/telemetry`,
        payload,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-API-Key':    API_KEY,
            },
            timeout: '2s',
        }
    );

    publishDuration.add(Date.now() - start);

    switch (res.status) {
        case 202: acceptedEvents.add(1); break;
        case 429: rateLimited.add(1);    break;
        case 503: queueFull.add(1);      break;
        default:  rejectedEvents.add(1); break;
    }

    const ok = check(res, {
        'accepted (202)':        (r) => r.status === 202,
        'response time <300ms':  (r) => r.timings.duration < 300,
    });

    errorRate.add(!ok);

    sleep(1);  // 1 ping per second per truck
}

// ── Teardown — runs ONCE after the load test ───────────────────────────────
export function teardown(data) {
    console.log(`Teardown: cleaning up ${data.drivers.length} drivers`);

    // for (const driver of data.drivers) {
    //     http.del(`${LOGISTICS_URL}/api/v1/dashboard/driver/${driver.driverUid}`);
    // }

    console.log('Teardown complete');
}
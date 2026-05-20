// k6 load scenario for Trading BFF.
// Запуск:
//   k6 run k6/load.js -e BASE=http://localhost:8080
// Перед запуском поднять стек: docker compose up -d postgres redis && ./gradlew :apps:bff:run

import http from 'k6/http';
import ws from 'k6/ws';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const BASE = __ENV.BASE || 'http://localhost:8080';
const placeOrderLatency = new Trend('place_order_latency_ms');

export const options = {
  scenarios: {
    read_instruments: {
      executor: 'constant-arrival-rate',
      rate: 5000,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 200,
      maxVUs: 500,
      exec: 'readInstruments',
    },
    place_orders: {
      executor: 'constant-arrival-rate',
      rate: 100,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 50,
      maxVUs: 100,
      exec: 'placeOrders',
    },
  },
  thresholds: {
    'http_req_duration{name:instruments}': ['p(95)<50'],
    'http_req_duration{name:portfolio}': ['p(95)<100'],
    'place_order_latency_ms': ['p(95)<200'],
  },
};

let TOKEN = null;

export function setup() {
  const email = `loadtest+${Date.now()}@example.com`;
  const reg = http.post(`${BASE}/api/v1/auth/register`,
    JSON.stringify({ email, password: 'Strong1Pass', displayName: 'load' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(reg, { 'register 201': r => r.status === 201 });
  return { token: reg.json().accessToken };
}

export function readInstruments(data) {
  const res = http.get(`${BASE}/api/v1/instruments`, {
    headers: { Authorization: `Bearer ${data.token}` },
    tags: { name: 'instruments' },
  });
  check(res, { '200': r => r.status === 200 });
}

export function placeOrders(data) {
  const start = Date.now();
  const res = http.post(`${BASE}/api/v1/orders`,
    JSON.stringify({ instrumentId: 1, side: 'BUY', type: 'MARKET', quantity: 10 }),
    {
      headers: {
        Authorization: `Bearer ${data.token}`,
        'Content-Type': 'application/json',
        'Idempotency-Key': uuidv4(),
      },
      tags: { name: 'orders' },
    });
  placeOrderLatency.add(Date.now() - start);
  check(res, { '201|422': r => [201, 422].includes(r.status) });
  sleep(0.1);
}

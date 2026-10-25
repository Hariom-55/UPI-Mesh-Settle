

import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };
const settled = new Counter('settled_payments');

export const options = {
  scenarios: {
    reads: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
      exec: 'readAccounts',
    },
    pipeline: {
      executor: 'per-vu-iterations',
      vus: 1,
      iterations: 200,
      maxDuration: '5m',
      exec: 'fullPipeline',
      startTime: '40s',
    },
  },
  thresholds: {
    // Declaring these makes k6 print per-scenario latency in the summary.
    'http_req_duration{scenario:reads}': ['p(95)<3000'],
    'http_req_duration{scenario:pipeline}': ['p(95)<5000'],
    'http_req_failed{scenario:reads}': ['rate<0.01'],
  },
};

export function readAccounts() {
  const res = http.get(`${BASE}/api/accounts`);
  check(res, { 'accounts 200': (r) => r.status === 200 });
}

export function fullPipeline() {
  const send = http.post(
    `${BASE}/api/demo/send`,
    JSON.stringify({
      senderVpa: 'alice@demo',
      receiverVpa: 'bob@demo',
      amount: 1,
      pin: '1234',
      ttl: 5,
      startDevice: 'phone-bridge', // the device that has internet
    }),
    JSON_HEADERS
  );
  const sendOk = check(send, { 'send 200': (r) => r.status === 200 });

  if (sendOk) {
    const flush = http.post(`${BASE}/api/mesh/flush`, null, JSON_HEADERS);
    const ok = check(flush, {
      'flush 200': (r) => r.status === 200,
      'one upload': (r) => r.json('uploadsAttempted') === 1,
    });
    if (ok) settled.add(1);
  }

  // Clear held packets and the idempotency cache so the next pass is clean.
  http.post(`${BASE}/api/mesh/reset`, null, JSON_HEADERS);
}

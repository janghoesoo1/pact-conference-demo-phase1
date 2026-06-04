import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const sessionLatency = new Trend('session_latency');
const attendeeLatency = new Trend('attendee_latency');

export const options = {
  stages: [
    { duration: '10s', target: 10 },   // ramp up
    { duration: '30s', target: 10 },   // steady
    { duration: '10s', target: 50 },   // spike
    { duration: '10s', target: 10 },   // recover
    { duration: '10s', target: 0 },    // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],   // 95% of requests under 500ms
    errors: ['rate<0.1'],               // error rate under 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  // GET sessions
  let res = http.get(`${BASE_URL}/api/sessions`, {
    headers: { Accept: 'application/json' },
  });
  check(res, { 'sessions status 200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);
  sessionLatency.add(res.timings.duration);

  sleep(0.5);

  // GET attendees
  res = http.get(`${BASE_URL}/api/attendees`, {
    headers: { Accept: 'application/json' },
  });
  check(res, { 'attendees status 200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);
  attendeeLatency.add(res.timings.duration);

  sleep(0.5);

  // GET proposals
  res = http.get(`${BASE_URL}/api/proposals`, {
    headers: { Accept: 'application/json' },
  });
  check(res, { 'proposals status 200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);

  sleep(0.5);

  // GET v2 sessions (versioned API)
  res = http.get(`${BASE_URL}/api/v2/sessions`, {
    headers: { Accept: 'application/json' },
  });
  check(res, { 'v2 sessions status 200': (r) => r.status === 200 });
  errorRate.add(res.status !== 200);

  sleep(1);
}

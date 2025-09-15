import http from 'k6/http';
import { sleep, check } from 'k6';

export let options = {
  thresholds: {
    http_req_duration: ['p(95)<500'],   // 95% 요청이 500ms 이내
    http_req_failed: ['rate<0.01'],     // 에러율 1% 미만
  },
  stages: [
    { duration: '1m', target: 1000 },  // 1분간 1,000명
    { duration: '2m', target: 3000 },  // 2분간 3,000명
    { duration: '5m', target: 3000 },  // 5분간 유지
    { duration: '2m', target: 0 },     // 2분간 감소
  ],
};

const BASE_URL = 'http://localhost:8080/api/queue';  // Spring Boot 직접 호출
const EVENT_ID = 'GD2501563';

export default function () {
  // 1. Join 요청 (RequestBody 필요)
  let payload = JSON.stringify({
    eventId: EVENT_ID,
  });
  let params = { headers: { 'Content-Type': 'application/json' } };

  let joinRes = http.post(`${BASE_URL}/join`, payload, params);
  check(joinRes, { 'join success': (r) => r.status === 200 });

  if (joinRes.status !== 200) return;

  let body = joinRes.json();
  let queueId = body.queueId;

  // 2. Status 요청
  let statusRes = http.get(`${BASE_URL}/checkStatus/${EVENT_ID}/${queueId}`);
  check(statusRes, { 'status ok': (r) => r.status === 200 });

  // 3. Heartbeat 요청
  let hbRes = http.post(`${BASE_URL}/heartbeat/${EVENT_ID}/${queueId}`);
  check(hbRes, { 'heartbeat ok': (r) => r.status === 200 });

  // 유저별 랜덤 대기
  sleep(Math.random() * 5 + 5);
}

import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 설정 (Options)
// Why: 목표 부하를 정의합니다. 갑자기 쏘지 않고 서서히 올립니다.
export const options = {
  stages: [
    { duration: '30s', target: 50 }, // 30초 동안 가상 유저(VU)를 0 -> 50명까지 늘림 (Ramp-up)
    { duration: '1m', target: 50 },  // 1분 동안 50명 유지 (Load)
    { duration: '10s', target: 0 },  // 10초 동안 0명으로 줄임 (Ramp-down)
  ],
  // 중요: 성공/실패 기준 정의 (SLA)
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내에 끝나야 성공
    http_req_failed: ['rate<0.01'],   // 에러율이 1% 미만이어야 성공
  },
};

function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
}

// 2. 가상 유저 행동 정의 (Main Logic)
export default function () {
  const BASE_URL = 'http://localhost:8400';

  const userId = generateUUID();
  const auctionId = "3fa85f64-5717-4562-b3fc-2c963f66afa9";

  const params = {
    headers: {
      'X-User-Id': userId
    },
  };
  // POST 요청

  try {
    const res = http.post(`${BASE_URL}/v1/queues/${auctionId}`, null, params);

    if (res.status !== 200)
      console.log(`Error Status: ${res.status}, Body: ${res.body}`);

    // 검증 (Check): 상태 코드가 200인지 확인
    check(res, {
      '입장 요청 성공': (r) => r.status === 200,
      '응답 시간 < 500ms': (r) => r.timings.duration < 500,
    });
  } catch (error) {
    console.log(`Error:`, error)
  }
  // TODO: 응답에서 '대기열 토큰'을 추출하는 로직이 필요할 수 있습니다.
  // const token = res.json('token');

  // --- [Step 2] 부하 조절 (Sleep) ---
  // Why: 사용자가 광클하는 것을 방지하고, 현실적인 부하를 주기 위해 대기
  sleep(1);
}
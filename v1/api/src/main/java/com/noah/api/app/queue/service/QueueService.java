package com.noah.api.app.queue.service;

 
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.noah.api.app.queue.entity.BulkJoinRequest;
import com.noah.api.app.queue.entity.QueueJoinResponse;
import com.noah.api.app.queue.entity.QueueStatusResponse;
import com.noah.api.app.queue.provider.TokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class QueueService {
	
    private final QueueServiceFlagRedis queueServiceFlagRedis;
	private final TokenProvider tokenProvider;
	private final StringRedisTemplate redis;

	// 하트비트 TTL
    @Value("${queue.system.waitSession.validitySecond}")     
	private int heartbeatDurationSecond;
    
    @Value("${queue.system.token.validityMillis}")
    private long tokenValidityMillis;
    
	// 최대 표시 할 대기 시간
    @Value("${queue.system.maxEtaMinutes}")     
	private int maxEtaMinutes;
    
	// 평균 계산 대기 시간
    @Value("${queue.system.windowMinutes}")     
	private int windowMinutes;
    

    /* Key Helpers */
    private String slotTag(String eventId)                    { return "{" + eventId + "}"; }
    private String zsetKey(String eventId)                    { return "queue"   + slotTag(eventId) + ":zset"; }
    private String seqKey(String eventId)                     { return "queue"   + slotTag(eventId) + ":seq"; }
    private String rateKey(String eventId, long minute)       { return "queue"   + slotTag(eventId) + ":rate:" + minute; }
    private String sessionKey(String eventId, String qid)     { return "session" + slotTag(eventId) + ":" + qid; }
    private String allowedKey(String eventId, String qid)     { return "allowed" + slotTag(eventId) + ":" + qid; }
    private String deviceKey(String eventId, String deviceId) { return "queue"   + slotTag(eventId) + ":device:" + deviceId; }


    /* 대기열 입장: ZADD(O(logN)) */
    public QueueJoinResponse joinQueue(String eventId, HttpServletRequest request, HttpServletResponse response) {
    	
    	if (!queueServiceFlagRedis.isEnabled(eventId)) { // 큐 등록하지 않고 즉시 입장 처리            
            return new QueueJoinResponse(UUID.randomUUID().toString(), 0, 0);
        }
    	
    	// 1) Device ID 추출
        String deviceId = null;
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("DEVICE_ID".equals(c.getName())) {
                    deviceId = c.getValue();
                    break;
                }
            }
        }

        // 2) 없으면 새로 발급 + 쿠키 저장
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            ResponseCookie cookie = ResponseCookie.from("DEVICE_ID", deviceId)
            		.httpOnly(true)                // JS에서는 접근 못 하게 (보안)
                    .secure(false)                 // 로컬 테스트면 false, 운영 HTTPS면 true 쿠키 → 로컬은 secure=false, 운영 HTTPS에서는 secure=true
                    .sameSite(false ? "Strict" : "Lax")               // Strict → 새 탭/리다이렉트 시 문제됨, 로컬 배포 .sameSite("Lax") (또는 크로스 오리진이면 "None"), 운영 배포 .sameSite("None") (크로스 도메인에서 반드시 None)
                    .path("/")
                    .maxAge(Duration.ofDays(30))
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            log.info("새 Device ID 발급: {}", deviceId);
        }
        
        // 3) 기존 queueId 확인
        String existingQid = redis.opsForValue().get(deviceKey(eventId, deviceId));
        if (existingQid != null) {
            Long rank = redis.opsForZSet().rank(zsetKey(eventId), existingQid);
            int position = rank == null ? -1 : rank.intValue() + 1;
            return new QueueJoinResponse(existingQid, position, -1);
        }

        // 4) 새 queueId 발급
        String qid = UUID.randomUUID().toString();
        Long seq = redis.opsForValue().increment(seqKey(eventId));
        if (seq == null) seq = 1L;

        redis.opsForZSet().add(zsetKey(eventId), qid, seq.doubleValue());
        redis.opsForValue().set(sessionKey(eventId, qid), "waiting",
                Duration.ofSeconds(heartbeatDurationSecond));

        // Device → queueId 매핑
        redis.opsForValue().set(deviceKey(eventId, deviceId), qid,
                Duration.ofSeconds(heartbeatDurationSecond));

        Long rank = redis.opsForZSet().rank(zsetKey(eventId), qid);
        int position = rank == null ? -1 : rank.intValue() + 1;
        return new QueueJoinResponse(qid, position, -1);
    }
    
    
    /* 대기열 상태 체크: ZRANK(O(logN)) + 동적 ETA + 청소 */
    public QueueStatusResponse checkStatus(String eventId, String queueId) {
    	
    	log.info("checkStatus: eventId=[{}], queueId=[{}}, enabled[{}]", eventId, queueId, queueServiceFlagRedis.isEnabled(eventId));

        // 1. 대기열 꺼진 상태면 바로 통과
        if (!queueServiceFlagRedis.isEnabled(eventId)) {
            String qid = queueId != null ? queueId : UUID.randomUUID().toString();
            String token = tokenProvider.createToken(eventId, qid);
            return new QueueStatusResponse(0, 0, true, token);
        }

        String zKey       = zsetKey(eventId);
        String allowedKey = allowedKey(eventId, queueId);
        String sessionKey = sessionKey(eventId, queueId);

        // 2. allowedKey 먼저 조회 (허용 토큰 있으면 최우선)
        String token = redis.opsForValue().get(allowedKey);
        
        log.info("checkStatus: allowedKey=[{}], token=[{}}", allowedKey, token);
        
        if (token != null) {
            // 허용 토큰이 있다면 → 만료 임박 여부 확인
            if (tokenProvider.isExpiringSoon(token)) {
                token = tokenProvider.createToken(eventId, queueId);
                redis.opsForValue().set(allowedKey, token, Duration.ofSeconds(heartbeatDurationSecond));
            }
            return new QueueStatusResponse(0, 0, true, token);
        }

        // 3. 세션이 없다면 → 유령 queueId 처리        
        log.info("checkStatus: sessionKey=[{}], hasKey=[{}}", sessionKey, redis.hasKey(sessionKey));
        
        if (!redis.hasKey(sessionKey)) {
            // 안전하게 ZSET에서도 제거
            redis.opsForZSet().remove(zKey, queueId);
            return new QueueStatusResponse(-1, -1, false);
        }

        // 4. 순번 확인
        Long rank = redis.opsForZSet().rank(zKey, queueId);
        log.info("checkStatus: rank=[{}}", rank);
        
        if (rank == null) {
            return new QueueStatusResponse(-1, -1, false);
        }

        int position = rank.intValue() + 1; // set 인텍스 0 부터 시작으로 맨 처음을 1을 설정

        // 5. ETA 계산 (최근 3분 평균 처리율 기반)
        int ratePerMin = recentThroughputPerMinute(eventId, windowMinutes);
        if (ratePerMin <= 0) ratePerMin = 1;

        // 초 단위 ETA (UX 개선)
        int etaMinutes = Math.min(((int) Math.ceil((double) position / Math.max(ratePerMin, 1))), maxEtaMinutes); // 최대 1시간까지만 노출
        log.info("checkStatus: windowMinutes=[{}}, maxEtaMinutes=[{}]", windowMinutes, maxEtaMinutes);

        return new QueueStatusResponse(position, etaMinutes, false, null);
    }

    /* 3분 간격 평균 처리 시간 계산 */
    private int recentThroughputPerMinute(String eventId, int windowMinutes) {
        long nowMin = System.currentTimeMillis() / 60000L;
        long sum = 0;
        for (int i = 0; i < windowMinutes; i++) {
            String k = rateKey(eventId, nowMin - i);
            String v = redis.opsForValue().get(k);
            if (v != null) {
                try { sum += Long.parseLong(v); } catch (NumberFormatException ignored) {}
            }
        }
        return (int)(sum / Math.max(windowMinutes, 1));
    }
    

    /* 
     * 입장 처리: ZPOPMIN + 처리량 카운트 (Lua, 원자적 실행 보장)
     *  - ZSET(대기열)에서 맨 앞 사용자(queueId)를 하나 꺼낸다.
     *  - 분당 허용 카운터(rateKey)를 증가시킨다.
     *  - 허용된 queueId를 리턴한다.
     * 
     * ✅ allowedKey 생성/TTL 관리 → TokenProvider.createToken()에서만 처리
     */
    private static final String ALLOW_NEXT_LUA =
        // KEYS[1] = zKey (대기열 ZSET), KEYS[2] = rateKey
        "local popped = redis.call('ZPOPMIN', KEYS[1], 1) " + 
        "if (popped == nil or #popped == 0) then return nil end " +
        "local qid = popped[1] " +
        // allowedKey 생성 부분 제거 (자바 createToken에서 처리)
        "redis.call('INCR', KEYS[2]) " +
        "redis.call('EXPIRE', KEYS[2], tonumber(ARGV[1])) " +
        "return qid ";

    private final RedisScript<String> ALLOW_NEXT_SCRIPT = RedisScript.of(ALLOW_NEXT_LUA, String.class);

    /**
     * allowNext
     * - 대기열 ZSET에서 맨 앞 사용자 1명 꺼내서 반환
     * - 분당 허용 카운터 증가
     * - JWT 토큰을 발급해서 반환
     */
    public String allowNext(String eventId) {
        if (!queueServiceFlagRedis.isEnabled(eventId)) { 
            // 대기열이 꺼진 상태라면 입장 처리 불필요
            return null;
        }

        try {
            /*
             * KEYS[1] = zsetKey(eventId) → 대기열 ZSET (이벤트별 대기열)
             * KEYS[2] = rateKey(eventId, minute) → 현재 분 단위 허용 카운터
             * ARGV[1] = "7200" → rate TTL (초 단위, 2시간 유지)
             */
            String qid = redis.execute(
                    ALLOW_NEXT_SCRIPT,
                    Arrays.asList(
                        zsetKey(eventId), 
                        rateKey(eventId, (System.currentTimeMillis() / 60000L)) // 현재 시각을 분 단위로 변환
                    ),
                    "7200", // rate TTL
                    slotTag(eventId) // Redis 해시태그(샤딩 고려시 사용)
            );

            if (qid == null) return null;

            // ✅ JWT 토큰 생성 (createToken에서 allowedKey 갱신까지 처리)
            return tokenProvider.createToken(eventId, qid);

        } catch (DataAccessException e) {
            log.error("allowNext 실패", e);
            return null;
        }
    }

    /* 대기순서 연장 HEARTBEAT: 30초마다 호출 권장 */
    public boolean heartbeat(String eventId, String queueId) {
        Boolean exists = redis.hasKey(sessionKey(eventId, queueId));
        if (Boolean.TRUE.equals(exists)) {
            redis.opsForValue().set(sessionKey(eventId, queueId), "waiting", Duration.ofSeconds(heartbeatDurationSecond));
            return true;
        }
        return false;
    }
    
    /* 대기열 이탈: 세션 + ZSET + allowedKey 정리 */
    public boolean leaveQueue(String eventId, String queueId) {
        if (queueId == null) return false;

        String zKey       = zsetKey(eventId);
        String sessionKey = sessionKey(eventId, queueId);
        String allowedKey = allowedKey(eventId, queueId);

        try {
            // 세션 삭제
            redis.delete(sessionKey);
            // ZSET 제거
            redis.opsForZSet().remove(zKey, queueId);
            // allowed 토큰 삭제 (이미 입장 허용된 사용자라면 무효화)
            redis.delete(allowedKey);

            log.info("leaveQueue: eventId={}, queueId={} -> 제거 완료", eventId, queueId);
            return true;
        } catch (Exception e) {
            log.error("leaveQueue 실패: eventId={}, queueId={}", eventId, queueId, e);
            return false;
        }
    }

    /* 부하 테스트용 샘플데이터 입력 (bulk insert: score는 i+1) */
    public String bulkJoinWithPipeline(BulkJoinRequest req) {

        long startJoin = System.currentTimeMillis();
        String eventId   = req.getEventId();
        int count        = req.getCount();
        int batchSize    = 1000;

        for (int start = 0; start < count; start += batchSize) {
            int end = Math.min(start + batchSize, count);
            int finalStart = start;

            redis.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (int i = finalStart; i < end; i++) {
                        String qid = UUID.randomUUID().toString();

                        // ✅ 실제 joinQueue는 seqKey increment 결과를 score로 사용
                        // ✅ 하지만 bulk insert에서는 응답값이 null이므로 i+1을 score로 사용
                        operations.opsForValue().increment(seqKey(eventId)); // 값은 무시
                        operations.opsForZSet().add(zsetKey(eventId), qid, i + 1);
                        // ✅ 세션 생성 (TTL 60분)
                        operations.opsForValue().set(sessionKey(eventId, qid), "waiting", heartbeatDurationSecond, TimeUnit.SECONDS);

                    }
                    return null;
                }
            });
        }

        long endJoin = System.currentTimeMillis();
        return count + "건 등록 완료. 소요 시간: " + (endJoin - startJoin) + " ms";
    }
}

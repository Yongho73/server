package com.noah.api.app.queue.service;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.noah.api.app.queue.component.QueueSystemFlag;
import com.noah.api.app.queue.entity.BulkJoinRequest;
import com.noah.api.app.queue.entity.QueueJoinResponse;
import com.noah.api.app.queue.entity.QueueStatusResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QueueService {

	@Autowired
    private QueueServiceFlagRedis queueServiceFlagRedis;
	
	private final StringRedisTemplate redis;

    public QueueService(StringRedisTemplate redisTemplate) {
        this.redis = redisTemplate;
    }

    /* ---------- Key Helpers ---------- */
    private String zsetKey(String eventId) { return "queue:z:" + eventId; }
    private String seqKey(String eventId)  { return "queue:seq:" + eventId; }
    private String sessionKey(String qid)  { return "session:" + qid; }
    private String allowedKey(String qid)  { return "allowed:" + qid; }
    private String rateKey(String eventId, long epochMinute) { return "allow:rate:" + eventId + ":" + epochMinute; }

    /* ---------- JOIN: ZADD(O(logN)) ---------- */
    public QueueJoinResponse joinQueue(String eventId) {
    	
    	if (!queueServiceFlagRedis.isEnabled(eventId)) {
            // 큐 등록하지 않고 즉시 입장 처리
            return new QueueJoinResponse(UUID.randomUUID().toString(), 0, 0);
        }
    	
        String qid = UUID.randomUUID().toString();

        Long seq = redis.opsForValue().increment(seqKey(eventId)); // score로 사용
        if (seq == null) seq = 1L;

        redis.opsForZSet().add(zsetKey(eventId), qid, seq.doubleValue());
        redis.opsForValue().set(sessionKey(qid), "waiting", Duration.ofMinutes(60)); // heartbeat로 연장

        Long rank = redis.opsForZSet().rank(zsetKey(eventId), qid);
        int position = rank == null ? -1 : rank.intValue() + 1;
        
        // int eta = position * 1; // 기본치(1분/명). 아래 동적 ETA 로 대체 가능
        // 최초 등롟히 예상 시간을 따로 넣지 않고, checkStatus 에서 예상 시간을 따로 구해 최초는 -1 로 등록함

        return new QueueJoinResponse(qid, position, -1);
    }

    /* ---------- STATUS: ZRANK(O(logN)) + 동적 ETA + 청소 ---------- */
    public QueueStatusResponse checkStatus(String eventId, String queueId) {
    	
    	if (!queueServiceFlagRedis.isEnabled(eventId)) {
            // 대기열 꺼진 상태 → 모두 입장
            return new QueueStatusResponse(0, 0, true);
        }
    	
    	String zKey = zsetKey(eventId);

        // 우선 rank 조회
        Long rank = redis.opsForZSet().rank(zKey, queueId);

        if (rank == null) {
            // ZSET엔 없는데 허용 토큰이 있다? => 이미 입장 허용
            Boolean allowed = redis.hasKey(allowedKey(queueId));
            if (Boolean.TRUE.equals(allowed)) return new QueueStatusResponse(0, 0, true);

            // 세션이 죽었다면 지연된 청소 or 잘못된 queueId
            return new QueueStatusResponse(-1, -1, false);
        }

        // 세션 없는 유저는 지연 청소 (부하 적음, O(logN))
        if (!Boolean.TRUE.equals(redis.hasKey(sessionKey(queueId)))) {
            // allowed 가 없다면 유효하지 않으므로 제거
            if (!Boolean.TRUE.equals(redis.hasKey(allowedKey(queueId)))) {
                redis.opsForZSet().remove(zKey, queueId);
                return new QueueStatusResponse(-1, -1, false);
            }
        }

        int position = rank.intValue() + 1;

        // ---- 동적 ETA: 최근 N분 처리량(명/분) 기반 ----
        int ratePerMin = recentThroughputPerMinute(eventId, /*windowMinutes*/3); // 3분 이동평균
        if (ratePerMin <= 0) ratePerMin = 1; // 안전 하한
        int eta = (int)Math.ceil((double)position / ratePerMin);

        // allowed 여부는 토큰으로만 true 처리
        boolean allowed = Boolean.TRUE.equals(redis.hasKey(allowedKey(queueId)));
        return new QueueStatusResponse(position, eta, allowed);
    }

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

    /* ---------- ALLOW NEXT: ZPOPMIN + allowed토큰 + 처리량 카운트 (Lua, 원자) ---------- */
    private static final String ALLOW_NEXT_LUA =
        // KEYS[1]=zKey, KEYS[2]=rateKey, ARGV[1]=allowedTTL, ARGV[2]=rateTTL
        "local popped = redis.call('ZPOPMIN', KEYS[1], 1) " +
        "if (popped == nil or #popped == 0) then return nil end " +
        "local qid = popped[1] " +
        "redis.call('SET', 'allowed:'..qid, 'true', 'EX', tonumber(ARGV[1])) " +
        "redis.call('INCR', KEYS[2]) " +
        "redis.call('EXPIRE', KEYS[2], tonumber(ARGV[2])) " +
        "return qid ";

    private final RedisScript<String> ALLOW_NEXT_SCRIPT = RedisScript.of(ALLOW_NEXT_LUA, String.class);

    public String allowNext(String eventId) {
        
    	if (!queueServiceFlagRedis.isEnabled(eventId)) {
            return null; // 대기열 꺼진 상태에서는 강제 허용 불필요
        }
    	
    	String zKey = zsetKey(eventId);
        String rKey = rateKey(eventId, System.currentTimeMillis() / 60000L); // 분 단위 처리량 키
        
        try {
            // allowed TTL=300s, rateKey TTL=7200s(2시간)
            return redis.execute(ALLOW_NEXT_SCRIPT, java.util.Arrays.asList(zKey, rKey), "300", "7200");
        } catch (DataAccessException e) {
            log.error("allowNext 실패", e);
            return null;
        }
    }

    /* ---------- HEARTBEAT: 30초마다 호출 권장 ---------- */
    public boolean heartbeat(String queueId) {
        Boolean exists = redis.hasKey(sessionKey(queueId));
        if (Boolean.TRUE.equals(exists)) {
            redis.opsForValue().set(sessionKey(queueId), "waiting", Duration.ofMinutes(60));
            return true;
        }
        return false;
    }
    
    /* ---------- 샘플데이터 입력 ---------- */
    public String bulkJoinWithPipeline(BulkJoinRequest req) {
        long startJoin = System.currentTimeMillis();
        String eventId = req.getEventId();
        int count = req.getCount();
        
        log.info("eventId=[{}]", eventId);
        log.info("count=[{}]", count);
        
        
        int batchSize = 1000;
        for (int start = 0; start < count; start += batchSize) {
            int end = Math.min(start + batchSize, count);
            int finalStart = start;

            redis.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (int i = finalStart; i < end; i++) {
                        String qid = UUID.randomUUID().toString();

                        // increment 결과는 무시 (파이프라인에서는 즉시 값 안 나올 수 있음)
                        operations.opsForValue().increment("queue:seq:" + eventId);

                        // score는 그냥 i+1 로 넣어도 무방
                        operations.opsForZSet().add("queue:z:" + eventId, qid, i + 1);

                        // Duration 대신 TimeUnit 사용
                        operations.opsForValue().set("session:" + qid, "waiting", 60, TimeUnit.MINUTES);
                    }
                    return null;
                }
            });
        }

        long endJoin = System.currentTimeMillis();
        return count + "건 등록 완료. 소요 시간: " + (endJoin - startJoin) + " ms";
    }     
}

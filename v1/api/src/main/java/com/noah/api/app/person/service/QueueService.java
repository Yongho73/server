package com.noah.api.app.person.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.noah.api.app.person.entity.queue.QueueJoinResponse;
import com.noah.api.app.person.entity.queue.QueueStatusResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QueueService {

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
        String qid = UUID.randomUUID().toString();

        Long seq = redis.opsForValue().increment(seqKey(eventId)); // score로 사용
        if (seq == null) seq = 1L;

        redis.opsForZSet().add(zsetKey(eventId), qid, seq.doubleValue());
        redis.opsForValue().set(sessionKey(qid), "waiting", Duration.ofMinutes(60)); // heartbeat로 연장

        Long rank = redis.opsForZSet().rank(zsetKey(eventId), qid);
        int position = rank == null ? -1 : rank.intValue() + 1;
        int eta = position * 1; // 기본치(1분/명). 아래 동적 ETA 로 대체 가능

        return new QueueJoinResponse(qid, position, eta);
    }

    /* ---------- STATUS: ZRANK(O(logN)) + 동적 ETA + 청소 ---------- */
    public QueueStatusResponse checkStatus(String eventId, String queueId) {
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
}

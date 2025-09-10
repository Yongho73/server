package com.noah.api.app.queue.scheduler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.noah.api.app.queue.service.QueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AllowNextScheduler {

    private final QueueService queueService;
    private final StringRedisTemplate redis;

    private final List<String> eventIds = List.of("EVT123");

    // 이벤트별 직전 허용자 수 저장 (현재 남아있는 allowed 키 개수)
    private final ConcurrentHashMap<String, AtomicInteger> prevAllowedCount = new ConcurrentHashMap<>();

    // 이벤트별 최초 허용 인원 (이 값은 환경설정/DB로도 뺄 수 있음)
    @SuppressWarnings("serial")
	private final ConcurrentHashMap<String, Integer> initialAllowLimit = new ConcurrentHashMap<>() {{
        put("EVT123", 5); // EVT123 은 최초 10명 허용
    }};

    private String allowedKeyPattern(String eventId) {
        return "allowed{" + eventId + "}:*";
    }

    /** SCAN 으로 allowed 키 개수 조회 */
    private int scanAllowedCount(String eventId) {
        String pattern = allowedKeyPattern(eventId);

        return redis.execute(new org.springframework.data.redis.core.RedisCallback<Integer>() {
            @Override
            public Integer doInRedis(org.springframework.data.redis.connection.RedisConnection connection) {
                int count = 0;

                try (Cursor<byte[]> cursor = connection.keyCommands().scan(
                        ScanOptions.scanOptions()
                                   .match(pattern)
                                   .count(1000)
                                   .build()
                )) {
                    while (cursor.hasNext()) {
                        cursor.next();
                        count++;
                    }
                } catch (Exception e) {
                    log.error("SCAN error for eventId={}", eventId, e);
                }

                return count;
            }
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void checkExpiredAndAllowNext() {
        for (String eventId : eventIds) {

            int current = scanAllowedCount(eventId);
            int prev = prevAllowedCount.computeIfAbsent(eventId, k -> new AtomicInteger(0)).get();
            int initialLimit = initialAllowLimit.getOrDefault(eventId, 0);

            int toAllow = 0;

            // ✅ 1. 서버 최초 기동 시 또는 현재 인원이 limit 미만일 때 → 부족분 보충
            if (current < initialLimit) {
                int need = initialLimit - current;
                toAllow = need;
                log.info("[initial-fill] eventId={} -> 현재 {}명, limit {}명 → {}명 보충",
                         eventId, current, initialLimit, need);
            }
            // ✅ 2. limit 은 이미 채워졌지만 만료자가 생긴 경우
            else if (current < prev) {
                int expiredCount = prev - current;
                toAllow = expiredCount;
                log.info("[expired] eventId={} -> {}명 만료됨, {}명 새로 허용",
                         eventId, expiredCount, toAllow);
            }

            // ✅ allowNext 실행
            for (int i = 0; i < toAllow; i++) {
                String token = queueService.allowNext(eventId);
                log.info("[allowNext] eventId={}, token={}", eventId, token);
            }

            // 현재값 갱신
            prevAllowedCount.get(eventId).set(current + toAllow);
            log.info("eventId=[{}], current(before)=[{}], afterAllow=[{}], prev=[{}], limit=[{}]",
                     eventId, current, current + toAllow, prev, initialLimit);
        }
    }
}

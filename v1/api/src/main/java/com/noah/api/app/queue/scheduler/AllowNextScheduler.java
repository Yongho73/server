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

    private String allowedKeyPattern(String eventId) {
        return "allowed{" + eventId + "}:*";
    }

    /** SCAN 으로 allowed 키 개수 조회 */
    private int scanAllowedCount(String eventId) {
        String pattern = allowedKeyPattern(eventId);
        int count = 0;

        Cursor<byte[]> cursor = null;
        try {
            cursor = redis.execute((org.springframework.data.redis.connection.RedisConnection connection) -> 
                    connection.scan(
                            ScanOptions.scanOptions()
                                       .match(pattern)
                                       .count(1000)
                                       .build()
                    )
            );

            if (cursor != null) {
                while (cursor.hasNext()) {
                    cursor.next();
                    count++;
                }
            }
        } catch (Exception e) {
            log.error("SCAN error for eventId={}", eventId, e);
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception ignore) {}
            }
        }

        return count;
    }


    @Scheduled(fixedDelay = 5000)
    public void checkExpiredAndAllowNext() {
        for (String eventId : eventIds) {

            // 현재 허용된 키 개수 (SCAN 기반)
            int current = scanAllowedCount(eventId);

            // 이전 값
            int prev = prevAllowedCount.computeIfAbsent(eventId, k -> new AtomicInteger(0)).get();

            if (current < prev) {
                int expiredCount = prev - current;
                log.info("[expired] eventId={} -> {}명 만료됨", eventId, expiredCount);

                // 만료 수만큼 allowNext 실행
                for (int i = 0; i < expiredCount; i++) {
                    String token = queueService.allowNext(eventId);
                    log.info("[allowNext] eventId={}, token={}", eventId, token);
                }
            }

            // 현재값을 저장
            prevAllowedCount.get(eventId).set(current);
            log.info("eventId=[{}], current=[{}], prev=[{}]", eventId, current, prev);
        }
    }
}

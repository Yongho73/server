package com.noah.api.app.queue.scheduler;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Scheduled(fixedDelay = 3000)
    public void checkExpiredAndAllowNext() {
        for (String eventId : eventIds) {
            
        	// 현재 허용된 키 개수 조회
            int current = redis.keys(allowedKeyPattern(eventId)).size();

            // 이전 값
            int prev = prevAllowedCount.computeIfAbsent(eventId, k -> new AtomicInteger(0)).get();

            if (current < prev) {
                int expiredCount = prev - current;
                System.out.println("[expired] eventId=" + eventId + " -> " + expiredCount + "명 만료됨");

                // 만료 수만큼 allowNext 실행
                for (int i = 0; i < expiredCount; i++) {
                    String token = queueService.allowNext(eventId);
                    System.out.println("[allowNext] eventId=" + eventId + ", token=" + token);
                }
            }

            // 현재값을 저장
            prevAllowedCount.get(eventId).set(current);           
            log.info("eventId=["+eventId+"], current=["+current+"], prev=["+prev+"]");
        }
    }
}

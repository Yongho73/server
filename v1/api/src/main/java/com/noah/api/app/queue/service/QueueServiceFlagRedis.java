package com.noah.api.app.queue.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueServiceFlagRedis {

    private final String MASTER_KEY = "queue:flag:master";
    private final String PER_EVENT_KEY_PREFIX = "queue:flag:";

    private final StringRedisTemplate redis;

    private Cache<String, Boolean> eventCache;   // 이벤트별 캐시
    private Cache<String, Boolean> masterCache;  // 마스터 캐시

    @PostConstruct
    public void initCache() {
        eventCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS) // TTL 5초
                .maximumSize(1000)
                .build();

        masterCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS) // TTL 5초
                .maximumSize(1) // 마스터 키는 단일 값
                .build();
    }

    /** 전체 마스터 ON/OFF */
    @SuppressWarnings("unused")
	public boolean isMasterEnabled() {
    	return masterCache.get(MASTER_KEY, k -> {
            String v = redis.opsForValue().get(MASTER_KEY);
            return v == null || "1".equals(v); // 기본 ON
        });
    }

    public void setMasterEnabled(boolean enabled) {
    	redis.opsForValue().set(MASTER_KEY, enabled ? "1" : "0");
        masterCache.put(MASTER_KEY, enabled); // 캐시 갱신
        eventCache.invalidateAll(); // 이벤트 캐시도 무효화 (마스터 OFF 시 전체 적용)
    }

    /** 이벤트별 ON/OFF 확인 (캐시 적용) */
    public boolean isEnabled(String eventId) {
        if (!isMasterEnabled()) return false;

        return eventCache.get(eventId, id -> {
            String v = redis.opsForValue().get(PER_EVENT_KEY_PREFIX + id);
            return v == null || "1".equals(v); // 기본 ON
        });
    }

    /** 이벤트별 ON/OFF 설정 */
    public void setEnabled(String eventId, boolean enabled) {
    	redis.opsForValue().set(PER_EVENT_KEY_PREFIX + eventId, enabled ? "1" : "0");
        eventCache.put(eventId, enabled); // 캐시 갱신
        redis.convertAndSend("queue:flag:changed", eventId + ":" + (enabled ? "on" : "off"));
    }

    /** 캐시 무효화 */
    public void evictEvent(String eventId) {
        eventCache.invalidate(eventId);
    }

    public void evictMaster() {
        masterCache.invalidate(MASTER_KEY);
    }
}

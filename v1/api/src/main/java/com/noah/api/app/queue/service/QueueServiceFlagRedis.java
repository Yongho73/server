package com.noah.api.app.queue.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueServiceFlagRedis {
	
    private final String MASTER_KEY = "queue:flag:master";
    private final String PER_EVENT_KEY_PREFIX = "queue:flag:";
    private final StringRedisTemplate redis;

    public boolean isMasterEnabled() {
        String v = redis.opsForValue().get(MASTER_KEY);
        return v == null || "1".equals(v); // 기본 ON
    }

    public void setMasterEnabled(boolean enabled) {
        redis.opsForValue().set(MASTER_KEY, enabled ? "1" : "0");
    }

    public boolean isEnabled(String eventId) {
        if (!isMasterEnabled()) return false;
        String v = redis.opsForValue().get(PER_EVENT_KEY_PREFIX + eventId);
        return v == null || "1".equals(v); // 기본 ON
    }

    public void setEnabled(String eventId, boolean enabled) {
        redis.opsForValue().set(PER_EVENT_KEY_PREFIX + eventId, enabled ? "1" : "0");
        // 선택) 다른 인스턴스에 변경 알림
        redis.convertAndSend("queue:flag:changed", eventId + ":" + (enabled ? "on" : "off"));
    }
}

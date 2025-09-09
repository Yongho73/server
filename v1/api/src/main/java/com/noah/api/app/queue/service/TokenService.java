package com.noah.api.app.queue.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.noah.api.app.queue.provider.TokenProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class TokenService {
	
	private final TokenProvider tokenProvider = new TokenProvider();
	
	private final StringRedisTemplate redis;
	
	public TokenService(StringRedisTemplate redisTemplate) {
        this.redis = redisTemplate;
    }
	
	public String createToken(String eventId, String queueId) {
		// ✅ 새 JWT 생성
	    String newToken = tokenProvider.createToken(eventId, queueId);	    
	    return newToken;
	}
	
	public String refreshToken(String eventId, String queueId) {
		
		String slotTag = "{" + eventId + "}";
	    String allowedKey = "allowed" + slotTag + ":" + queueId;

	    // Redis에 키가 있어야만 갱신
	    if (!Boolean.TRUE.equals(redis.hasKey(allowedKey))) {
	        return null;
	    }

	    // ✅ 새 JWT 생성
	    String newToken = tokenProvider.createToken(eventId, queueId);

	    // ✅ TTL 10분으로 갱신
	    redis.opsForValue().set(allowedKey, newToken, Duration.ofMinutes(10));
	    
	    return newToken;
	}
}

package com.noah.api.app.queue.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.noah.api.app.queue.provider.TokenProvider;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class TokenService {
	
	private final TokenProvider tokenProvider;
	private final StringRedisTemplate redis;
	
	@Value("${queue.system.waitSession.validitySecond}")
    private long validitySecond; 
	
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
	    redis.opsForValue().set(allowedKey, newToken, Duration.ofSeconds(validitySecond));
	    
	    return newToken;
	}
}

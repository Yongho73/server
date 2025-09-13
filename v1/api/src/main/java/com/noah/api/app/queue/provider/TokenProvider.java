package com.noah.api.app.queue.provider;

import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenProvider {
	
	private final StringRedisTemplate redis;
 
	// ✅ yml에서 세션 유효시간(분 단위) 읽기
    @Value("${queue.system.token.validityMillis}")
    private long tokenValidityMillis;

    // ✅ 서명용 시크릿 키 (Base64 인코딩된 문자열을 디코딩해서 사용 권장)
    @Value("${queue.system.token.secretKey}")    
    private String tokenSecretKey;
    
    /* Key Helpers */
    private String slotTag(String eventId)                { return "{" + eventId + "}"; }   
    private String allowedKey(String eventId, String qid) { return "allowed" + slotTag(eventId) + ":" + qid; }

    
    public String createToken(String eventId, String queueId) {
    	
    	//log.info("createToken: eventId=[{}], queueId=[{}], tokenSecretKey=[{}], tokenValidityMillis=[{}]", eventId, queueId, tokenSecretKey, tokenValidityMillis);
    	//log.info("createToken: allowedKey=[{}]", allowedKey(eventId, queueId));
    	
        long now = System.currentTimeMillis();        
        byte[] decodedKey = Base64.getDecoder().decode(tokenSecretKey);
        SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "HmacSHA256");
 
        try {
        	        	
        	// ✅ 토큰 생성
        	String token = Jwts.builder()
                    .setSubject(queueId)                                // queueId → subject
                    .claim("eventId", eventId)                          // eventId 추가
                    .setIssuedAt(new Date(now))                         // 발급 시간
                    .setExpiration(new Date(now + tokenValidityMillis)) // 만료 시간
                    .signWith(secretKey, SignatureAlgorithm.HS256)      // ✅ 최신 방식
                    .compact();
        	
        	// ✅ Redis 에 입장 허가로 갱신
            redis.opsForValue().set(allowedKey(eventId, queueId), token, Duration.ofMillis(tokenValidityMillis));
        	
            return token;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean isExpiringSoon(String token) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(tokenSecretKey);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(keyBytes)   // Key 또는 byte[] 사용 가능
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            long expMillis = claims.getExpiration().getTime();
            long nowMillis = System.currentTimeMillis();

            long remaining = expMillis - nowMillis;
            
            // ✅ 밀리초 → 분/초 변환
            long remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remaining);
            long remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60;

            //log.info("isExpiringSoon: remaining=[{} ms] ({}분 {}초 남음)", remaining, remainingMinutes, remainingSeconds);

            // ✅ 만료까지 1분 이하 남으면 true
            return remaining <= 60_000;

        } catch (Exception e) {
            // 파싱 실패 → 만료로 처리
            return true;
        }
    }
}

package com.noah.api.app.queue.provider;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class TokenProvider {
    
	private final SecretKey secretKey = Keys.hmacShaKeyFor("SmartixSecretKey1234SmartixSecretKey1234".getBytes()); // ⚠️ 최소 32바이트 이상 길이 필요 (HS256 권장 길이)

    public String createToken(String eventId, String queueId) {

        long now = System.currentTimeMillis();
        long validityMs = 600_000; // 10분

        return Jwts.builder()
            .setSubject(queueId)
            .claim("eventId", eventId)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + validityMs))
            .signWith(secretKey) // ✅ 최신 방식 (Key 객체 전달)
            .compact(); // ✅ Compact Serialization
    }
}
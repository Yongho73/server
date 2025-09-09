package com.noah.api.app.queue.provider;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class TokenProvider {   
	
	/*private final SecretKey secretKey = Keys.hmacShaKeyFor("MO6c+g04gJeWYzeZTTTT1eEN7jo06sR18BhsiyRNrYg=".getBytes()); // ⚠️ 최소 32바이트 이상 길이 필요 (HS256 권장 길이)

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
    }*/

	/* 랜덤 키 생성
	 Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
	 System.out.println(Base64.getEncoder().encodeToString(secretKey.getEncoded()));
	 */	
	//private final Key secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode("MO6c+g04gJeWYzeZTTTT1eEN7jo06sR18BhsiyRNrYg=")); // ⚠️ 최소 32바이트 이상 길이 필요 (HS256 권장 길이)
	byte[] keyBytes = "MO6c+g04gJeWYzeZTTTT1eEN7jo06sR18BhsiyRNrYg=".getBytes(StandardCharsets.UTF_8);

    public String createToken(String eventId, String queueId) {

        long now = System.currentTimeMillis();        
        long validityMs = 600_000; // 10분
        String token = "";
        
        try {

        	token = Jwts.builder()
                    .setSubject(queueId)                  // queueId를 subject로
                    .claim("eventId", eventId)            // eventId도 claim에 추가
                    .setIssuedAt(new Date(now))           // 발급 시간
                    .setExpiration(new Date(now + validityMs)) // 만료 시간
                    .signWith(SignatureAlgorithm.HS256, keyBytes)   // Key 객체
                    .compact();

        } catch (Exception e) {
        	e.printStackTrace();        	
        }
        
        return token;
    }
}
package com.noah.api.config.security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtTokenProvider {

	@Value("${settings.jwt.secret}")
	private String secretKey;

	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secretKey.getBytes());
	}

	public String generateToken(String username) {

		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + 3600000); // 1 hour

		return Jwts.builder()
				.setSubject(username)
				.setIssuedAt(now)
				.setExpiration(expiryDate)
				.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	public Claims getClaimsFromToken(String token) {
		JwtParser jwtParser = Jwts.parserBuilder().setSigningKey(getSigningKey()).build();
		return jwtParser.parseClaimsJws(token).getBody();
	}

	public String validateToken(String token) {
		try {
			getClaimsFromToken(token);			
			return "valid";	 
        } catch (ExpiredJwtException e) {            
        	log.info("만료된 JWT 토큰입니다.");
            return "expired";
        } catch (UnsupportedJwtException e) {        	
        	log.info("지원되지 않는 JWT 토큰입니다.");
        	return "unsuported";
        } catch (Exception e) {        	
        	log.info("JWT 토큰이 잘못되었습니다.");
        	return "invalid";
        }        
	}
}

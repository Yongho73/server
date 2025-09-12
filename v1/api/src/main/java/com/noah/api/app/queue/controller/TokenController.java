package com.noah.api.app.queue.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.app.queue.service.TokenService;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue")
public class TokenController {
	
	private final TokenService tokenService; 
	
	@PostMapping("/token/create/{eventId}/{queueId}")
	public ResponseEntity<String> createToken(
		@PathVariable("eventId") String eventId, 
	    @PathVariable("queueId") String queueId  
	) {

	    String newToken = tokenService.createToken(eventId, queueId);
	    
	    if (newToken == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 생성 실패");
	    } else {	    
	    	return ResponseEntity.ok(newToken);
	    }
	}
	
	@PostMapping("/token/refresh/{eventId}/{queueId}")
	public ResponseEntity<String> refreshToken(
		@PathVariable("eventId") String eventId, 
	    @PathVariable("queueId") String queueId  
	) {

	    String slotTag = "{" + eventId + "}";
	    String allowedKey = "allowed" + slotTag + ":" + queueId;

	    // Redis에 키가 있어야만 갱신
	    String newToken = tokenService.refreshToken(eventId, queueId);
	    
	    if (newToken == null) {
	        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰 없음 또는 만료됨");
	    } else {	    
	    	return ResponseEntity.ok(newToken);
	    }
	}
}

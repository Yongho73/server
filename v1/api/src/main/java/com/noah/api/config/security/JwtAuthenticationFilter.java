package com.noah.api.config.security;

import java.util.Collections;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.noah.api.cmmn.ApiResponse;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends BasicAuthenticationFilter {
    
	private final JwtTokenProvider jwtTokenProvider;
	private final ApiResponse apiResponse;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        super(authenticationManager -> null);
        this.jwtTokenProvider = jwtTokenProvider;
        this.apiResponse = new ApiResponse();
    }
	
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        try {
        	
	    	String token = request.getHeader("Authorization");	    
	        if (token == null) { // 토큰인증을 하지 않거나 토큰인증 정보가 없을경우 그냥 스프링프레임웍에 맏긴다.(방법이 없음)
	            chain.doFilter(request, response);
	            return;
	        }

	        String check = jwtTokenProvider.validateToken(token);

	        if("invalid".equals(check)) {	        	
	        	apiResponse.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401002", "JWT 토큰이 잘못되었습니다.");				
	        } else if ("expired".equals(check)) {           	        	
	        	apiResponse.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401003", "만료된 JWT 토큰입니다.");
	        } else if ("unsuported".equals(check)) {	        
	        	apiResponse.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401004", "지원되지 않는 JWT 토큰입니다.");
	        } else {        	
	        	Claims claims = jwtTokenProvider.getClaimsFromToken(token);
	            String username = claims.getSubject();
	            if (username != null) {	                
	            	UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
	                        username, 
	                        null, 
	                        Collections.emptyList());               
	                SecurityContextHolder.getContext().setAuthentication(authentication);
	                chain.doFilter(request, response);
	            } else {
	            	// 유효하지 않은 토큰인 경우 401 오류 발생 및 JSON 응답 반환
	            	apiResponse.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401001", "인증정보가 없습니다.");
	            }	         
	        }

	     } catch (Exception e) {
	    	 // 유효하지 않은 토큰인 경우 401 오류 발생 및 JSON 응답 반환
	    	 apiResponse.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "401005", "유효하지 않은 토큰입니다.");
	     }
    }
}
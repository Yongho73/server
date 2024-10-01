package com.noah.api.cmmn;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

public class ApiResponse {
	
	// JSON 형식의 오류 응답을 보내는 메서드
    public Map<String, Object> sendResponse(Object content) {

        // JSON 응답 생성        
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        body.put("code", "200");
        body.put("message", "");
        body.put("content", content);
        response.put("body", body);
        response.put("contentType", "Result");
        
        return response;
    }
	
	// JSON 형식의 오류 응답을 보내는 메서드
    public void sendErrorResponse(HttpServletResponse response, HttpStatus status, String code, String message) {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        // JSON 응답 생성
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> errorResponse = new HashMap<>();
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        errorResponse.put("body", body);
        errorResponse.put("contentType", "ErrorResult");

        // 응답 전송
        try {
        	PrintWriter writer = response.getWriter();
			writer.println(objectMapper.writeValueAsString(errorResponse));
			writer.flush();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    
    }
}

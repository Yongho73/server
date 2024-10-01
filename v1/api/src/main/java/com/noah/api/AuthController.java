package com.noah.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.noah.api.config.security.JwtTokenProvider;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final JwtTokenProvider jwtTokenProvider;
	
	public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

	@PostMapping("/token")
	public String generateToken(@RequestBody UserRequest userRequest) {
		// 토큰 생성
		return jwtTokenProvider.generateToken(userRequest.getUserName());
	}
	
	public static class UserRequest {
        private String userName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
}

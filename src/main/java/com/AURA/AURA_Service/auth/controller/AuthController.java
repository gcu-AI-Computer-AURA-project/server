package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.GoogleLoginRequest;
import com.AURA.AURA_Service.auth.dto.GoogleLoginResponse;
import com.AURA.AURA_Service.auth.service.AuthService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(summary = "Google OAuth 로그인 처리", description = "Authorization Code를 Google Token으로 교환하고 AURA JWT를 발급합니다.")
	@PostMapping("/google/login")
	public ResponseEntity<ApiResponse<GoogleLoginResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
		return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
	}
}

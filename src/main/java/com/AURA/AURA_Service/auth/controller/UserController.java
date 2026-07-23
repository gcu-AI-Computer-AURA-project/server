package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.UserMeResponse;
import com.AURA.AURA_Service.auth.service.UserService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 계정 정보를 조회합니다.")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserMeResponse>> getMe(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(userService.getMe(Long.valueOf(userId))));
	}
}

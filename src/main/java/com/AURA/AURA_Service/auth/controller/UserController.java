package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.UserMeResponse;
import com.AURA.AURA_Service.auth.dto.UserPrivacyDataResponse;
import com.AURA.AURA_Service.auth.dto.UserWithdrawalRequest;
import com.AURA.AURA_Service.auth.dto.UserWithdrawalResponse;
import com.AURA.AURA_Service.auth.service.UserService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

	@Operation(summary = "개인정보 및 데이터 관리 조회", description = "현재 로그인한 사용자의 동의 상태와 데이터 관리 요약 정보를 조회합니다.")
	@GetMapping("/me/privacy-data")
	public ResponseEntity<ApiResponse<UserPrivacyDataResponse>> getPrivacyData(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(userService.getPrivacyData(Long.valueOf(userId))));
	}

	@Operation(summary = "서비스 탈퇴", description = "현재 로그인한 사용자의 Google 토큰을 폐기하고 계정 개인정보를 익명화합니다.")
	@PostMapping("/me/withdrawal")
	public ResponseEntity<ApiResponse<UserWithdrawalResponse>> withdraw(@AuthenticationPrincipal String userId,
		@Valid @RequestBody UserWithdrawalRequest request) {
		return ResponseEntity.ok(ApiResponse.success(userService.withdraw(Long.valueOf(userId), request)));
	}
}

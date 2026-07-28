package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.GooglePermissionRecheckResponse;
import com.AURA.AURA_Service.auth.dto.GooglePermissionResponse;
import com.AURA.AURA_Service.auth.service.GooglePermissionService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/google/permissions")
public class GooglePermissionController {
	private final GooglePermissionService googlePermissionService;

	public GooglePermissionController(GooglePermissionService googlePermissionService) {
		this.googlePermissionService = googlePermissionService;
	}

	@Operation(summary = "Gmail 및 Drive 권한 연결 상태 조회", description = "현재 로그인한 사용자의 Google 서비스별 권한 연결 상태를 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<GooglePermissionResponse>> get(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(googlePermissionService.get(Long.valueOf(userId))));
	}

	@Operation(summary = "Google 권한 재검증", description = "저장된 Refresh Token으로 Access Token을 갱신하고 Gmail/Drive 권한 상태를 확인합니다.")
	@PostMapping("/recheck")
	public ResponseEntity<ApiResponse<GooglePermissionRecheckResponse>> recheck(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(googlePermissionService.recheck(Long.valueOf(userId))));
	}
}

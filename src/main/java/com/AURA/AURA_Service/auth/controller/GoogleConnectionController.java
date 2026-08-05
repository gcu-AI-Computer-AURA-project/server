package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.GoogleConnectionDisconnectResponse;
import com.AURA.AURA_Service.auth.service.GooglePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/google")
public class GoogleConnectionController {
	private final GooglePermissionService googlePermissionService;

	public GoogleConnectionController(GooglePermissionService googlePermissionService) {
		this.googlePermissionService = googlePermissionService;
	}

	@Operation(summary = "Google 계정 연결 해제", description = "저장된 Google Refresh Token을 폐기하고 Gmail/Drive 권한 상태를 DISCONNECTED로 변경합니다.")
	@DeleteMapping("/connection")
	public ResponseEntity<GoogleConnectionDisconnectResponse> disconnect(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(googlePermissionService.disconnectConnection(Long.valueOf(userId)));
	}
}

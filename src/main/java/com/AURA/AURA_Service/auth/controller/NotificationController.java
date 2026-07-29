package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.FcmTokenRequest;
import com.AURA.AURA_Service.auth.dto.FcmTokenResponse;
import com.AURA.AURA_Service.auth.service.NotificationService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@Operation(summary = "FCM 토큰 등록 또는 갱신", description = "사용자 기기의 FCM 토큰을 등록하거나 기존 토큰 정보를 최신 상태로 갱신합니다.")
	@PostMapping("/fcm-token")
	public ResponseEntity<ApiResponse<FcmTokenResponse>> saveFcmToken(@AuthenticationPrincipal String userId,
		@Valid @RequestBody FcmTokenRequest request) {
		return ResponseEntity.ok(ApiResponse.success(notificationService.saveFcmToken(Long.valueOf(userId), request)));
	}
}

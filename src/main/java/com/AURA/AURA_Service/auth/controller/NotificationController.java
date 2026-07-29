package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.FcmTokenRequest;
import com.AURA.AURA_Service.auth.dto.FcmTokenResponse;
import com.AURA.AURA_Service.auth.dto.NotificationPageResponse;
import com.AURA.AURA_Service.auth.dto.NotificationReadResponse;
import com.AURA.AURA_Service.auth.service.NotificationService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@Operation(summary = "알림 목록 조회", description = "사용자에게 발송된 알림 이력을 최신순으로 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<NotificationPageResponse>> getList(@AuthenticationPrincipal String userId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(name = "unread_only", defaultValue = "false") boolean unreadOnly) {
		return ResponseEntity.ok(ApiResponse.success(notificationService.getList(Long.valueOf(userId), page, size, unreadOnly)));
	}

	@Operation(summary = "알림 읽음 처리", description = "사용자가 확인한 알림을 읽음 상태로 변경합니다.")
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<NotificationReadResponse>> read(@AuthenticationPrincipal String userId,
		@PathVariable Long notificationId) {
		return ResponseEntity.ok(ApiResponse.success(notificationService.read(Long.valueOf(userId), notificationId)));
	}
}

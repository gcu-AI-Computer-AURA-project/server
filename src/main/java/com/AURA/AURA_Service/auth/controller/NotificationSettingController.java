package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.NotificationSettingRequest;
import com.AURA.AURA_Service.auth.dto.NotificationSettingResponse;
import com.AURA.AURA_Service.auth.service.NotificationSettingService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/settings")
public class NotificationSettingController {
	private final NotificationSettingService notificationSettingService;

	public NotificationSettingController(NotificationSettingService notificationSettingService) {
		this.notificationSettingService = notificationSettingService;
	}

	@Operation(summary = "알림 설정 조회", description = "사용자가 설정 화면에서 관리하는 알림 수신 여부를 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<NotificationSettingResponse>> get(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(notificationSettingService.get(Long.valueOf(userId))));
	}

	@Operation(summary = "알림 설정 수정", description = "스캔 완료 알림과 스캔 권장 알림 수신 여부를 저장합니다.")
	@PutMapping
	public ResponseEntity<ApiResponse<NotificationSettingResponse>> save(@AuthenticationPrincipal String userId,
		@Valid @RequestBody NotificationSettingRequest request) {
		return ResponseEntity.ok(ApiResponse.success(notificationSettingService.save(Long.valueOf(userId), request)));
	}
}

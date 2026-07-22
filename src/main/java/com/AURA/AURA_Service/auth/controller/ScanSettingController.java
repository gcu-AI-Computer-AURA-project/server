package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.ScanSettingRequest;
import com.AURA.AURA_Service.auth.dto.ScanSettingResponse;
import com.AURA.AURA_Service.auth.service.ScanSettingService;
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
@RequestMapping("/api/scan/settings")
public class ScanSettingController {
	private final ScanSettingService scanSettingService;

	public ScanSettingController(ScanSettingService scanSettingService) {
		this.scanSettingService = scanSettingService;
	}

	@Operation(summary = "기본 스캔 조건 조회", description = "사용자 프로필에 저장된 기본 스캔 조건을 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<ScanSettingResponse>> get(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(scanSettingService.get(Long.valueOf(userId))));
	}

	@Operation(summary = "기본 스캔 조건 저장", description = "최초 스캔 또는 설정 화면에서 입력한 기본 스캔 조건을 저장합니다.")
	@PutMapping
	public ResponseEntity<ApiResponse<ScanSettingResponse>> save(@AuthenticationPrincipal String userId,
		@Valid @RequestBody ScanSettingRequest request) {
		return ResponseEntity.ok(ApiResponse.success(scanSettingService.save(Long.valueOf(userId), request)));
	}
}

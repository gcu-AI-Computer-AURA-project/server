package com.AURA.AURA_Service.auth.controller;

import com.AURA.AURA_Service.auth.dto.UserConsentRequest;
import com.AURA.AURA_Service.auth.dto.UserConsentResponse;
import com.AURA.AURA_Service.auth.dto.UserConsentSaveResponse;
import com.AURA.AURA_Service.auth.service.UserConsentService;
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
@RequestMapping("/api/user/consents")
public class UserConsentController {
	private final UserConsentService userConsentService;

	public UserConsentController(UserConsentService userConsentService) {
		this.userConsentService = userConsentService;
	}

	@Operation(summary = "사용자 동의 상태 조회", description = "현재 로그인한 사용자의 개인정보 및 AI 분석 필수 동의 상태를 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<UserConsentResponse>> get(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(userConsentService.get(Long.valueOf(userId))));
	}

	@Operation(summary = "필수 동의 저장", description = "현재 로그인한 사용자의 필수 동의 상태를 저장하거나 갱신합니다.")
	@PutMapping
	public ResponseEntity<ApiResponse<UserConsentSaveResponse>> save(@AuthenticationPrincipal String userId,
		@Valid @RequestBody UserConsentRequest request) {
		return ResponseEntity.ok(ApiResponse.success(userConsentService.save(Long.valueOf(userId), request)));
	}
}

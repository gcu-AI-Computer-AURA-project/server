package com.AURA.AURA_Service.cleanup.controller;

import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateRequest;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateResponse;
import com.AURA.AURA_Service.cleanup.service.CleanupJobService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cleanup-jobs")
public class CleanupJobController {
	private final CleanupJobService cleanupJobService;

	public CleanupJobController(CleanupJobService cleanupJobService) {
		this.cleanupJobService = cleanupJobService;
	}

	@Operation(summary = "정리 작업 생성", description = "사용자가 최종 승인한 AI 정리 후보를 스냅샷으로 고정하고 정리 작업을 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<CleanupJobCreateResponse>> create(@AuthenticationPrincipal String userId,
		@Valid @RequestBody CleanupJobCreateRequest request) {
		CleanupJobCreateResponse response = cleanupJobService.create(Long.valueOf(userId), request);
		return ResponseEntity.created(URI.create("/api/cleanup-jobs/" + response.cleanupJobId()))
			.body(ApiResponse.success(response));
	}
}

package com.AURA.AURA_Service.cleanup.controller;

import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateRequest;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateResponse;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobDetailResponse;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobItemListResponse;
import com.AURA.AURA_Service.cleanup.service.CleanupJobService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@Operation(summary = "정리 작업 상세 및 진행 상태 조회", description = "정리 작업의 선택 항목 수, 처리 결과, 진행률과 오류 메시지를 조회합니다.")
	@GetMapping("/{cleanup_job_id}")
	public ResponseEntity<ApiResponse<CleanupJobDetailResponse>> getDetail(@AuthenticationPrincipal String userId,
		@PathVariable("cleanup_job_id") Long cleanupJobId) {
		CleanupJobDetailResponse response = cleanupJobService.getDetail(Long.valueOf(userId), cleanupJobId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "정리 작업 대상 및 개별 처리 결과 조회", description = "정리 작업에 고정된 대상 항목 스냅샷과 개별 처리 상태를 조회합니다.")
	@GetMapping("/{cleanup_job_id}/items")
	public ResponseEntity<ApiResponse<CleanupJobItemListResponse>> getItems(@AuthenticationPrincipal String userId,
		@PathVariable("cleanup_job_id") Long cleanupJobId) {
		CleanupJobItemListResponse response = cleanupJobService.getItems(Long.valueOf(userId), cleanupJobId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}

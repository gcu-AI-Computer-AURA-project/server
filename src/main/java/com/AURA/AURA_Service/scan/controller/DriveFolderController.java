package com.AURA.AURA_Service.scan.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.dto.DriveFolderResponse;
import com.AURA.AURA_Service.scan.service.DriveFolderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/scan/drive-folders")
public class DriveFolderController {
	private final DriveFolderService driveFolderService;

	public DriveFolderController(DriveFolderService driveFolderService) {
		this.driveFolderService = driveFolderService;
	}

	@Operation(summary = "Google Drive 폴더 목록 조회", description = "최초 스캔 또는 기본 분석 범위 설정 시 선택 가능한 Drive 폴더 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<DriveFolderResponse>> getDriveFolders(@AuthenticationPrincipal String userId,
		@RequestParam(name = "parent_id", required = false) String parentId,
		@RequestParam(name = "page_token", required = false) String pageToken,
		@RequestParam(defaultValue = "50") @Min(1) @Max(100) Integer size) {
		return ResponseEntity.ok(ApiResponse.success(driveFolderService.getDriveFolders(Long.valueOf(userId), parentId, pageToken, size)));
	}
}

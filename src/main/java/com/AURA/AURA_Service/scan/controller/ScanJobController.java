package com.AURA.AURA_Service.scan.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.dto.ScanCreateRequest;
import com.AURA.AURA_Service.scan.dto.ScanCreateResponse;
import com.AURA.AURA_Service.scan.dto.ScanRunningResponse;
import com.AURA.AURA_Service.scan.service.ScanJobService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scans")
public class ScanJobController {
	private final ScanJobService scanJobService;

	public ScanJobController(ScanJobService scanJobService) {
		this.scanJobService = scanJobService;
	}

	@Operation(summary = "스캔 및 AI 의미 분석 작업 시작", description = "Gmail/Drive 메타데이터 조회와 Gemini API 기반 AI 의미 분석 작업을 생성합니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<ScanCreateResponse>> create(@AuthenticationPrincipal String userId,
		@Valid @RequestBody ScanCreateRequest request) {
		ScanCreateResponse response = scanJobService.create(Long.valueOf(userId), request);
		return ResponseEntity.created(URI.create("/api/scans/" + response.scanJobId()))
			.body(ApiResponse.success(response));
	}

	@Operation(summary = "진행 중인 스캔 조회", description = "현재 사용자에게 진행 중인 스캔 작업이 있으면 진행 상태를 조회합니다.")
	@GetMapping("/running")
	public ResponseEntity<ApiResponse<ScanRunningResponse>> getRunning(@AuthenticationPrincipal String userId) {
		ScanRunningResponse response = scanJobService.getRunning(Long.valueOf(userId));
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(response));
	}
}

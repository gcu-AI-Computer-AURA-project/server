package com.AURA.AURA_Service.scan.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisSummaryResponse;
import com.AURA.AURA_Service.scan.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scans")
public class AnalysisController {
	private final AnalysisService analysisService;

	public AnalysisController(AnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@Operation(summary = "분석 결과 요약 조회", description = "분석 완료 후 분류 카드 화면에 필요한 후보 수와 예상 확보 용량을 조회합니다.")
	@GetMapping("/{scan_job_id}/analysis-summary")
	public ResponseEntity<ApiResponse<AnalysisSummaryResponse>> getAnalysisSummary(@AuthenticationPrincipal String userId,
		@PathVariable("scan_job_id") Long scanJobId) {
		return ResponseEntity.ok(ApiResponse.success(analysisService.getAnalysisSummary(Long.valueOf(userId), scanJobId)));
	}
}

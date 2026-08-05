package com.AURA.AURA_Service.scan.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisCandidateDetailResponse;
import com.AURA.AURA_Service.scan.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
	private final AnalysisService analysisService;

	public CandidateController(AnalysisService analysisService) {
		this.analysisService = analysisService;
	}

	@Operation(summary = "분석 후보 상세 조회", description = "분석 후보의 원본 항목 메타데이터와 AI 분석 상세 정보를 조회합니다.")
	@GetMapping("/{candidate_id}")
	public ResponseEntity<ApiResponse<AnalysisCandidateDetailResponse>> getCandidate(@AuthenticationPrincipal String userId,
		@PathVariable("candidate_id") Long candidateId) {
		return ResponseEntity.ok(ApiResponse.success(analysisService.getCandidate(Long.valueOf(userId), candidateId)));
	}
}

package com.AURA.AURA_Service.scan.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisCandidateDetailResponse;
import com.AURA.AURA_Service.scan.dto.CandidateSelectionRequest;
import com.AURA.AURA_Service.scan.dto.CandidateSelectionResponse;
import com.AURA.AURA_Service.scan.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

	@Operation(summary = "단일 후보 선택 상태 변경", description = "분석 후보의 선택 상태를 현재 선택 버전 검증 후 변경합니다.")
	@PatchMapping("/{candidate_id}/selection")
	public ResponseEntity<ApiResponse<CandidateSelectionResponse>> updateSelection(@AuthenticationPrincipal String userId,
		@PathVariable("candidate_id") Long candidateId,
		@Valid @RequestBody CandidateSelectionRequest request) {
		return ResponseEntity.ok(ApiResponse.success(analysisService.updateCandidateSelection(Long.valueOf(userId), candidateId, request)));
	}
}

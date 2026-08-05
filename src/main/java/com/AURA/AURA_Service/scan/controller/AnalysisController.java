package com.AURA.AURA_Service.scan.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.dto.AnalysisCandidatePageResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisSummaryResponse;
import com.AURA.AURA_Service.scan.dto.CandidateBulkSelectionRequest;
import com.AURA.AURA_Service.scan.dto.CandidateBulkSelectionResponse;
import com.AURA.AURA_Service.scan.dto.SelectedCandidateResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
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

	@Operation(summary = "분석 후보 목록 조회", description = "스캔 작업의 AI 분석 후보를 필터, 정렬, 페이징 조건으로 조회합니다.")
	@GetMapping("/{scan_job_id}/candidates")
	public ResponseEntity<ApiResponse<AnalysisCandidatePageResponse>> getCandidates(@AuthenticationPrincipal String userId,
		@PathVariable("scan_job_id") Long scanJobId,
		@RequestParam(required = false) CandidateCategory category,
		@RequestParam(name = "item_source", required = false) ItemSource itemSource,
		@RequestParam(name = "selection_status", required = false) SelectionStatus selectionStatus,
		@RequestParam(name = "include_protected", defaultValue = "false") boolean includeProtected,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "30") int size,
		@RequestParam(defaultValue = "priority_desc") String sort) {
		return ResponseEntity.ok(ApiResponse.success(analysisService.getCandidates(Long.valueOf(userId), scanJobId,
			category, itemSource, selectionStatus, includeProtected, page, size, sort)));
	}

	@Operation(summary = "후보 일괄 선택 상태 변경", description = "스캔 작업의 분석 후보를 조건에 따라 일괄 선택 또는 선택 해제합니다.")
	@PatchMapping("/{scan_job_id}/candidates/selection")
	public ResponseEntity<ApiResponse<CandidateBulkSelectionResponse>> updateCandidateSelections(
		@AuthenticationPrincipal String userId,
		@PathVariable("scan_job_id") Long scanJobId,
		@Valid @RequestBody CandidateBulkSelectionRequest request) {
		return ResponseEntity.ok(ApiResponse.success(analysisService.updateCandidateSelections(Long.valueOf(userId),
			scanJobId, request)));
	}

	@Operation(summary = "선택 항목 검토 조회", description = "스캔 작업에서 현재 선택된 후보 목록과 선택/보호 요약을 조회합니다.")
	@GetMapping("/{scan_job_id}/selected-candidates")
	public ResponseEntity<ApiResponse<SelectedCandidateResponse>> getSelectedCandidates(@AuthenticationPrincipal String userId,
		@PathVariable("scan_job_id") Long scanJobId) {
		return ResponseEntity.ok(ApiResponse.success(analysisService.getSelectedCandidates(Long.valueOf(userId), scanJobId)));
	}
}

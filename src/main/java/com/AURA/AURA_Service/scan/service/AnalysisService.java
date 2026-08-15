package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.dto.AnalysisCandidateDetailResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisCandidatePageResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisCategorySummaryResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisSummaryResponse;
import com.AURA.AURA_Service.scan.dto.CandidateBulkSelectionRequest;
import com.AURA.AURA_Service.scan.dto.CandidateBulkSelectionResponse;
import com.AURA.AURA_Service.scan.dto.CandidateSelectionRequest;
import com.AURA.AURA_Service.scan.dto.CandidateSelectionResponse;
import com.AURA.AURA_Service.scan.dto.SelectedCandidateResponse;
import com.AURA.AURA_Service.scan.repository.AnalysisCandidateRepository;
import com.AURA.AURA_Service.scan.repository.CandidateSelectionSummary;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {
	private static final int MAX_PAGE_SIZE = 100;
	private static final Set<String> ALLOWED_SORTS = Set.of("priority_desc", "size_desc", "date_asc");

	private final ScanJobRepository scanJobRepository;
	private final AnalysisCandidateRepository analysisCandidateRepository;

	public AnalysisService(ScanJobRepository scanJobRepository, AnalysisCandidateRepository analysisCandidateRepository) {
		this.scanJobRepository = scanJobRepository;
		this.analysisCandidateRepository = analysisCandidateRepository;
	}

	@Transactional(readOnly = true)
	public AnalysisSummaryResponse getAnalysisSummary(Long userId, Long scanJobId) {
		ScanJob scanJob = scanJobRepository.findByScanJobIdAndUser_UserIdAndDeletedAtIsNull(scanJobId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.SCAN_JOB_NOT_FOUND));
		List<AnalysisCategorySummaryResponse> categories = analysisCandidateRepository.summarizeByScanJobId(scanJobId, SelectionStatus.SELECTED).stream()
			.map(AnalysisCategorySummaryResponse::from)
			.toList();
		return AnalysisSummaryResponse.from(scanJob, categories);
	}

	@Transactional(readOnly = true)
	public AnalysisCandidatePageResponse getCandidates(Long userId, Long scanJobId, CandidateCategory category,
		ItemSource itemSource, SelectionStatus selectionStatus, boolean includeProtected, int page, int size, String sort) {
		findUserScanJob(userId, scanJobId);
		Pageable pageable = createPageable(page, size);
		String resolvedSort = resolveSort(sort);
		boolean resolvedIncludeProtected = includeProtected || category == CandidateCategory.PROTECTED;
		return AnalysisCandidatePageResponse.from(analysisCandidateRepository.findCandidates(scanJobId, category,
			itemSource, selectionStatus, resolvedIncludeProtected, resolvedSort, pageable));
	}

	@Transactional(readOnly = true)
	public AnalysisCandidateDetailResponse getCandidate(Long userId, Long candidateId) {
		return analysisCandidateRepository.findDetailByCandidateIdAndUserId(candidateId, userId)
			.map(AnalysisCandidateDetailResponse::from)
			.orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CANDIDATE_NOT_FOUND));
	}

	@Transactional
	public CandidateSelectionResponse updateCandidateSelection(Long userId, Long candidateId, CandidateSelectionRequest request) {
		AnalysisCandidate candidate = analysisCandidateRepository.findDetailByCandidateIdAndUserId(candidateId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.ANALYSIS_CANDIDATE_NOT_FOUND));
		validateSelectionVersion(candidate.getSelectionVersion(), request.selectionVersion());
		validateProtectedSelection(candidate.isProtected(), request.selectionStatus());
		candidate.updateSelection(request.selectionStatus());
		return CandidateSelectionResponse.from(candidate);
	}

	@Transactional
	public CandidateBulkSelectionResponse updateCandidateSelections(Long userId, Long scanJobId,
		CandidateBulkSelectionRequest request) {
		findUserScanJob(userId, scanJobId);
		validateCandidateIds(request.candidateIds());
		List<Long> candidateIds = resolveCandidateIds(request.candidateIds());
		List<AnalysisCandidate> candidates = analysisCandidateRepository.findCandidatesForSelection(scanJobId,
			request.category(), request.itemSource(), candidateIds, request.candidateIds() != null,
			request.excludeProtected());
		candidates.forEach(candidate -> {
			validateProtectedSelection(candidate.isProtected(), request.selectionStatus());
			candidate.updateSelection(request.selectionStatus());
		});
		CandidateSelectionSummary summary = analysisCandidateRepository.summarizeSelectedByScanJobId(scanJobId,
			SelectionStatus.SELECTED);
		return CandidateBulkSelectionResponse.from(candidates.size(), summary);
	}

	@Transactional(readOnly = true)
	public SelectedCandidateResponse getSelectedCandidates(Long userId, Long scanJobId) {
		findUserScanJob(userId, scanJobId);
		List<AnalysisCandidate> selectedCandidates = analysisCandidateRepository.findSelectedCandidates(scanJobId,
			SelectionStatus.SELECTED);
		List<AnalysisCandidate> protectedCandidates = analysisCandidateRepository.findProtectedCandidates(scanJobId);
		return SelectedCandidateResponse.from(scanJobId, selectedCandidates, protectedCandidates,
			extractProtectedConditions(protectedCandidates));
	}

	private ScanJob findUserScanJob(Long userId, Long scanJobId) {
		return scanJobRepository.findByScanJobIdAndUser_UserIdAndDeletedAtIsNull(scanJobId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.SCAN_JOB_NOT_FOUND));
	}

	private Pageable createPageable(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return PageRequest.of(page, size);
	}

	private String resolveSort(String sort) {
		String resolvedSort = sort == null || sort.isBlank() ? "priority_desc" : sort;
		if (!ALLOWED_SORTS.contains(resolvedSort)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return resolvedSort;
	}

	private void validateSelectionVersion(Integer currentVersion, Integer requestVersion) {
		if (!Objects.equals(currentVersion, requestVersion)) {
			throw new CustomException(ErrorCode.ANALYSIS_SELECTION_VERSION_CONFLICT);
		}
	}

	private void validateProtectedSelection(boolean isProtected, SelectionStatus selectionStatus) {
		if (isProtected && selectionStatus != SelectionStatus.NONE) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private void validateCandidateIds(List<Long> candidateIds) {
		if (candidateIds != null && candidateIds.isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private List<Long> resolveCandidateIds(List<Long> candidateIds) {
		if (candidateIds == null) {
			return List.of(-1L);
		}
		return candidateIds;
	}

	private List<String> extractProtectedConditions(List<AnalysisCandidate> protectedCandidates) {
		Set<String> conditions = new LinkedHashSet<>();
		for (AnalysisCandidate candidate : protectedCandidates) {
			Map<String, Object> matchedConditions = candidate.getMatchedConditions();
			if (matchedConditions == null) {
				continue;
			}
			addProtectedRuleCondition(conditions, matchedConditions.get("rule"));
			addKeywordConditions(conditions, matchedConditions.get("direct_exclude_keyword_matches"));
			addKeywordConditions(conditions, matchedConditions.get("semantic_exclude_keyword_matches"));
		}
		return List.copyOf(conditions);
	}

	private void addProtectedRuleCondition(Set<String> conditions, Object rule) {
		if (!(rule instanceof String ruleName)) {
			return;
		}
		switch (ruleName) {
			case "exclude_keyword" -> conditions.add("제외 키워드 포함");
			case "starred_or_important_mail" -> conditions.add("별표 또는 중요 표시");
			case "shared_drive_file" -> conditions.add("공유 Drive 파일");
			case "owner_mismatch" -> conditions.add("소유자 불일치");
			case "recent_activity" -> conditions.add("최근 사용 항목");
			default -> conditions.add(ruleName);
		}
	}

	@SuppressWarnings("unchecked")
	private void addKeywordConditions(Set<String> conditions, Object keywords) {
		if (!(keywords instanceof List<?> keywordList) || keywordList.isEmpty()) {
			return;
		}
		keywordList.stream()
			.filter(String.class::isInstance)
			.map(String.class::cast)
			.map(keyword -> "제외 키워드: " + keyword)
			.forEach(conditions::add);
	}
}

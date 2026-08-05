package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.dto.AnalysisCandidatePageResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisCategorySummaryResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisSummaryResponse;
import com.AURA.AURA_Service.scan.repository.AnalysisCandidateRepository;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.util.List;
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
		return AnalysisCandidatePageResponse.from(analysisCandidateRepository.findCandidates(scanJobId, category,
			itemSource, selectionStatus, includeProtected, resolvedSort, pageable));
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
}

package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.dto.AnalysisCategorySummaryResponse;
import com.AURA.AURA_Service.scan.dto.AnalysisSummaryResponse;
import com.AURA.AURA_Service.scan.repository.AnalysisCandidateRepository;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {
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
}

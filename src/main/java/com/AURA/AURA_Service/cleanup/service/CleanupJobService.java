package com.AURA.AURA_Service.cleanup.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateRequest;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateResponse;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobDetailResponse;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobItemListResponse;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobItemRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.repository.AnalysisCandidateRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CleanupJobService {
	private final UserRepository userRepository;
	private final AnalysisCandidateRepository analysisCandidateRepository;
	private final CleanupJobRepository cleanupJobRepository;
	private final CleanupJobItemRepository cleanupJobItemRepository;

	public CleanupJobService(UserRepository userRepository,
		AnalysisCandidateRepository analysisCandidateRepository,
		CleanupJobRepository cleanupJobRepository,
		CleanupJobItemRepository cleanupJobItemRepository) {
		this.userRepository = userRepository;
		this.analysisCandidateRepository = analysisCandidateRepository;
		this.cleanupJobRepository = cleanupJobRepository;
		this.cleanupJobItemRepository = cleanupJobItemRepository;
	}

	@Transactional
	public CleanupJobCreateResponse create(Long userId, CleanupJobCreateRequest request) {
		validateRequest(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		Map<Long, Integer> requestedVersions = toRequestedVersionMap(request.candidates());
		List<AnalysisCandidate> candidates = analysisCandidateRepository.findCleanupCandidates(
			request.scanJobId(), userId, requestedVersions.keySet().stream().toList());
		if (candidates.size() != requestedVersions.size()) {
			throw new CustomException(ErrorCode.ANALYSIS_CANDIDATE_NOT_FOUND);
		}
		List<AnalysisCandidate> cleanupTargets = candidates.stream()
			.peek(candidate -> validateSelectionVersion(candidate, requestedVersions.get(candidate.getCandidateId())))
			.filter(candidate -> !candidate.isProtected() && candidate.isSelected())
			.peek(this::validateExternalItemId)
			.toList();
		if (cleanupTargets.isEmpty()) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		ScanJob scanJob = cleanupTargets.get(0).getScanJob();
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, scanJob, request.actionType(),
			countBySource(cleanupTargets, ItemSource.GMAIL),
			countBySource(cleanupTargets, ItemSource.DRIVE),
			sumSelectedBytes(cleanupTargets),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = cleanupTargets.stream()
			.map(candidate -> CleanupJobItem.snapshot(cleanupJob, candidate))
			.toList();
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	@Transactional(readOnly = true)
	public CleanupJobDetailResponse getDetail(Long userId, Long cleanupJobId) {
		CleanupJob cleanupJob = cleanupJobRepository.findByCleanupJobIdAndUserUserId(cleanupJobId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
		return CleanupJobDetailResponse.from(cleanupJob);
	}

	@Transactional(readOnly = true)
	public CleanupJobItemListResponse getItems(Long userId, Long cleanupJobId) {
		cleanupJobRepository.findByCleanupJobIdAndUserUserId(cleanupJobId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
		List<CleanupJobItem> cleanupJobItems = cleanupJobItemRepository
			.findByCleanupJobCleanupJobIdOrderByCleanupItemIdAsc(cleanupJobId);
		return CleanupJobItemListResponse.from(cleanupJobItems);
	}

	private void validateRequest(CleanupJobCreateRequest request) {
		if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		if (request.actionType() != ActionType.MOVE_TO_TRASH) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private Map<Long, Integer> toRequestedVersionMap(List<CleanupJobCreateRequest.CandidateRequest> candidates) {
		Set<Long> candidateIds = new HashSet<>();
		Map<Long, Integer> requestedVersions = new HashMap<>();
		for (CleanupJobCreateRequest.CandidateRequest candidate : candidates) {
			if (!candidateIds.add(candidate.candidateId())) {
				throw new CustomException(ErrorCode.INVALID_INPUT);
			}
			requestedVersions.put(candidate.candidateId(), candidate.selectionVersion());
		}
		return requestedVersions;
	}

	private void validateSelectionVersion(AnalysisCandidate candidate, Integer requestedVersion) {
		if (!candidate.getSelectionVersion().equals(requestedVersion)) {
			throw new CustomException(ErrorCode.ANALYSIS_SELECTION_VERSION_CONFLICT);
		}
	}

	private void validateExternalItemId(AnalysisCandidate candidate) {
		ScannedItem item = candidate.getScannedItem();
		if (item.getExternalItemId() == null || item.getExternalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
	}

	private int countBySource(List<AnalysisCandidate> candidates, ItemSource itemSource) {
		return (int)candidates.stream()
			.filter(candidate -> candidate.getScannedItem().getItemSource() == itemSource)
			.count();
	}

	private long sumSelectedBytes(List<AnalysisCandidate> candidates) {
		return candidates.stream()
			.mapToLong(candidate -> candidate.getScannedItem().getEstimatedReclaimBytes())
			.sum();
	}
}

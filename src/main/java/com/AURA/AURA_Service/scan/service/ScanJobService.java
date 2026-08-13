package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.ScanSettingRepository;
import com.AURA.AURA_Service.auth.repository.UserConsentRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.service.ScanKeywordValidator;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.dto.ScanCancelResponse;
import com.AURA.AURA_Service.scan.dto.ScanCreateRequest;
import com.AURA.AURA_Service.scan.dto.ScanCreateResponse;
import com.AURA.AURA_Service.scan.dto.ScanHistoryItemResponse;
import com.AURA.AURA_Service.scan.dto.ScanHistoryResponse;
import com.AURA.AURA_Service.scan.dto.ScanJobDetailResponse;
import com.AURA.AURA_Service.scan.dto.ScanRunningJobResponse;
import com.AURA.AURA_Service.scan.dto.ScanRunningResponse;
import com.AURA.AURA_Service.scan.dto.ScanSettingsOverrideRequest;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ScanJobService {
	private static final int MAX_PAGE_SIZE = 100;
	private static final List<JobStatus> RUNNING_STATUSES = List.of(JobStatus.PENDING, JobStatus.SCANNING, JobStatus.ANALYZING);

	private final UserRepository userRepository;
	private final ScanSettingRepository scanSettingRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final ScanJobRepository scanJobRepository;
	private final ScanJobExecutionLauncher scanJobExecutionLauncher;
	private final UserConsentRepository userConsentRepository;
	private final ScanKeywordValidator scanKeywordValidator;
	private final JdbcTemplate jdbcTemplate;

	public ScanJobService(UserRepository userRepository, ScanSettingRepository scanSettingRepository,
		OAuthTokenRepository oauthTokenRepository, GooglePermissionRepository googlePermissionRepository,
		ScanJobRepository scanJobRepository, ScanJobExecutionLauncher scanJobExecutionLauncher,
		UserConsentRepository userConsentRepository, ScanKeywordValidator scanKeywordValidator, JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.scanSettingRepository = scanSettingRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.scanJobRepository = scanJobRepository;
		this.scanJobExecutionLauncher = scanJobExecutionLauncher;
		this.userConsentRepository = userConsentRepository;
		this.scanKeywordValidator = scanKeywordValidator;
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 스캔 작업 생성 메소드
	 * 요청 시점의 조건을 스냅샷으로 고정하고 중복 실행과 Google 권한 상태를 검증한다.
	 *
	 * @return : 생성된 스캔 작업 응답
	 * @since : 2026.08.01
	 * @version : 0.0.1
	 * @author : 최준혁
	 */
	@Transactional
	public ScanCreateResponse create(Long userId, ScanCreateRequest request) {
		User user = findUser(userId);
		validateRequiredConsent(user);
		validateNoRunningScan(user);
		AppliedScanCondition appliedCondition = resolveCondition(user, request);
		validateDriveFolder(appliedCondition.condition());
		scanKeywordValidator.validateNoConflict(appliedCondition.condition().getIncludeKeywords(),
			appliedCondition.condition().getExcludeKeywords());
		validateGoogleAccess(user, appliedCondition.condition().getScanSource());

		ScanJob scanJob = new ScanJob(user, appliedCondition.scanSetting(),
			appliedCondition.condition().getScanSource(), appliedCondition.condition().toSnapshot());
		ScanJob savedScanJob = scanJobRepository.save(scanJob);
		launchAfterCommit(savedScanJob.getScanJobId());
		return ScanCreateResponse.from(savedScanJob);
	}

	@Transactional(readOnly = true)
	public ScanRunningResponse getRunning(Long userId) {
		User user = findUser(userId);
		return scanJobRepository.findFirstByUserAndJobStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(user, RUNNING_STATUSES)
			.map(scanJob -> ScanRunningResponse.from(ScanRunningJobResponse.from(scanJob,
				estimateRemainingSeconds(scanJob))))
			.orElseGet(ScanRunningResponse::empty);
	}

	@Transactional(readOnly = true)
	public ScanJobDetailResponse getDetail(Long userId, Long scanJobId) {
		User user = findUser(userId);
		ScanJob scanJob = scanJobRepository.findByScanJobIdAndUserAndDeletedAtIsNull(scanJobId, user)
			.orElseThrow(() -> new CustomException(ErrorCode.SCAN_JOB_NOT_FOUND));
		return ScanJobDetailResponse.from(scanJob);
	}

	@Transactional
	public ScanCancelResponse cancel(Long userId, Long scanJobId) {
		User user = findUser(userId);
		ScanJob scanJob = scanJobRepository.findByScanJobIdAndUserAndDeletedAtIsNull(scanJobId, user)
			.orElseThrow(() -> new CustomException(ErrorCode.SCAN_JOB_NOT_FOUND));
		if (!RUNNING_STATUSES.contains(scanJob.getJobStatus()) && !isCancelableEmptyResult(scanJob)) {
			throw new CustomException(ErrorCode.SCAN_CANCEL_NOT_ALLOWED);
		}
		scanJob.markCanceled();
		return ScanCancelResponse.from(scanJob);
	}

	@Transactional(readOnly = true)
	public ScanHistoryResponse getHistory(Long userId, int page, int size, String status) {
		User user = findUser(userId);
		Pageable pageable = createHistoryPageable(page, size);
		JobStatus jobStatus = parseJobStatus(status);
		Page<ScanJob> scanJobs = jobStatus == null
			? scanJobRepository.findByUserAndDeletedAtIsNull(user, pageable)
			: scanJobRepository.findByUserAndJobStatusAndDeletedAtIsNull(user, jobStatus, pageable);
		Map<Long, CleanupHistorySummary> cleanupHistorySummaries =
			findCleanupHistorySummaries(user.getUserId(), scanJobs.getContent());
		List<ScanHistoryItemResponse> content = scanJobs.getContent().stream()
			.map(scanJob -> {
				CleanupHistorySummary cleanupHistorySummary = cleanupHistorySummaries
					.getOrDefault(scanJob.getScanJobId(), CleanupHistorySummary.empty());
				return ScanHistoryItemResponse.from(scanJob, cleanupHistorySummary.cleanupDone(),
					cleanupHistorySummary.reclaimedBytes(), cleanupHistorySummary.estimatedCarbonGrams());
			})
			.toList();
		return ScanHistoryResponse.from(scanJobs, content);
	}

	private void launchAfterCommit(Long scanJobId) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				scanJobExecutionLauncher.launch(scanJobId);
			}
		});
	}

	private boolean isCancelableEmptyResult(ScanJob scanJob) {
		return scanJob.getJobStatus() == JobStatus.COMPLETED && defaultZero(scanJob.getCandidateCount()) == 0;
	}

	private int defaultZero(Integer value) {
		return value == null ? 0 : value;
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateNoRunningScan(User user) {
		if (scanJobRepository.existsByUserAndJobStatusInAndDeletedAtIsNull(user, RUNNING_STATUSES)) {
			throw new CustomException(ErrorCode.SCAN_ALREADY_RUNNING);
		}
	}

	private void validateRequiredConsent(User user) {
		UserConsent consent = userConsentRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(ErrorCode.REQUIRED_CONSENT_REQUIRED));
		if (!consent.isRequiredConsentCompleted()) {
			throw new CustomException(ErrorCode.REQUIRED_CONSENT_REQUIRED);
		}
	}

	private AppliedScanCondition resolveCondition(User user, ScanCreateRequest request) {
		if (Boolean.TRUE.equals(request.useSavedSettings())) {
			ScanSetting scanSetting = scanSettingRepository.findByUser(user)
				.orElseThrow(() -> new CustomException(ErrorCode.SCAN_SETTING_NOT_FOUND));
			ScanCondition condition = ScanCondition.from(scanSetting).merge(request.settingsOverride());
			return new AppliedScanCondition(scanSetting, condition);
		}

		ScanSettingsOverrideRequest override = request.settingsOverride();
		if (override == null || override.scanSource() == null) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return new AppliedScanCondition(null, ScanCondition.fromOverride(override));
	}

	private void validateDriveFolder(ScanCondition condition) {
		if (condition.getScanSource() == ScanSource.DRIVE_FOLDER && isBlank(condition.getDriveFolderId())) {
			throw new CustomException(ErrorCode.DRIVE_FOLDER_REQUIRED);
		}
	}

	private void validateGoogleAccess(User user, ScanSource scanSource) {
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> permissionException(scanSource));
		if (oauthToken.getTokenStatus() != TokenStatus.VALID || isBlank(oauthToken.getEncryptedRefreshToken())) {
			throw permissionException(scanSource);
		}
		if (requiresGmail(scanSource)) validatePermission(user, ServiceType.GMAIL, ErrorCode.GMAIL_PERMISSION_REQUIRED);
		if (requiresDrive(scanSource)) validatePermission(user, ServiceType.DRIVE, ErrorCode.DRIVE_PERMISSION_REQUIRED);
	}

	private void validatePermission(User user, ServiceType serviceType, ErrorCode errorCode) {
		GooglePermission permission = googlePermissionRepository.findByUserAndServiceType(user, serviceType)
			.orElseThrow(() -> new CustomException(errorCode));
		if (permission.getPermissionStatus() != PermissionStatus.CONNECTED) {
			throw new CustomException(errorCode);
		}
	}

	private CustomException permissionException(ScanSource scanSource) {
		if (requiresGmail(scanSource)) return new CustomException(ErrorCode.GMAIL_PERMISSION_REQUIRED);
		return new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED);
	}

	private boolean requiresGmail(ScanSource scanSource) {
		return scanSource == ScanSource.MAIL || scanSource == ScanSource.MAIL_AND_DRIVE;
	}

	private boolean requiresDrive(ScanSource scanSource) {
		return scanSource == ScanSource.DRIVE_ALL || scanSource == ScanSource.DRIVE_FOLDER || scanSource == ScanSource.MAIL_AND_DRIVE;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private Pageable createHistoryPageable(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")));
	}

	private JobStatus parseJobStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return JobStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private Map<Long, CleanupHistorySummary> findCleanupHistorySummaries(Long userId, List<ScanJob> scanJobs) {
		if (scanJobs.isEmpty()) {
			return Map.of();
		}
		List<Long> scanJobIds = scanJobs.stream()
			.map(ScanJob::getScanJobId)
			.toList();
		String placeholders = scanJobIds.stream()
			.map(scanJobId -> "?")
			.collect(Collectors.joining(", "));
		List<Object> parameters = new ArrayList<>();
		parameters.add(userId);
		parameters.addAll(scanJobIds);
		try {
			return jdbcTemplate.query("""
				select
					ch.scan_job_id,
					coalesce(sum(ch.reclaimed_bytes), 0) as reclaimed_bytes,
					coalesce(sum(csh.estimated_carbon_grams), 0.0000) as estimated_carbon_grams
				from cleanup_histories ch
				left join carbon_saving_histories csh on csh.history_id = ch.history_id
				where ch.user_id = ?
					and ch.scan_job_id in (%s)
				group by ch.scan_job_id
				""".formatted(placeholders), resultSet -> {
				Map<Long, CleanupHistorySummary> summaries = new LinkedHashMap<>();
				while (resultSet.next()) {
					summaries.put(resultSet.getLong("scan_job_id"), new CleanupHistorySummary(
						true,
						resultSet.getLong("reclaimed_bytes"),
						defaultCarbon(resultSet.getBigDecimal("estimated_carbon_grams"))
					));
				}
				return summaries;
			}, parameters.toArray());
		} catch (DataAccessException exception) {
			return Map.of();
		}
	}

	private BigDecimal defaultCarbon(BigDecimal value) {
		return value == null ? BigDecimal.ZERO.setScale(4) : value;
	}

	private Long estimateRemainingSeconds(ScanJob scanJob) {
		LocalDateTime startedAt = scanJob.getStartedAt();
		BigDecimal progressPercent = scanJob.getProgressPercent();
		if (startedAt == null || progressPercent == null || progressPercent.compareTo(BigDecimal.ZERO) <= 0) {
			return null;
		}

		long elapsedSeconds = Duration.between(startedAt, LocalDateTime.now()).getSeconds();
		if (elapsedSeconds <= 0 || progressPercent.compareTo(new BigDecimal("100.00")) >= 0) {
			return 0L;
		}

		return BigDecimal.valueOf(elapsedSeconds)
			.multiply(new BigDecimal("100.00").subtract(progressPercent))
			.divide(progressPercent, 0, RoundingMode.HALF_UP)
			.longValue();
	}

	private record AppliedScanCondition(ScanSetting scanSetting, ScanCondition condition) {
	}

	private record CleanupHistorySummary(
		boolean cleanupDone,
		Long reclaimedBytes,
		BigDecimal estimatedCarbonGrams
	) {
		private static CleanupHistorySummary empty() {
			return new CleanupHistorySummary(false, 0L, BigDecimal.ZERO.setScale(4));
		}
	}
}

package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.ScanSettingRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.dto.ScanCreateRequest;
import com.AURA.AURA_Service.scan.dto.ScanCreateResponse;
import com.AURA.AURA_Service.scan.dto.ScanSettingsOverrideRequest;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ScanJobService {
	private static final List<JobStatus> RUNNING_STATUSES = List.of(JobStatus.PENDING, JobStatus.SCANNING, JobStatus.ANALYZING);

	private final UserRepository userRepository;
	private final ScanSettingRepository scanSettingRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final ScanJobRepository scanJobRepository;
	private final ScanJobExecutionLauncher scanJobExecutionLauncher;

	public ScanJobService(UserRepository userRepository, ScanSettingRepository scanSettingRepository,
		OAuthTokenRepository oauthTokenRepository, GooglePermissionRepository googlePermissionRepository,
		ScanJobRepository scanJobRepository, ScanJobExecutionLauncher scanJobExecutionLauncher) {
		this.userRepository = userRepository;
		this.scanSettingRepository = scanSettingRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.scanJobRepository = scanJobRepository;
		this.scanJobExecutionLauncher = scanJobExecutionLauncher;
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
		validateNoRunningScan(user);
		AppliedScanCondition appliedCondition = resolveCondition(user, request);
		validateDriveFolder(appliedCondition.condition());
		validateGoogleAccess(user, appliedCondition.condition().getScanSource());

		ScanJob scanJob = new ScanJob(user, appliedCondition.scanSetting(),
			appliedCondition.condition().getScanSource(), appliedCondition.condition().toSnapshot());
		ScanJob savedScanJob = scanJobRepository.save(scanJob);
		launchAfterCommit(savedScanJob.getScanJobId());
		return ScanCreateResponse.from(savedScanJob);
	}

	private void launchAfterCommit(Long scanJobId) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				scanJobExecutionLauncher.launch(scanJobId);
			}
		});
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateNoRunningScan(User user) {
		if (scanJobRepository.existsByUserAndJobStatusInAndDeletedAtIsNull(user, RUNNING_STATUSES)) {
			throw new CustomException(ErrorCode.SCAN_ALREADY_RUNNING);
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

	private record AppliedScanCondition(ScanSetting scanSetting, ScanCondition condition) {
	}
}

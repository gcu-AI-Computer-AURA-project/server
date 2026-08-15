package com.AURA.AURA_Service.home.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleToken;
import com.AURA.AURA_Service.auth.service.TokenEncryptionService;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.LatestCleanupResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.LatestScanResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.PermissionSummaryResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.StorageSummaryResponse;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {
	private static final Logger LOGGER = LoggerFactory.getLogger(HomeService.class);
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final List<JobStatus> RUNNING_STATUSES = List.of(JobStatus.PENDING, JobStatus.SCANNING, JobStatus.ANALYZING);
	private static final BigDecimal ZERO_CARBON_GRAMS = new BigDecimal("0.0000");

	private final UserRepository userRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final ScanJobRepository scanJobRepository;
	private final JdbcTemplate jdbcTemplate;

	public HomeService(UserRepository userRepository, GooglePermissionRepository googlePermissionRepository,
		OAuthTokenRepository oauthTokenRepository, TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient, ScanJobRepository scanJobRepository, JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.scanJobRepository = scanJobRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public HomeSummaryResponse getSummary(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		ScanJob latestScan = scanJobRepository.findFirstByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user).orElse(null);
		boolean hasRunningScan = scanJobRepository.existsByUserAndJobStatusInAndDeletedAtIsNull(user, RUNNING_STATUSES);
		PermissionSummaryResponse permissionSummary = createPermissionSummary(user);
		return new HomeSummaryResponse(
			createStorageSummary(user, latestScan, permissionSummary),
			LatestScanResponse.from(latestScan),
			findLatestCleanup(user.getUserId()).orElse(null),
			permissionSummary,
			hasRunningScan
		);
	}

	private StorageSummaryResponse createStorageSummary(User user, ScanJob latestScan, PermissionSummaryResponse permissionSummary) {
		CleanupStorageSummary cleanupSummary = findCleanupStorageSummary(user.getUserId());
		DriveStorageSnapshot driveStorageSnapshot = findLiveDriveStorageSnapshot(user, permissionSummary)
			.orElse(new DriveStorageSnapshot(cleanupSummary.latestRemainingDriveBytes(), cleanupSummary.totalDriveBytes()));
		return new StorageSummaryResponse(
			latestScan == null ? 0L : latestScan.getEstimatedReclaimBytes(),
			driveStorageSnapshot.remainingDriveBytes(),
			driveStorageSnapshot.totalDriveBytes(),
			cleanupSummary.totalReclaimedBytes(),
			cleanupSummary.totalEstimatedCarbonGrams()
		);
	}

	private PermissionSummaryResponse createPermissionSummary(User user) {
		List<GooglePermission> permissions = googlePermissionRepository.findByUser(user);
		return new PermissionSummaryResponse(
			findPermissionStatus(permissions, ServiceType.GMAIL),
			findPermissionStatus(permissions, ServiceType.DRIVE)
		);
	}

	private PermissionStatus findPermissionStatus(List<GooglePermission> permissions, ServiceType serviceType) {
		return permissions.stream()
			.filter(permission -> permission.getServiceType() == serviceType)
			.map(GooglePermission::getPermissionStatus)
			.findFirst()
			.orElse(PermissionStatus.DISCONNECTED);
	}

	private CleanupStorageSummary findCleanupStorageSummary(Long userId) {
		try {
			Long totalReclaimedBytes = jdbcTemplate.queryForObject("""
				select coalesce(sum(reclaimed_bytes), 0)
				from cleanup_histories
				where user_id = ?
				""", Long.class, userId);
			DriveStorageSnapshot latestDriveStorageSnapshot = jdbcTemplate.query("""
				select remaining_drive_bytes, total_drive_bytes
				from cleanup_histories
				where user_id = ?
					and (remaining_drive_bytes is not null or total_drive_bytes is not null)
				order by completed_at desc
				limit 1
				""", (rs, rowNum) -> new DriveStorageSnapshot(
					rs.getObject("remaining_drive_bytes", Long.class),
					rs.getObject("total_drive_bytes", Long.class)
				), userId).stream()
				.findFirst()
				.orElse(DriveStorageSnapshot.empty());
			BigDecimal totalEstimatedCarbonGrams = jdbcTemplate.queryForObject("""
				select coalesce(sum(estimated_carbon_grams), 0.0000)
				from carbon_saving_histories
				where user_id = ?
				""", BigDecimal.class, userId);
			return new CleanupStorageSummary(
				defaultZero(totalReclaimedBytes),
				latestDriveStorageSnapshot.remainingDriveBytes(),
				latestDriveStorageSnapshot.totalDriveBytes(),
				defaultCarbon(totalEstimatedCarbonGrams)
			);
		} catch (DataAccessException exception) {
			return CleanupStorageSummary.empty();
		}
	}

	private Optional<LatestCleanupResponse> findLatestCleanup(Long userId) {
		try {
			return jdbcTemplate.query("""
				select
					ch.cleanup_job_id,
					ch.cleaned_item_count,
					ch.reclaimed_bytes,
					coalesce(csh.estimated_carbon_grams, 0.0000) as estimated_carbon_grams,
					ch.completed_at
				from cleanup_histories ch
				left join carbon_saving_histories csh on csh.history_id = ch.history_id
				where ch.user_id = ?
				order by ch.completed_at desc
				limit 1
				""", (rs, rowNum) -> new LatestCleanupResponse(
					rs.getLong("cleanup_job_id"),
					rs.getInt("cleaned_item_count"),
					rs.getLong("reclaimed_bytes"),
					defaultCarbon(rs.getBigDecimal("estimated_carbon_grams")),
					toLocalDateTime(rs.getTimestamp("completed_at"))
				), userId).stream().findFirst();
		} catch (DataAccessException exception) {
			return Optional.empty();
		}
	}

	private Optional<DriveStorageSnapshot> findLiveDriveStorageSnapshot(User user, PermissionSummaryResponse permissionSummary) {
		if (permissionSummary.driveStatus() != PermissionStatus.CONNECTED) {
			return Optional.empty();
		}
		try {
			OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
				.orElseThrow(() -> new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED));
			validateDriveToken(oauthToken);
			GoogleToken googleToken = googleOAuthClient.refreshAccessToken(tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken()));
			oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());
			return Optional.of(fetchDriveStorageSnapshot(googleToken.accessToken()));
		} catch (RuntimeException exception) {
			LOGGER.warn("홈 요약 Drive 용량 조회에 실패했습니다. userId={}", user.getUserId(), exception);
			return Optional.empty();
		}
	}

	private DriveStorageSnapshot fetchDriveStorageSnapshot(String accessToken) {
		try {
			About about = createDrive(accessToken).about().get()
				.setFields("storageQuota(limit,usage)")
				.execute();
			if (about.getStorageQuota() == null) {
				return DriveStorageSnapshot.empty();
			}
			Long totalDriveBytes = about.getStorageQuota().getLimit();
			Long usageBytes = about.getStorageQuota().getUsage();
			Long remainingDriveBytes = totalDriveBytes == null || usageBytes == null
				? null
				: Math.max(totalDriveBytes - usageBytes, 0L);
			return new DriveStorageSnapshot(remainingDriveBytes, totalDriveBytes);
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED);
		}
	}

	private void validateDriveToken(OAuthToken oauthToken) {
		if (oauthToken.getTokenStatus() != TokenStatus.VALID || oauthToken.getEncryptedRefreshToken() == null
			|| oauthToken.getEncryptedRefreshToken().isBlank()) {
			throw new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED);
		}
	}

	private Drive createDrive(String accessToken) {
		try {
			NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			return new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), createRequestInitializer(accessToken))
				.setApplicationName(APPLICATION_NAME)
				.build();
		} catch (GeneralSecurityException | IOException exception) {
			throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		}
	}

	private HttpRequestInitializer createRequestInitializer(String accessToken) {
		GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, Date.from(Instant.now().plusSeconds(3600))));
		return new HttpCredentialsAdapter(credentials);
	}

	private Long defaultZero(Long value) {
		return value == null ? 0L : value;
	}

	private BigDecimal defaultCarbon(BigDecimal value) {
		return value == null ? ZERO_CARBON_GRAMS : value;
	}

	private LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private record CleanupStorageSummary(
		Long totalReclaimedBytes,
		Long latestRemainingDriveBytes,
		Long totalDriveBytes,
		BigDecimal totalEstimatedCarbonGrams
	) {
		private static CleanupStorageSummary empty() {
			return new CleanupStorageSummary(0L, null, null, ZERO_CARBON_GRAMS);
		}
	}

	private record DriveStorageSnapshot(Long remainingDriveBytes, Long totalDriveBytes) {
		private static DriveStorageSnapshot empty() {
			return new DriveStorageSnapshot(null, null);
		}
	}
}

package com.AURA.AURA_Service.cleanup.service;

import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleToken;
import com.AURA.AURA_Service.auth.service.TokenEncryptionService;
import com.AURA.AURA_Service.cleanup.domain.CarbonSavingHistory;
import com.AURA.AURA_Service.cleanup.domain.CleanupHistory;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem.ProcessStatus;
import com.AURA.AURA_Service.cleanup.repository.CarbonSavingHistoryRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupHistoryRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobItemRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.File;
import com.google.api.services.gmail.Gmail;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CleanupJobExecutionService {
	private static final Logger log = LoggerFactory.getLogger(CleanupJobExecutionService.class);
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final String GOOGLE_USER_ID = "me";
	private static final String CARBON_FORMULA_VERSION = "v1";
	private static final BigDecimal GIB_BYTES = new BigDecimal("1073741824");
	private static final BigDecimal CARBON_GRAMS_PER_GIB = new BigDecimal("1.5600");
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final long DRIVE_QUOTA_SCALE_CORRECTION_THRESHOLD_BYTES = 100L * 1024 * 1024 * 1024 * 1024;
	private static final long DRIVE_QUOTA_SCALE_CORRECTION_FACTOR = 100_000L;

	private final CleanupJobRepository cleanupJobRepository;
	private final CleanupJobItemRepository cleanupJobItemRepository;
	private final CleanupHistoryRepository cleanupHistoryRepository;
	private final CarbonSavingHistoryRepository carbonSavingHistoryRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final JdbcTemplate jdbcTemplate;
	private final TransactionTemplate transactionTemplate;
	private final CleanupCompletionNotificationService cleanupCompletionNotificationService;

	public CleanupJobExecutionService(CleanupJobRepository cleanupJobRepository,
		CleanupJobItemRepository cleanupJobItemRepository,
		CleanupHistoryRepository cleanupHistoryRepository,
		CarbonSavingHistoryRepository carbonSavingHistoryRepository,
		OAuthTokenRepository oauthTokenRepository,
		TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient,
		JdbcTemplate jdbcTemplate,
		TransactionTemplate transactionTemplate,
		CleanupCompletionNotificationService cleanupCompletionNotificationService) {
		this.cleanupJobRepository = cleanupJobRepository;
		this.cleanupJobItemRepository = cleanupJobItemRepository;
		this.cleanupHistoryRepository = cleanupHistoryRepository;
		this.carbonSavingHistoryRepository = carbonSavingHistoryRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.jdbcTemplate = jdbcTemplate;
		this.transactionTemplate = transactionTemplate;
		this.cleanupCompletionNotificationService = cleanupCompletionNotificationService;
	}

	public void execute(Long cleanupJobId) {
		CleanupExecutionContext cleanupExecutionContext = startJob(cleanupJobId);
		if (cleanupExecutionContext.pendingItems().isEmpty()) {
			if (isCanceled(cleanupJobId)) {
				markPendingItemsSkipped(cleanupJobId, "Cleanup job canceled before processing.");
			}
			return;
		}
		GoogleToken googleToken;
		DriveStorageQuotaSnapshot driveStorageQuotaSnapshot = DriveStorageQuotaSnapshot.empty();
		try {
			googleToken = refreshGoogleAccessToken(cleanupExecutionContext.userId());
		} catch (RuntimeException exception) {
			if (isCanceled(cleanupJobId)) return;
			failPendingItems(cleanupJobId, exception.getMessage());
			if (completeJob(cleanupJobId, driveStorageQuotaSnapshot)) {
				cleanupCompletionNotificationService.notifyIfCompleted(cleanupJobId);
			}
			return;
		}
		try {
			Gmail gmail = createGmail(googleToken.accessToken());
			Drive drive = createDrive(googleToken.accessToken());
			for (CleanupItemWork item : cleanupExecutionContext.pendingItems()) {
				if (handleCancellation(cleanupJobId, cleanupExecutionContext.actionType(), gmail, drive)) return;
				CleanupItemProcessResult result = processItem(cleanupExecutionContext.actionType(), item, gmail, drive);
				recordItemResult(cleanupJobId, item.cleanupItemId(), result);
				if (handleCancellation(cleanupJobId, cleanupExecutionContext.actionType(), gmail, drive)) return;
			}
			if (handleCancellation(cleanupJobId, cleanupExecutionContext.actionType(), gmail, drive)) return;
			driveStorageQuotaSnapshot = fetchDriveStorageQuota(drive, cleanupJobId);
		} catch (RuntimeException exception) {
			if (isCanceled(cleanupJobId)) return;
			failPendingItems(cleanupJobId, exception.getMessage());
		}
		if (completeJob(cleanupJobId, driveStorageQuotaSnapshot)) {
			cleanupCompletionNotificationService.notifyIfCompleted(cleanupJobId);
		}
	}

	private CleanupItemProcessResult processItem(ActionType actionType, CleanupItemWork item, Gmail gmail, Drive drive) {
		LocalDateTime processedAt = LocalDateTime.now();
		try {
			validateExternalItemId(item);
			if (item.itemSource() == ItemSource.GMAIL) {
				processGmailItem(actionType, item.externalItemId(), gmail);
			} else {
				processDriveItem(actionType, item.externalItemId(), drive);
			}
			return CleanupItemProcessResult.success(processedAt);
		} catch (RuntimeException | IOException exception) {
			return CleanupItemProcessResult.failed(exception.getMessage(), processedAt);
		}
	}

	private void processGmailItem(ActionType actionType, String externalItemId, Gmail gmail) throws IOException {
		if (actionType == ActionType.MOVE_TO_TRASH) {
			gmail.users().messages().trash(GOOGLE_USER_ID, externalItemId).execute();
			return;
		}
		if (actionType == ActionType.RESTORE_FROM_TRASH) {
			gmail.users().messages().untrash(GOOGLE_USER_ID, externalItemId).execute();
			return;
		}
		gmail.users().messages().delete(GOOGLE_USER_ID, externalItemId).execute();
	}

	private void processDriveItem(ActionType actionType, String externalItemId, Drive drive) throws IOException {
		if (actionType == ActionType.MOVE_TO_TRASH) {
			drive.files().update(externalItemId, new File().setTrashed(true))
				.execute();
			return;
		}
		if (actionType == ActionType.RESTORE_FROM_TRASH) {
			drive.files().update(externalItemId, new File().setTrashed(false))
				.execute();
			return;
		}
		drive.files().delete(externalItemId)
			.execute();
	}

	private CleanupExecutionContext startJob(Long cleanupJobId) {
		CleanupExecutionContext cleanupExecutionContext = transactionTemplate.execute(status -> {
			CleanupJob cleanupJob = findCleanupJob(cleanupJobId);
			if (cleanupJob.getJobStatus() == CleanupJob.JobStatus.CANCELED) {
				return CleanupExecutionContext.empty();
			}
			List<CleanupJobItem> pendingItems = cleanupJobItemRepository
				.findByCleanupJobCleanupJobIdAndProcessStatusOrderByCleanupItemIdAsc(cleanupJobId, ProcessStatus.PENDING);
			if (!pendingItems.isEmpty()) {
				cleanupJob.start();
			}
			return new CleanupExecutionContext(
				cleanupJob.getUser().getUserId(),
				cleanupJob.getActionType(),
				pendingItems.stream()
					.map(item -> new CleanupItemWork(item.getCleanupItemId(), item.getItemSource(), item.getExternalItemId()))
					.toList()
			);
		});
		if (cleanupExecutionContext == null) throw new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND);
		return cleanupExecutionContext;
	}

	private void recordItemResult(Long cleanupJobId, Long cleanupItemId, CleanupItemProcessResult result) {
		transactionTemplate.executeWithoutResult(status -> {
			CleanupJob cleanupJob = findCleanupJob(cleanupJobId);
			if (cleanupJob.getJobStatus() == CleanupJob.JobStatus.CANCELED) {
				return;
			}
			CleanupJobItem item = cleanupJobItemRepository.findById(cleanupItemId)
				.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
			if (result.success()) {
				item.markSuccess(result.processedAt());
			} else {
				item.markFailed(result.failureReason(), result.processedAt());
			}
			updateProcessingProgress(cleanupJobId);
		});
	}

	private void failPendingItems(Long cleanupJobId, String failureReason) {
		transactionTemplate.executeWithoutResult(status -> {
			CleanupJob cleanupJob = findCleanupJob(cleanupJobId);
			if (cleanupJob.getJobStatus() == CleanupJob.JobStatus.CANCELED) {
				return;
			}
			LocalDateTime processedAt = LocalDateTime.now();
			List<CleanupJobItem> pendingItems = cleanupJobItemRepository
				.findByCleanupJobCleanupJobIdAndProcessStatusOrderByCleanupItemIdAsc(cleanupJobId, ProcessStatus.PENDING);
			pendingItems.forEach(item -> item.markFailed(failureReason, processedAt));
			updateProcessingProgress(cleanupJobId);
		});
	}

	private void updateProcessingProgress(Long cleanupJobId) {
		CleanupJob cleanupJob = findCleanupJob(cleanupJobId);
		List<CleanupJobItem> items = cleanupJobItemRepository
			.findByCleanupJobCleanupJobIdOrderByCleanupItemIdAsc(cleanupJobId);
		int successItemCount = countByStatus(items, ProcessStatus.SUCCESS);
		int failedItemCount = countByStatus(items, ProcessStatus.FAILED);
		int processedItemCount = successItemCount + failedItemCount + countByStatus(items, ProcessStatus.SKIPPED);
		cleanupJob.updateProcessingProgress(successItemCount, failedItemCount, processedItemCount, items.size());
	}

	private DriveStorageQuotaSnapshot fetchDriveStorageQuota(Drive drive, Long cleanupJobId) {
		try {
			About about = drive.about().get()
				.setFields("storageQuota(limit,usage,usageInDrive,usageInDriveTrash)")
				.execute();
			if (about == null || about.getStorageQuota() == null) {
				return DriveStorageQuotaSnapshot.empty();
			}
			Long totalDriveBytes = normalizeDriveQuotaBytes(about.getStorageQuota().getLimit());
			Long usageBytes = selectDriveUsageBytes(
				totalDriveBytes,
				about.getStorageQuota().getUsage(),
				about.getStorageQuota().getUsageInDrive(),
				about.getStorageQuota().getUsageInDriveTrash()
			);
			Long remainingDriveBytes = totalDriveBytes != null && usageBytes != null
				? Math.max(totalDriveBytes - usageBytes, 0L)
				: null;
			return new DriveStorageQuotaSnapshot(remainingDriveBytes, totalDriveBytes);
		} catch (Exception exception) {
			log.warn("Drive storage quota lookup failed. cleanupJobId={}", cleanupJobId, exception);
			return DriveStorageQuotaSnapshot.empty();
		}
	}

	private Long selectDriveUsageBytes(Long totalDriveBytes, Long usageBytes, Long usageInDriveBytes, Long usageInDriveTrashBytes) {
		Long normalizedUsageBytes = normalizeDriveQuotaBytes(usageBytes);
		if (isValidUsageBytes(totalDriveBytes, normalizedUsageBytes)) {
			return normalizedUsageBytes;
		}
		Long driveOnlyUsageBytes = normalizeDriveQuotaBytes(usageInDriveBytes);
		if (isValidUsageBytes(totalDriveBytes, driveOnlyUsageBytes)) {
			log.warn("Drive quota usage fallback applied. totalDriveBytes={}, usageBytes={}, driveOnlyUsageBytes={}",
				totalDriveBytes, normalizedUsageBytes, driveOnlyUsageBytes);
			return driveOnlyUsageBytes;
		}
		return normalizedUsageBytes;
	}

	private boolean isValidUsageBytes(Long totalDriveBytes, Long usageBytes) {
		return totalDriveBytes == null || usageBytes == null || usageBytes <= totalDriveBytes;
	}

	private boolean completeJob(Long cleanupJobId, DriveStorageQuotaSnapshot driveStorageQuotaSnapshot) {
		return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
			CleanupJob cleanupJob = findCleanupJob(cleanupJobId);
			if (cleanupJob.getJobStatus() == CleanupJob.JobStatus.CANCELED) {
				return false;
			}
			List<CleanupJobItem> items = cleanupJobItemRepository
				.findByCleanupJobCleanupJobIdOrderByCleanupItemIdAsc(cleanupJob.getCleanupJobId());
			int successItemCount = countByStatus(items, ProcessStatus.SUCCESS);
			int failedItemCount = countByStatus(items, ProcessStatus.FAILED);
			LocalDateTime completedAt = LocalDateTime.now();
			cleanupJob.complete(successItemCount, failedItemCount, completedAt);
			if (failedItemCount == 0 && successItemCount > 0 && cleanupJob.getActionType() != ActionType.RESTORE_FROM_TRASH) {
				createCompletionHistory(cleanupJob, items, successItemCount, completedAt, driveStorageQuotaSnapshot);
			}
			return cleanupJob.getJobStatus() == CleanupJob.JobStatus.COMPLETED
				|| cleanupJob.getJobStatus() == CleanupJob.JobStatus.PARTIAL_FAILED;
		}));
	}

	private boolean isCanceled(Long cleanupJobId) {
		return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
			CleanupJob cleanupJob = findCleanupJob(cleanupJobId);
			return cleanupJob.getJobStatus() == CleanupJob.JobStatus.CANCELED;
		}));
	}

	private boolean handleCancellation(Long cleanupJobId, ActionType actionType, Gmail gmail, Drive drive) {
		if (!isCanceled(cleanupJobId)) return false;
		rollbackCanceledMoveToTrash(cleanupJobId, actionType, gmail, drive);
		return true;
	}

	private void rollbackCanceledMoveToTrash(Long cleanupJobId, ActionType actionType, Gmail gmail, Drive drive) {
		if (actionType != ActionType.MOVE_TO_TRASH) {
			markPendingItemsSkipped(cleanupJobId, "Cleanup job canceled.");
			return;
		}
		List<CleanupItemWork> successfulItems = getSuccessfulItemWorks(cleanupJobId);
		for (CleanupItemWork item : successfulItems) {
			LocalDateTime processedAt = LocalDateTime.now();
			try {
				restoreMovedItem(item, gmail, drive);
				markItemSkipped(cleanupJobId, item.cleanupItemId(), "Cleanup job canceled and reverted.", processedAt);
			} catch (RuntimeException | IOException exception) {
				markItemFailed(cleanupJobId, item.cleanupItemId(),
					"Cleanup job canceled, but rollback failed: " + exception.getMessage(), processedAt);
			}
		}
		markPendingItemsSkipped(cleanupJobId, "Cleanup job canceled before processing.");
	}

	private void restoreMovedItem(CleanupItemWork item, Gmail gmail, Drive drive) throws IOException {
		validateExternalItemId(item);
		if (item.itemSource() == ItemSource.GMAIL) {
			gmail.users().messages().untrash(GOOGLE_USER_ID, item.externalItemId()).execute();
			return;
		}
		drive.files().update(item.externalItemId(), new File().setTrashed(false))
			.execute();
	}

	private List<CleanupItemWork> getSuccessfulItemWorks(Long cleanupJobId) {
		return transactionTemplate.execute(status -> cleanupJobItemRepository
			.findByCleanupJobCleanupJobIdAndProcessStatusOrderByCleanupItemIdAsc(cleanupJobId, ProcessStatus.SUCCESS)
			.stream()
			.map(item -> new CleanupItemWork(item.getCleanupItemId(), item.getItemSource(), item.getExternalItemId()))
			.toList());
	}

	private void markItemSkipped(Long cleanupJobId, Long cleanupItemId, String reason, LocalDateTime processedAt) {
		transactionTemplate.executeWithoutResult(status -> {
			CleanupJobItem item = cleanupJobItemRepository.findById(cleanupItemId)
				.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
			item.markSkipped(reason, processedAt);
			updateProcessingProgress(cleanupJobId);
		});
	}

	private void markItemFailed(Long cleanupJobId, Long cleanupItemId, String reason, LocalDateTime processedAt) {
		transactionTemplate.executeWithoutResult(status -> {
			CleanupJobItem item = cleanupJobItemRepository.findById(cleanupItemId)
				.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
			item.markFailed(reason, processedAt);
			updateProcessingProgress(cleanupJobId);
		});
	}

	private void markPendingItemsSkipped(Long cleanupJobId, String reason) {
		transactionTemplate.executeWithoutResult(status -> {
			LocalDateTime processedAt = LocalDateTime.now();
			List<CleanupJobItem> pendingItems = cleanupJobItemRepository
				.findByCleanupJobCleanupJobIdAndProcessStatusOrderByCleanupItemIdAsc(cleanupJobId, ProcessStatus.PENDING);
			pendingItems.forEach(item -> item.markSkipped(reason, processedAt));
			updateProcessingProgress(cleanupJobId);
		});
	}

	private void createCompletionHistory(CleanupJob cleanupJob, List<CleanupJobItem> items, int successItemCount,
		LocalDateTime completedAt, DriveStorageQuotaSnapshot driveStorageQuotaSnapshot) {
		if (cleanupHistoryRepository.existsByCleanupJobCleanupJobId(cleanupJob.getCleanupJobId())) return;
		long reclaimedBytes = items.stream()
			.filter(item -> item.getProcessStatus() == ProcessStatus.SUCCESS)
			.mapToLong(CleanupJobItem::getSnapshotSizeBytes)
			.sum();
		CleanupHistory cleanupHistory = cleanupHistoryRepository.save(CleanupHistory.create(
			cleanupJob.getUser(),
			cleanupJob,
			cleanupJob.getScanJob(),
			cleanupJob.getActionType(),
			successItemCount,
			reclaimedBytes,
			driveStorageQuotaSnapshot.remainingDriveBytes(),
			driveStorageQuotaSnapshot.totalDriveBytes(),
			completedAt
		));
		BigDecimal estimatedCarbonGrams = calculateCarbonGrams(reclaimedBytes);
		carbonSavingHistoryRepository.save(CarbonSavingHistory.create(
			cleanupJob.getUser(),
			cleanupHistory,
			reclaimedBytes,
			estimatedCarbonGrams,
			CARBON_FORMULA_VERSION,
			completedAt
		));
		upsertMonthlyStatistic(cleanupJob, successItemCount, reclaimedBytes, estimatedCarbonGrams, completedAt);
	}

	private GoogleToken refreshGoogleAccessToken(Long userId) {
		String refreshToken = transactionTemplate.execute(status -> {
			OAuthToken oauthToken = oauthTokenRepository.findByUserUserId(userId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_AUTH_TOKEN));
			if (oauthToken.getTokenStatus() != TokenStatus.VALID || oauthToken.getEncryptedRefreshToken() == null
				|| oauthToken.getEncryptedRefreshToken().isBlank()) {
				throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
			}
			return tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken());
		});
		if (refreshToken == null || refreshToken.isBlank()) throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
		GoogleToken googleToken = googleOAuthClient.refreshAccessToken(refreshToken);
		transactionTemplate.executeWithoutResult(status -> {
			OAuthToken oauthToken = oauthTokenRepository.findByUserUserId(userId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_AUTH_TOKEN));
			oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());
		});
		return googleToken;
	}

	private Gmail createGmail(String accessToken) {
		try {
			NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			return new Gmail.Builder(httpTransport, GsonFactory.getDefaultInstance(), createRequestInitializer(accessToken))
				.setApplicationName(APPLICATION_NAME)
				.build();
		} catch (GeneralSecurityException | IOException exception) {
			throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
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

	private CleanupJob findCleanupJob(Long cleanupJobId) {
		return cleanupJobRepository.findById(cleanupJobId)
			.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
	}

	private void validateExternalItemId(CleanupItemWork item) {
		if (item.externalItemId() == null || item.externalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
	}

	private int countByStatus(List<CleanupJobItem> items, ProcessStatus processStatus) {
		return (int)items.stream()
			.filter(item -> item.getProcessStatus() == processStatus)
			.count();
	}

	private BigDecimal calculateCarbonGrams(long reclaimedBytes) {
		return BigDecimal.valueOf(reclaimedBytes)
			.multiply(CARBON_GRAMS_PER_GIB)
			.divide(GIB_BYTES, 4, RoundingMode.HALF_UP);
	}

	private Long normalizeDriveQuotaBytes(Long bytes) {
		if (bytes == null || bytes <= DRIVE_QUOTA_SCALE_CORRECTION_THRESHOLD_BYTES) {
			return bytes;
		}
		long normalizedBytes = Math.round(bytes / (double)DRIVE_QUOTA_SCALE_CORRECTION_FACTOR);
		if (normalizedBytes <= 0 || normalizedBytes > DRIVE_QUOTA_SCALE_CORRECTION_THRESHOLD_BYTES) {
			return bytes;
		}
		log.warn("Drive quota value normalized. rawBytes={}, normalizedBytes={}", bytes, normalizedBytes);
		return normalizedBytes;
	}

	private void upsertMonthlyStatistic(CleanupJob cleanupJob, int successItemCount, long reclaimedBytes,
		BigDecimal estimatedCarbonGrams, LocalDateTime completedAt) {
		String statYearMonth = YearMonth.from(completedAt).format(YEAR_MONTH_FORMATTER);
		long trashedItemCount = cleanupJob.getActionType() == ActionType.MOVE_TO_TRASH ? successItemCount : 0L;
		long permanentlyDeletedItemCount = cleanupJob.getActionType() == ActionType.MOVE_TO_TRASH ? 0L : successItemCount;
		try {
			jdbcTemplate.update("""
				insert into monthly_user_statistics (
					user_id, stat_owner_key, stat_year_month, scan_count, cleanup_count,
					trashed_item_count, permanently_deleted_item_count, reclaimed_bytes, estimated_carbon_grams
				)
				values (?, ?, ?, 0, 1, ?, ?, ?, ?)
				on duplicate key update
					cleanup_count = cleanup_count + values(cleanup_count),
					trashed_item_count = trashed_item_count + values(trashed_item_count),
					permanently_deleted_item_count = permanently_deleted_item_count + values(permanently_deleted_item_count),
					reclaimed_bytes = reclaimed_bytes + values(reclaimed_bytes),
					estimated_carbon_grams = estimated_carbon_grams + values(estimated_carbon_grams)
				""",
				cleanupJob.getUser().getUserId(),
				"USER:" + cleanupJob.getUser().getUserId(),
				statYearMonth,
				trashedItemCount,
				permanentlyDeletedItemCount,
				reclaimedBytes,
				estimatedCarbonGrams
			);
		} catch (DataAccessException exception) {
			return;
		}
	}

	private record CleanupExecutionContext(Long userId, ActionType actionType, List<CleanupItemWork> pendingItems) {
		private static CleanupExecutionContext empty() {
			return new CleanupExecutionContext(null, null, List.of());
		}
	}

	private record CleanupItemWork(Long cleanupItemId, ItemSource itemSource, String externalItemId) {
	}

	private record CleanupItemProcessResult(boolean success, String failureReason, LocalDateTime processedAt) {
		private static CleanupItemProcessResult success(LocalDateTime processedAt) {
			return new CleanupItemProcessResult(true, null, processedAt);
		}

		private static CleanupItemProcessResult failed(String failureReason, LocalDateTime processedAt) {
			return new CleanupItemProcessResult(false, failureReason, processedAt);
		}
	}

	private record DriveStorageQuotaSnapshot(Long remainingDriveBytes, Long totalDriveBytes) {
		private static DriveStorageQuotaSnapshot empty() {
			return new DriveStorageQuotaSnapshot(null, null);
		}
	}
}

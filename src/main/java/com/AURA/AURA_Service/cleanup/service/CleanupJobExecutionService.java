package com.AURA.AURA_Service.cleanup.service;

import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.User;
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
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.About;
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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CleanupJobExecutionService {
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final String GOOGLE_USER_ID = "me";
	private static final String CARBON_FORMULA_VERSION = "v1";
	private static final BigDecimal GIB_BYTES = new BigDecimal("1073741824");
	private static final BigDecimal CARBON_GRAMS_PER_GIB = new BigDecimal("1.5600");
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

	private final CleanupJobRepository cleanupJobRepository;
	private final CleanupJobItemRepository cleanupJobItemRepository;
	private final CleanupHistoryRepository cleanupHistoryRepository;
	private final CarbonSavingHistoryRepository carbonSavingHistoryRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final JdbcTemplate jdbcTemplate;

	public CleanupJobExecutionService(CleanupJobRepository cleanupJobRepository,
		CleanupJobItemRepository cleanupJobItemRepository,
		CleanupHistoryRepository cleanupHistoryRepository,
		CarbonSavingHistoryRepository carbonSavingHistoryRepository,
		OAuthTokenRepository oauthTokenRepository,
		TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient,
		JdbcTemplate jdbcTemplate) {
		this.cleanupJobRepository = cleanupJobRepository;
		this.cleanupJobItemRepository = cleanupJobItemRepository;
		this.cleanupHistoryRepository = cleanupHistoryRepository;
		this.carbonSavingHistoryRepository = carbonSavingHistoryRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional
	public void execute(Long cleanupJobId) {
		CleanupJob cleanupJob = cleanupJobRepository.findById(cleanupJobId)
			.orElseThrow(() -> new CustomException(ErrorCode.CLEANUP_JOB_NOT_FOUND));
		List<CleanupJobItem> pendingItems = cleanupJobItemRepository
			.findByCleanupJobCleanupJobIdAndProcessStatusOrderByCleanupItemIdAsc(cleanupJobId, ProcessStatus.PENDING);
		if (pendingItems.isEmpty()) return;

		cleanupJob.start();
		GoogleToken googleToken;
		Long totalDriveBytes = null;
		try {
			googleToken = refreshGoogleAccessToken(cleanupJob.getUser());
		} catch (RuntimeException exception) {
			LocalDateTime processedAt = LocalDateTime.now();
			pendingItems.forEach(item -> item.markFailed(exception.getMessage(), processedAt));
			completeJob(cleanupJob, null);
			return;
		}
		try {
			Gmail gmail = createGmail(googleToken.accessToken());
			Drive drive = createDrive(googleToken.accessToken());
			try {
				About about = drive.about().get().setFields("storageQuota").execute();
				totalDriveBytes = about.getStorageQuota().getLimit();
			} catch (Exception ignored) { }
			for (CleanupJobItem item : pendingItems) {
				processItem(cleanupJob.getActionType(), item, gmail, drive);
			}
		} catch (RuntimeException exception) {
			LocalDateTime processedAt = LocalDateTime.now();
			pendingItems.stream()
				.filter(item -> item.getProcessStatus() == ProcessStatus.PENDING)
				.forEach(item -> item.markFailed(exception.getMessage(), processedAt));
		}
		completeJob(cleanupJob, totalDriveBytes);
	}

	private void processItem(ActionType actionType, CleanupJobItem item, Gmail gmail, Drive drive) {
		LocalDateTime processedAt = LocalDateTime.now();
		try {
			validateExternalItemId(item);
			if (item.getItemSource() == ItemSource.GMAIL) {
				processGmailItem(actionType, item.getExternalItemId(), gmail);
			} else {
				processDriveItem(actionType, item.getExternalItemId(), drive);
			}
			item.markSuccess(processedAt);
		} catch (RuntimeException | IOException exception) {
			item.markFailed(exception.getMessage(), processedAt);
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
				.setSupportsAllDrives(true)
				.execute();
			return;
		}
		if (actionType == ActionType.RESTORE_FROM_TRASH) {
			drive.files().update(externalItemId, new File().setTrashed(false))
				.setSupportsAllDrives(true)
				.execute();
			return;
		}
		drive.files().delete(externalItemId)
			.setSupportsAllDrives(true)
			.execute();
	}

	private void completeJob(CleanupJob cleanupJob, Long totalDriveBytes) {
		List<CleanupJobItem> items = cleanupJobItemRepository
			.findByCleanupJobCleanupJobIdOrderByCleanupItemIdAsc(cleanupJob.getCleanupJobId());
		int successItemCount = countByStatus(items, ProcessStatus.SUCCESS);
		int failedItemCount = countByStatus(items, ProcessStatus.FAILED);
		LocalDateTime completedAt = LocalDateTime.now();
		cleanupJob.complete(successItemCount, failedItemCount, completedAt);
		if (failedItemCount == 0 && successItemCount > 0 && cleanupJob.getActionType() != ActionType.RESTORE_FROM_TRASH) {
			createCompletionHistory(cleanupJob, items, successItemCount, completedAt, totalDriveBytes);
		}
	}

	private void createCompletionHistory(CleanupJob cleanupJob, List<CleanupJobItem> items, int successItemCount,
		LocalDateTime completedAt, Long totalDriveBytes) {
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
			null,
			totalDriveBytes,
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

	private GoogleToken refreshGoogleAccessToken(User user) {
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_AUTH_TOKEN));
		if (oauthToken.getTokenStatus() != TokenStatus.VALID || oauthToken.getEncryptedRefreshToken() == null
			|| oauthToken.getEncryptedRefreshToken().isBlank()) {
			throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
		}
		GoogleToken googleToken = googleOAuthClient.refreshAccessToken(tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken()));
		oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());
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

	private void validateExternalItemId(CleanupJobItem item) {
		if (item.getExternalItemId() == null || item.getExternalItemId().isBlank()) {
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
}

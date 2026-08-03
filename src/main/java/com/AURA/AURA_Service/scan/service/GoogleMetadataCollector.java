package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleToken;
import com.AURA.AURA_Service.auth.service.TokenEncryptionService;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleMetadataCollector {
	private static final Logger log = LoggerFactory.getLogger(GoogleMetadataCollector.class);
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final String USER_ID = "me";
	private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	private static final String GMAIL_LIST_FIELDS = "nextPageToken,messages(id,threadId)";
	private static final String GMAIL_MESSAGE_FIELDS = "id,threadId,labelIds,snippet,internalDate,sizeEstimate,payload(headers,parts(filename,mimeType,body/size,parts(filename,mimeType,body/size)))";
	private static final String DRIVE_FILE_FIELDS = "nextPageToken,files(id,name,parents,mimeType,size,createdTime,modifiedTime,viewedByMeTime,shared,md5Checksum,owners(emailAddress),trashed,trashedTime)";
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int PAGE_SIZE = 100;
	private static final int DEFAULT_GOOGLE_API_TIMEOUT_SECONDS = 60;
	private static final int GOOGLE_API_MAX_ATTEMPTS = 3;
	private static final int PROGRESS_LOG_INTERVAL = 100;

	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final int googleApiTimeoutMillis;
	private final int gmailMaxMessages;
	private final int driveMaxFiles;

	public GoogleMetadataCollector(UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		TokenEncryptionService tokenEncryptionService, GoogleOAuthClient googleOAuthClient,
		@Value("${aura.google.api-timeout-seconds}") int googleApiTimeoutSeconds,
		@Value("${aura.scan.gmail-max-messages}") int gmailMaxMessages,
		@Value("${aura.scan.drive-max-files}") int driveMaxFiles) {
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.googleApiTimeoutMillis = toMillis(googleApiTimeoutSeconds);
		this.gmailMaxMessages = normalizeLimit(gmailMaxMessages);
		this.driveMaxFiles = normalizeLimit(driveMaxFiles);
	}

	public List<CollectedItem> collect(Long userId, ScanCondition condition) {
		return collect(userId, condition, ScanProgressListener.none());
	}

	public List<CollectedItem> collect(Long userId, ScanCondition condition, ScanProgressListener progressListener) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> permissionException(condition.getScanSource()));
		validateToken(oauthToken, condition.getScanSource());

		String refreshToken = tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken());
		GoogleToken googleToken = googleOAuthClient.refreshAccessToken(refreshToken);
		oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());
		oauthTokenRepository.save(oauthToken);

		log.info("Google metadata scan started. userId={}, scanSource={}", userId, condition.getScanSource());
		List<CollectedItem> items = new ArrayList<>();
		MetadataProgressTracker progressTracker = new MetadataProgressTracker(progressListener,
			requiresGmail(condition.getScanSource()), requiresDrive(condition.getScanSource()), gmailMaxMessages, driveMaxFiles);
		if (requiresGmail(condition.getScanSource())) items.addAll(collectGmail(googleToken.accessToken(), condition, progressTracker));
		if (requiresDrive(condition.getScanSource())) items.addAll(collectDrive(googleToken.accessToken(), condition, progressTracker));
		progressTracker.finish();
		log.info("Google metadata scan finished. userId={}, totalCount={}, mailCount={}, driveCount={}",
			userId, items.size(), countBySource(items, ItemSource.GMAIL), countBySource(items, ItemSource.DRIVE));
		return items;
	}

	private List<CollectedItem> collectGmail(String accessToken, ScanCondition condition, MetadataProgressTracker progressTracker) {
		try {
			Gmail gmail = createGmail(accessToken);
			List<CollectedItem> items = new ArrayList<>();
			String pageToken = null;
			do {
				String currentPageToken = pageToken;
				ListMessagesResponse response = executeWithRetry(() -> gmail.users().messages().list(USER_ID)
					.setQ(createGmailQuery(condition))
					.setMaxResults((long) pageSize(items, gmailMaxMessages))
					.setFields(GMAIL_LIST_FIELDS)
					.setPageToken(currentPageToken)
					.execute());
				progressTracker.updateGmailExpected(response.getResultSizeEstimate());
				if (response.getMessages() != null) {
					for (Message listedMessage : response.getMessages()) {
						if (!hasRemaining(items, gmailMaxMessages)) break;
						items.add(toGmailItem(executeWithRetry(() -> gmail.users().messages().get(USER_ID, listedMessage.getId())
							.setFormat("metadata")
							.setMetadataHeaders(List.of("Subject", "From", "Date"))
							.setFields(GMAIL_MESSAGE_FIELDS)
							.execute()), condition));
						progressTracker.incrementMail();
						logProgress("Gmail metadata scan", items.size());
					}
				}
				pageToken = response.getNextPageToken();
			} while (pageToken != null && hasRemaining(items, gmailMaxMessages));
			return items;
		} catch (GoogleJsonResponseException exception) {
			throw new CustomException(ErrorCode.GOOGLE_GMAIL_SCAN_FAILED, createGoogleApiErrorMessage("Gmail", exception));
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_GMAIL_SCAN_FAILED, "Gmail API 요청 실패: " + exception.getMessage());
		}
	}

	private List<CollectedItem> collectDrive(String accessToken, ScanCondition condition, MetadataProgressTracker progressTracker) {
		try {
			Drive drive = createDrive(accessToken);
			if (condition.getScanSource() == ScanSource.DRIVE_FOLDER) {
				return collectDriveFolder(drive, condition, progressTracker);
			}
			return collectDriveAll(drive, progressTracker);
		} catch (GoogleJsonResponseException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED, createGoogleApiErrorMessage("Drive", exception));
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED, "Google Drive API 요청 실패: " + exception.getMessage());
		}
	}

	private String createGoogleApiErrorMessage(String serviceName, GoogleJsonResponseException exception) {
		GoogleJsonError details = exception.getDetails();
		String reason = null;
		String message = exception.getStatusMessage();
		if (details != null) {
			message = details.getMessage();
			if (details.getErrors() != null && !details.getErrors().isEmpty()) {
				reason = details.getErrors().get(0).getReason();
			}
		}
		return serviceName + " API 오류(status=" + exception.getStatusCode()
			+ ", reason=" + blankToDefault(reason, "unknown")
			+ ", message=" + blankToDefault(message, "no_message") + ")";
	}

	private List<CollectedItem> collectDriveAll(Drive drive, MetadataProgressTracker progressTracker) throws IOException {
		List<CollectedItem> items = new ArrayList<>();
		Map<String, String> folderPathCache = new HashMap<>();
		String pageToken = null;
		do {
			String currentPageToken = pageToken;
			FileList fileList = executeWithRetry(() -> drive.files().list()
				.setQ("mimeType != '" + FOLDER_MIME_TYPE + "' and trashed = false")
				.setFields(DRIVE_FILE_FIELDS)
				.setPageSize(pageSize(items, driveMaxFiles))
				.setPageToken(currentPageToken)
				.setOrderBy("modifiedTime")
				.setSupportsAllDrives(true)
				.setIncludeItemsFromAllDrives(true)
				.execute());
			if (fileList.getFiles() != null) {
				for (File file : fileList.getFiles()) {
					if (!hasRemaining(items, driveMaxFiles)) break;
					String parentId = extractParentId(file);
					items.add(toDriveItem(file, resolveFolderPath(drive, parentId, folderPathCache)));
					progressTracker.incrementDrive();
					logProgress("Drive metadata scan", items.size());
				}
			}
			pageToken = fileList.getNextPageToken();
		} while (pageToken != null && hasRemaining(items, driveMaxFiles));
		return items;
	}

	private List<CollectedItem> collectDriveFolder(Drive drive, ScanCondition condition, MetadataProgressTracker progressTracker) throws IOException {
		List<CollectedItem> items = new ArrayList<>();
		Deque<DriveFolderScope> folderScopes = new ArrayDeque<>();
		folderScopes.add(new DriveFolderScope(condition.getDriveFolderId(), loadFolderName(drive, condition.getDriveFolderId())));
		while (!folderScopes.isEmpty() && hasRemaining(items, driveMaxFiles)) {
			DriveFolderScope scope = folderScopes.removeFirst();
			String pageToken = null;
			do {
				String currentPageToken = pageToken;
				FileList fileList = executeWithRetry(() -> drive.files().list()
					.setQ(createFolderChildrenQuery(scope.folderId(), condition.isIncludeSubfolders()))
					.setFields(DRIVE_FILE_FIELDS)
					.setPageSize(pageSize(items, driveMaxFiles))
					.setPageToken(currentPageToken)
					.setOrderBy("modifiedTime")
					.setSupportsAllDrives(true)
					.setIncludeItemsFromAllDrives(true)
					.execute());
				if (fileList.getFiles() != null) {
					for (File file : fileList.getFiles()) {
						if (isFolder(file)) {
							if (condition.isIncludeSubfolders()) folderScopes.add(new DriveFolderScope(file.getId(), appendPath(scope.folderPath(), file.getName())));
							continue;
						}
						if (!hasRemaining(items, driveMaxFiles)) break;
						items.add(toDriveItem(file, scope.folderPath()));
						progressTracker.incrementDrive();
						logProgress("Drive folder metadata scan", items.size());
					}
				}
				pageToken = fileList.getNextPageToken();
			} while (pageToken != null && hasRemaining(items, driveMaxFiles));
		}
		return items;
	}

	private CollectedItem toGmailItem(Message message, ScanCondition condition) {
		List<String> labelIds = message.getLabelIds() == null ? List.of() : message.getLabelIds();
		long attachmentSizeBytes = sumAttachmentSize(message.getPayload());
		Long effectiveAttachmentSize = condition.isIncludeMailAttachmentSize() ? attachmentSizeBytes : 0L;
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("thread_id", message.getThreadId());
		metadata.put("label_ids", labelIds);
		metadata.put("size_estimate", message.getSizeEstimate());

		return new CollectedItem(
			ItemSource.GMAIL,
			message.getId(),
			message.getThreadId(),
			null,
			header(message, "Subject"),
			extractSenderDomain(header(message, "From")),
			String.join(",", labelIds),
			message.getSnippet(),
			"message/rfc822",
			null,
			toLong(message.getSizeEstimate()),
			effectiveAttachmentSize,
			toLocalDateTime(message.getInternalDate()),
			null,
			null,
			null,
			labelIds.contains("STARRED"),
			labelIds.contains("IMPORTANT"),
			attachmentSizeBytes > 0,
			false,
			null,
			null,
			false,
			null,
			metadata
		);
	}

	private CollectedItem toDriveItem(File file, String folderPath) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("parents", file.getParents());
		metadata.put("shared", file.getShared());
		metadata.put("trashed", file.getTrashed());

		return new CollectedItem(
			ItemSource.DRIVE,
			file.getId(),
			extractParentId(file),
			folderPath,
			file.getName(),
			null,
			null,
			null,
			file.getMimeType(),
			extractExtension(file.getName()),
			file.getSize(),
			0L,
			null,
			toLocalDateTime(file.getCreatedTime()),
			toLocalDateTime(file.getModifiedTime()),
			toLocalDateTime(file.getViewedByMeTime()),
			false,
			false,
			false,
			Boolean.TRUE.equals(file.getShared()),
			file.getMd5Checksum(),
			extractOwnerEmail(file),
			Boolean.TRUE.equals(file.getTrashed()),
			toLocalDateTime(file.getTrashedTime()),
			metadata
		);
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
		HttpCredentialsAdapter credentialsAdapter = new HttpCredentialsAdapter(credentials);
		return request -> {
			credentialsAdapter.initialize(request);
			request.setConnectTimeout(googleApiTimeoutMillis);
			request.setReadTimeout(googleApiTimeoutMillis);
		};
	}

	private void validateToken(OAuthToken oauthToken, ScanSource scanSource) {
		if (oauthToken.getTokenStatus() != TokenStatus.VALID || isBlank(oauthToken.getEncryptedRefreshToken())) {
			throw permissionException(scanSource);
		}
	}

	private boolean requiresGmail(ScanSource scanSource) {
		return scanSource == ScanSource.MAIL || scanSource == ScanSource.MAIL_AND_DRIVE;
	}

	private boolean requiresDrive(ScanSource scanSource) {
		return scanSource == ScanSource.DRIVE_ALL || scanSource == ScanSource.DRIVE_FOLDER || scanSource == ScanSource.MAIL_AND_DRIVE;
	}

	private CustomException permissionException(ScanSource scanSource) {
		if (requiresGmail(scanSource)) return new CustomException(ErrorCode.GMAIL_PERMISSION_REQUIRED);
		return new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED);
	}

	private String createFolderChildrenQuery(String folderId, boolean includeSubfolders) {
		String query = "'" + escapeQueryValue(folderId) + "' in parents and trashed = false";
		if (!includeSubfolders) query += " and mimeType != '" + FOLDER_MIME_TYPE + "'";
		return query;
	}

	private String createGmailQuery(ScanCondition condition) {
		List<String> queryParts = new ArrayList<>();
		queryParts.add("-in:trash");
		if (condition.isApplyRecentConditions()) {
			Integer createdBeforeMonths = condition.getCreatedBeforeMonths();
			Integer excludeRecentDays = condition.getExcludeRecentDays();
			if (createdBeforeMonths != null && createdBeforeMonths > 0) {
				queryParts.add("older_than:" + createdBeforeMonths + "m");
			} else if (excludeRecentDays != null && excludeRecentDays > 0) {
				queryParts.add("older_than:" + excludeRecentDays + "d");
			}
		}
		return String.join(" ", queryParts);
	}

	private String resolveFolderPath(Drive drive, String folderId, Map<String, String> folderPathCache) {
		if (isBlank(folderId)) return null;
		if (folderPathCache.containsKey(folderId)) return folderPathCache.get(folderId);
		try {
			File folder = executeWithRetry(() -> drive.files().get(folderId)
				.setFields("id,name,parents")
				.setSupportsAllDrives(true)
				.execute());
			String parentPath = resolveFolderPath(drive, extractParentId(folder), folderPathCache);
			String folderPath = appendPath(parentPath, folder.getName());
			folderPathCache.put(folderId, folderPath);
			return folderPath;
		} catch (IOException exception) {
			folderPathCache.put(folderId, null);
			return null;
		}
	}

	private String loadFolderName(Drive drive, String folderId) {
		try {
			File folder = executeWithRetry(() -> drive.files().get(folderId)
				.setFields("id,name")
				.setSupportsAllDrives(true)
				.execute());
			return folder.getName();
		} catch (IOException exception) {
			return folderId;
		}
	}

	private long sumAttachmentSize(MessagePart part) {
		if (part == null) return 0L;
		long size = 0L;
		if (!isBlank(part.getFilename()) && part.getBody() != null && part.getBody().getSize() != null) {
			size += part.getBody().getSize();
		}
		if (part.getParts() != null) {
			for (MessagePart child : part.getParts()) {
				size += sumAttachmentSize(child);
			}
		}
		return size;
	}

	private String header(Message message, String name) {
		if (message.getPayload() == null || message.getPayload().getHeaders() == null) return null;
		for (MessagePartHeader header : message.getPayload().getHeaders()) {
			if (name.equalsIgnoreCase(header.getName())) return header.getValue();
		}
		return null;
	}

	private String extractSenderDomain(String from) {
		if (isBlank(from)) return null;
		int atIndex = from.lastIndexOf('@');
		if (atIndex < 0 || atIndex == from.length() - 1) return null;
		String domain = from.substring(atIndex + 1).replace(">", "").trim().toLowerCase(Locale.ROOT);
		int spaceIndex = domain.indexOf(' ');
		if (spaceIndex >= 0) domain = domain.substring(0, spaceIndex);
		return domain;
	}

	private String extractExtension(String fileName) {
		if (isBlank(fileName)) return null;
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0 || dotIndex == fileName.length() - 1) return null;
		return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
	}

	private String extractParentId(File file) {
		List<String> parents = file.getParents();
		if (parents == null || parents.isEmpty()) return null;
		return parents.get(0);
	}

	private String extractOwnerEmail(File file) {
		if (file.getOwners() == null || file.getOwners().isEmpty()) return null;
		return file.getOwners().get(0).getEmailAddress();
	}

	private boolean isFolder(File file) {
		return FOLDER_MIME_TYPE.equals(file.getMimeType());
	}

	private Long toLong(Integer value) {
		return value == null ? 0L : value.longValue();
	}

	private LocalDateTime toLocalDateTime(Long epochMilli) {
		if (epochMilli == null) return null;
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), KOREA_ZONE_ID);
	}

	private LocalDateTime toLocalDateTime(DateTime dateTime) {
		if (dateTime == null) return null;
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime.getValue()), KOREA_ZONE_ID);
	}

	private String appendPath(String parentPath, String name) {
		if (isBlank(parentPath)) return name;
		if (isBlank(name)) return parentPath;
		return parentPath + "/" + name;
	}

	private String escapeQueryValue(String value) {
		return value.replace("\\", "\\\\").replace("'", "\\'");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private boolean hasRemaining(List<CollectedItem> items, int maxCount) {
		if (maxCount <= 0) return true;
		return items.size() < maxCount;
	}

	private long countBySource(List<CollectedItem> items, ItemSource itemSource) {
		return items.stream().filter(item -> item.itemSource() == itemSource).count();
	}

	private void logProgress(String taskName, int collectedCount) {
		if (collectedCount > 0 && collectedCount % PROGRESS_LOG_INTERVAL == 0) {
			log.info("{} progressed. collectedCount={}", taskName, collectedCount);
		}
	}

	private int pageSize(List<CollectedItem> items, int maxCount) {
		if (maxCount <= 0) return PAGE_SIZE;
		return Math.max(1, Math.min(PAGE_SIZE, maxCount - items.size()));
	}

	private int toMillis(int seconds) {
		long normalizedSeconds = normalizePositive(seconds, DEFAULT_GOOGLE_API_TIMEOUT_SECONDS);
		return (int) Math.min(Integer.MAX_VALUE, normalizedSeconds * 1000L);
	}

	private int normalizePositive(int value, int defaultValue) {
		if (value <= 0) return defaultValue;
		return value;
	}

	private int normalizeLimit(int value) {
		return Math.max(value, 0);
	}

	private <T> T executeWithRetry(GoogleApiCall<T> apiCall) throws IOException {
		IOException lastException = null;
		for (int attempt = 1; attempt <= GOOGLE_API_MAX_ATTEMPTS; attempt++) {
			try {
				return apiCall.execute();
			} catch (IOException exception) {
				lastException = exception;
				if (attempt >= GOOGLE_API_MAX_ATTEMPTS || !isRetryable(exception)) throw exception;
				sleepBeforeRetry(attempt, exception);
			}
		}
		throw lastException;
	}

	private boolean isRetryable(IOException exception) {
		if (isTimeout(exception)) return true;
		if (exception instanceof GoogleJsonResponseException googleException) {
			int statusCode = googleException.getStatusCode();
			return statusCode == 408 || statusCode == 429 || statusCode >= 500;
		}
		return false;
	}

	private boolean isTimeout(Throwable throwable) {
		Throwable current = throwable;
		while (current != null) {
			if (current instanceof SocketTimeoutException) return true;
			current = current.getCause();
		}
		return false;
	}

	private void sleepBeforeRetry(int attempt, IOException exception) throws IOException {
		try {
			Thread.sleep(500L * attempt);
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			throw exception;
		}
	}

	private String blankToDefault(String value, String defaultValue) {
		if (isBlank(value)) return defaultValue;
		return value;
	}

	private record DriveFolderScope(String folderId, String folderPath) {
	}

	private static class MetadataProgressTracker {
		private final ScanProgressListener progressListener;
		private final boolean isGmailRequired;
		private final boolean isDriveRequired;
		private final int gmailMaxMessages;
		private final int driveMaxFiles;
		private Integer gmailExpectedCount;
		private Integer driveExpectedCount;
		private int mailCollectedCount;
		private int driveCollectedCount;

		private MetadataProgressTracker(ScanProgressListener progressListener, boolean isGmailRequired,
			boolean isDriveRequired, int gmailMaxMessages, int driveMaxFiles) {
			this.progressListener = progressListener == null ? ScanProgressListener.none() : progressListener;
			this.isGmailRequired = isGmailRequired;
			this.isDriveRequired = isDriveRequired;
			this.gmailMaxMessages = gmailMaxMessages;
			this.driveMaxFiles = driveMaxFiles;
			this.gmailExpectedCount = isGmailRequired && gmailMaxMessages > 0 ? gmailMaxMessages : null;
			this.driveExpectedCount = isDriveRequired && driveMaxFiles > 0 ? driveMaxFiles : null;
			report();
		}

		private void updateGmailExpected(Long resultSizeEstimate) {
			if (!isGmailRequired || resultSizeEstimate == null || resultSizeEstimate < 0) return;
			int estimatedCount = resultSizeEstimate > Integer.MAX_VALUE ? Integer.MAX_VALUE : resultSizeEstimate.intValue();
			int expectedCount = gmailMaxMessages > 0 ? Math.min(gmailMaxMessages, estimatedCount) : estimatedCount;
			gmailExpectedCount = Math.max(mailCollectedCount, expectedCount);
			report();
		}

		private void incrementMail() {
			mailCollectedCount++;
			report();
		}

		private void incrementDrive() {
			driveCollectedCount++;
			report();
		}

		private void finish() {
			if (isGmailRequired && gmailExpectedCount == null) gmailExpectedCount = mailCollectedCount;
			if (isDriveRequired && driveExpectedCount == null) driveExpectedCount = driveCollectedCount;
			report();
		}

		private void report() {
			int collectedCount = mailCollectedCount + driveCollectedCount;
			int expectedCount = expectedCount();
			if (expectedCount <= 0) return;
			progressListener.onMetadataProgress(collectedCount, expectedCount);
		}

		private int expectedCount() {
			int expectedCount = 0;
			if (isGmailRequired) expectedCount += sourceExpectedCount(gmailExpectedCount, mailCollectedCount);
			if (isDriveRequired) expectedCount += sourceExpectedCount(driveExpectedCount, driveCollectedCount);
			return expectedCount;
		}

		private int sourceExpectedCount(Integer expectedCount, int collectedCount) {
			if (expectedCount != null) return Math.max(expectedCount, collectedCount);
			if (collectedCount <= 0) return PAGE_SIZE;
			return collectedCount + PAGE_SIZE;
		}
	}

	@FunctionalInterface
	private interface GoogleApiCall<T> {
		T execute() throws IOException;
	}
}

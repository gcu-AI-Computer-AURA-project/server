package com.AURA.AURA_Service.storage.service;

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
import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateResponse;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobItemRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobRepository;
import com.AURA.AURA_Service.cleanup.service.CleanupJobExecutionLauncher;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.repository.ScannedItemRepository;
import com.AURA.AURA_Service.storage.dto.StorageItemDetailResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemListItemResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemLiveDetailResponse;
import com.AURA.AURA_Service.storage.dto.StorageMoveToTrashRequest;
import com.AURA.AURA_Service.storage.dto.StorageItemPageResponse;
import com.AURA.AURA_Service.storage.dto.StoragePermanentDeleteRequest;
import com.AURA.AURA_Service.storage.dto.StorageTrashEmptyRequest;
import com.AURA.AURA_Service.storage.dto.StorageTrashEmptyRequest.TargetSource;
import com.AURA.AURA_Service.storage.dto.StorageTrashItemResponse;
import com.AURA.AURA_Service.storage.dto.StorageTrashPageResponse;
import com.AURA.AURA_Service.storage.dto.StorageTrashRestoreRequest;
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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

@Service
public class StorageItemService {
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final String GOOGLE_USER_ID = "me";
	private static final String DRIVE_ROOT_FOLDER_ID = "root";
	private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	private static final String GMAIL_LIST_FIELDS = "nextPageToken,resultSizeEstimate,messages(id,threadId)";
	private static final String GMAIL_MESSAGE_FIELDS = "id,threadId,labelIds,snippet,internalDate,sizeEstimate,payload(headers)";
	private static final String GMAIL_DETAIL_FIELDS = "id,threadId,labelIds,snippet,internalDate,sizeEstimate,payload";
	private static final String DRIVE_FILE_FIELDS = "id,name,mimeType,size,createdTime,modifiedTime,viewedByMeTime,shared,owners(emailAddress),trashed,trashedTime,parents,driveId,webViewLink";
	private static final String DRIVE_LIST_FIELDS = "nextPageToken,files(" + DRIVE_FILE_FIELDS + ")";
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 30;
	private static final int MAX_SIZE = 100;
	private static final int EMPTY_TRASH_GOOGLE_PAGE_SIZE = 100;
	private static final String PERMANENT_DELETE_CONFIRMATION_TEXT = "\uc601\uad6c\uc0ad\uc81c";
	private static final String EMPTY_TRASH_CONFIRMATION_TEXT = "\ud734\uc9c0\ud1b5\ube44\uc6b0\uae30";

	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final ScannedItemRepository scannedItemRepository;
	private final CleanupJobRepository cleanupJobRepository;
	private final CleanupJobItemRepository cleanupJobItemRepository;
	private final CleanupJobExecutionLauncher cleanupJobExecutionLauncher;

	public StorageItemService(UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		GooglePermissionRepository googlePermissionRepository, TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient,
		ScannedItemRepository scannedItemRepository, CleanupJobRepository cleanupJobRepository,
		CleanupJobItemRepository cleanupJobItemRepository,
		CleanupJobExecutionLauncher cleanupJobExecutionLauncher) {
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.scannedItemRepository = scannedItemRepository;
		this.cleanupJobRepository = cleanupJobRepository;
		this.cleanupJobItemRepository = cleanupJobItemRepository;
		this.cleanupJobExecutionLauncher = cleanupJobExecutionLauncher;
	}

	@Transactional
	public StorageItemPageResponse getItems(Long userId, ItemSource itemSource, Boolean trashed, String sort,
		Integer page, Integer size, String parentId) {
		GoogleToken googleToken = refreshGoogleAccessToken(userId, itemSource);
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);
		return switch (itemSource) {
			case GMAIL -> getLiveGmailItems(userId, googleToken.accessToken(), trashed, normalizedPage, normalizedSize);
			case DRIVE -> getLiveDriveItems(userId, googleToken.accessToken(), trashed, sort, normalizedPage, normalizedSize,
				parentId);
		};
	}

	@Transactional
	public StorageItemDetailResponse getItem(Long userId, Long itemId) {
		if (!userRepository.existsById(userId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		ScannedItem item = scannedItemRepository.findDetailByItemIdAndUserId(itemId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
		if (item.getExternalItemId() == null || item.getExternalItemId().isBlank()) {
			return StorageItemDetailResponse.fromSnapshot(item);
		}
		GoogleToken googleToken = refreshGoogleAccessToken(userId, item.getItemSource());
		StorageItemLiveDetailResponse liveMetadata = switch (item.getItemSource()) {
			case GMAIL -> getLiveGmailDetail(googleToken.accessToken(), item.getExternalItemId());
			case DRIVE -> getLiveDriveDetail(googleToken.accessToken(), item.getExternalItemId());
		};
		return StorageItemDetailResponse.fromLiveMetadata(item, liveMetadata);
	}

	@Transactional
	public StorageItemLiveDetailResponse getLiveItemDetail(Long userId, ItemSource itemSource, String externalItemId) {
		if (externalItemId == null || externalItemId.isBlank()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		GoogleToken googleToken = refreshGoogleAccessToken(userId, itemSource);
		return switch (itemSource) {
			case GMAIL -> getLiveGmailDetail(googleToken.accessToken(), externalItemId.trim());
			case DRIVE -> getLiveDriveDetail(googleToken.accessToken(), externalItemId.trim());
		};
	}

	@Transactional
	public String getLiveGmailBodyText(Long userId, String externalItemId) {
		if (externalItemId == null || externalItemId.isBlank()) {
			return null;
		}
		GoogleToken googleToken = refreshGoogleAccessToken(userId, ItemSource.GMAIL);
		return getLiveGmailBodyText(googleToken.accessToken(), externalItemId.trim());
	}

	@Transactional
	public StorageTrashPageResponse getTrashItems(Long userId, ItemSource itemSource, Integer page, Integer size) {
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);
		if (itemSource == null) {
			GoogleToken googleToken = refreshGoogleAccessToken(userId, ItemSource.GMAIL);
			validateGooglePermission(userId, ItemSource.DRIVE);
			StorageTrashPageResponse gmailResponse = getLiveGmailTrashItems(userId, googleToken.accessToken(),
				normalizedPage, normalizedSize);
			StorageTrashPageResponse driveResponse = getLiveDriveTrashItems(userId, googleToken.accessToken(),
				normalizedPage, normalizedSize);
			List<StorageTrashItemResponse> content = new ArrayList<>();
			content.addAll(gmailResponse.content());
			content.addAll(driveResponse.content());
			content.sort(Comparator.comparing(StorageTrashItemResponse::trashedAt,
				Comparator.nullsLast(Comparator.reverseOrder())));
			List<StorageTrashItemResponse> pagedContent = content.stream()
				.limit(normalizedSize)
				.toList();
			return StorageTrashPageResponse.live(pagedContent, normalizedPage, normalizedSize,
				gmailResponse.totalElements() + driveResponse.totalElements());
		}
		GoogleToken googleToken = refreshGoogleAccessToken(userId, itemSource);
		return switch (itemSource) {
			case GMAIL -> getLiveGmailTrashItems(userId, googleToken.accessToken(), normalizedPage, normalizedSize);
			case DRIVE -> getLiveDriveTrashItems(userId, googleToken.accessToken(), normalizedPage, normalizedSize);
		};
	}

	@Transactional
	public CleanupJobCreateResponse permanentDeleteTrashItems(Long userId, StoragePermanentDeleteRequest request) {
		validatePermanentDeleteRequest(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		validatePermanentDeleteItemPermissions(user, request.items());
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, null, ActionType.PERMANENT_DELETE,
			countBySource(request.items(), ItemSource.GMAIL),
			countBySource(request.items(), ItemSource.DRIVE),
			sumSnapshotBytes(request.items()),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = createPermanentDeleteItems(cleanupJob, userId, request.items());
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		cleanupJobExecutionLauncher.launch(cleanupJob.getCleanupJobId());
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	@Transactional
	public CleanupJobCreateResponse moveItemsToTrash(Long userId, StorageMoveToTrashRequest request) {
		validateMoveToTrashRequest(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		validateMoveItemPermissions(user, request.items());
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, null, ActionType.MOVE_TO_TRASH,
			countMoveItemsBySource(request.items(), ItemSource.GMAIL),
			countMoveItemsBySource(request.items(), ItemSource.DRIVE),
			sumMoveSnapshotBytes(request.items()),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = createMoveToTrashItems(cleanupJob, userId, request.items());
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		cleanupJobExecutionLauncher.launch(cleanupJob.getCleanupJobId());
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	@Transactional
	public CleanupJobCreateResponse restoreTrashItems(Long userId, StorageTrashRestoreRequest request) {
		validateRestoreRequest(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		validateRestoreItemPermissions(user, request.items());
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, null, ActionType.RESTORE_FROM_TRASH,
			countRestoreItemsBySource(request.items(), ItemSource.GMAIL),
			countRestoreItemsBySource(request.items(), ItemSource.DRIVE),
			sumRestoreSnapshotBytes(request.items()),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = createRestoreItems(cleanupJob, userId, request.items());
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		cleanupJobExecutionLauncher.launch(cleanupJob.getCleanupJobId());
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	@Transactional
	public CleanupJobCreateResponse emptyTrash(Long userId, StorageTrashEmptyRequest request) {
		validateEmptyTrashRequest(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		validateTargetSourcePermissions(user, request.targetSource());
		GoogleToken googleToken = refreshGoogleAccessToken(userId, firstItemSource(request.targetSource()));
		List<StorageTrashItemResponse> trashItems = collectLiveTrashItems(userId, googleToken.accessToken(),
			request.targetSource());
		if (trashItems.isEmpty()) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		trashItems.forEach(this::validateCleanupExternalItemId);
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, null, ActionType.EMPTY_TRASH,
			countTrashItemsBySource(trashItems, ItemSource.GMAIL),
			countTrashItemsBySource(trashItems, ItemSource.DRIVE),
			sumTrashItemBytes(trashItems),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = trashItems.stream()
			.map(item -> CleanupJobItem.directSnapshot(
				cleanupJob,
				findOptionalScannedItem(userId, item),
				item.itemSource(),
				item.externalItemId().trim(),
				item.title(),
				defaultZero(item.sizeBytes())
			))
			.toList();
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		cleanupJobExecutionLauncher.launch(cleanupJob.getCleanupJobId());
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	private GoogleToken refreshGoogleAccessToken(Long userId, ItemSource itemSource) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(resolvePermissionError(itemSource)));
		validateToken(oauthToken, itemSource);
		validateGooglePermission(user, oauthToken, itemSource);
		GoogleToken googleToken = googleOAuthClient.refreshAccessToken(tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken()));
		oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());
		return googleToken;
	}

	private StorageItemPageResponse getLiveGmailItems(Long userId, String accessToken, Boolean trashed, int page,
		int size) {
		try {
			Gmail gmail = createGmail(accessToken);
			ListMessagesResponse response = getGmailMessagePage(gmail, createGmailStorageQuery(trashed), page, size);
			List<Message> messages = response.getMessages() == null ? List.of() : response.getMessages();
			Map<String, Long> snapshotItemIds = findSnapshotItemIds(userId, ItemSource.GMAIL,
				messages.stream().map(Message::getId).toList());
			List<StorageItemListItemResponse> content = new ArrayList<>();
			for (Message listedMessage : messages) {
				Message message = gmail.users().messages().get(GOOGLE_USER_ID, listedMessage.getId())
					.setFormat("metadata")
					.setMetadataHeaders(List.of("Subject", "From"))
					.setFields(GMAIL_MESSAGE_FIELDS)
					.execute();
				content.add(toGmailListItem(message, snapshotItemIds.get(message.getId())));
			}
			return StorageItemPageResponse.live(content, page, size, resolveTotalElements(response.getResultSizeEstimate(), page, size, content.size()));
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_GMAIL_SCAN_FAILED);
		}
	}

	private StorageItemPageResponse getLiveDriveItems(Long userId, String accessToken, Boolean trashed, String sort,
		int page, int size, String parentId) {
		try {
			FileList fileList = getDriveFilePage(createDrive(accessToken), createDriveStorageQuery(trashed, parentId),
				createDriveOrderBy(sort), page, size);
			List<File> files = filterMyDriveFiles(fileList.getFiles());
			Map<String, Long> snapshotItemIds = findSnapshotItemIds(userId, ItemSource.DRIVE,
				files.stream().map(File::getId).toList());
			List<StorageItemListItemResponse> content = files.stream()
				.map(file -> toDriveListItem(file, snapshotItemIds.get(file.getId())))
				.toList();
			return StorageItemPageResponse.live(content, page, size, resolveTotalElements(null, page, size, content.size()));
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED);
		}
	}

	private StorageTrashPageResponse getLiveGmailTrashItems(Long userId, String accessToken, int page, int size) {
		StorageItemPageResponse response = getLiveGmailItems(userId, accessToken, true, page, size);
		List<StorageTrashItemResponse> content = response.content().stream()
			.map(StorageTrashItemResponse::live)
			.toList();
		return StorageTrashPageResponse.live(content, page, size, response.totalElements());
	}

	private StorageTrashPageResponse getLiveDriveTrashItems(Long userId, String accessToken, int page, int size) {
		StorageItemPageResponse response = getLiveDriveItems(userId, accessToken, true, "modified_desc", page, size,
			null);
		List<StorageTrashItemResponse> content = response.content().stream()
			.map(StorageTrashItemResponse::live)
			.toList();
		return StorageTrashPageResponse.live(content, page, size, response.totalElements());
	}

	private List<StorageTrashItemResponse> collectLiveTrashItems(Long userId, String accessToken,
		TargetSource targetSource) {
		List<StorageTrashItemResponse> content = new ArrayList<>();
		if (targetSource == TargetSource.GMAIL || targetSource == TargetSource.ALL) {
			content.addAll(collectLiveGmailTrashItems(userId, accessToken));
		}
		if (targetSource == TargetSource.DRIVE || targetSource == TargetSource.ALL) {
			content.addAll(collectLiveDriveTrashItems(userId, accessToken));
		}
		return content.stream()
			.sorted(Comparator.comparing(StorageTrashItemResponse::itemSource)
				.thenComparing(StorageTrashItemResponse::externalItemId, Comparator.nullsLast(String::compareTo)))
			.toList();
	}

	private List<StorageTrashItemResponse> collectLiveGmailTrashItems(Long userId, String accessToken) {
		try {
			Gmail gmail = createGmail(accessToken);
			List<StorageTrashItemResponse> content = new ArrayList<>();
			String pageToken = null;
			do {
				ListMessagesResponse response = gmail.users().messages().list(GOOGLE_USER_ID)
					.setQ("in:trash")
					.setMaxResults((long)EMPTY_TRASH_GOOGLE_PAGE_SIZE)
					.setFields(GMAIL_LIST_FIELDS)
					.setPageToken(pageToken)
					.execute();
				List<Message> messages = response.getMessages() == null ? List.of() : response.getMessages();
				Map<String, Long> snapshotItemIds = findSnapshotItemIds(userId, ItemSource.GMAIL,
					messages.stream().map(Message::getId).toList());
				for (Message listedMessage : messages) {
					Message message = gmail.users().messages().get(GOOGLE_USER_ID, listedMessage.getId())
						.setFormat("metadata")
						.setMetadataHeaders(List.of("Subject", "From"))
						.setFields(GMAIL_MESSAGE_FIELDS)
						.execute();
					StorageItemListItemResponse item = toGmailListItem(message, snapshotItemIds.get(message.getId()));
					content.add(StorageTrashItemResponse.live(item));
				}
				pageToken = response.getNextPageToken();
			} while (pageToken != null);
			return content;
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_GMAIL_SCAN_FAILED);
		}
	}

	private List<StorageTrashItemResponse> collectLiveDriveTrashItems(Long userId, String accessToken) {
		try {
			Drive drive = createDrive(accessToken);
			List<StorageTrashItemResponse> content = new ArrayList<>();
			String pageToken = null;
			do {
				FileList fileList = drive.files().list()
					.setQ(createDriveStorageQuery(true, null))
					.setFields(DRIVE_LIST_FIELDS)
					.setPageSize(EMPTY_TRASH_GOOGLE_PAGE_SIZE)
					.setPageToken(pageToken)
					.setOrderBy("modifiedTime desc")
					.setCorpora("user")
					.setIncludeItemsFromAllDrives(false)
					.setSupportsAllDrives(false)
					.execute();
				List<File> files = filterMyDriveFiles(fileList.getFiles());
				Map<String, Long> snapshotItemIds = findSnapshotItemIds(userId, ItemSource.DRIVE,
					files.stream().map(File::getId).toList());
				content.addAll(files.stream()
					.map(file -> {
						StorageItemListItemResponse item = toDriveListItem(file, snapshotItemIds.get(file.getId()));
						return StorageTrashItemResponse.live(item);
					})
					.toList());
				pageToken = fileList.getNextPageToken();
			} while (pageToken != null);
			return content;
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED);
		}
	}

	private ListMessagesResponse getGmailMessagePage(Gmail gmail, String query, int page, int size) throws IOException {
		String pageToken = null;
		ListMessagesResponse response = new ListMessagesResponse();
		for (int index = 0; index <= page; index++) {
			response = gmail.users().messages().list(GOOGLE_USER_ID)
				.setQ(query)
				.setMaxResults((long)size)
				.setFields(GMAIL_LIST_FIELDS)
				.setPageToken(pageToken)
				.execute();
			pageToken = response.getNextPageToken();
			if (pageToken == null && index < page) return new ListMessagesResponse().setMessages(List.of());
		}
		return response;
	}

	private FileList getDriveFilePage(Drive drive, String query, String orderBy, int page, int size) throws IOException {
		String pageToken = null;
		FileList fileList = new FileList();
		for (int index = 0; index <= page; index++) {
			fileList = drive.files().list()
				.setQ(query)
				.setFields(DRIVE_LIST_FIELDS)
				.setPageSize(size)
				.setPageToken(pageToken)
				.setOrderBy(orderBy)
				.setCorpora("user")
				.setIncludeItemsFromAllDrives(false)
				.setSupportsAllDrives(false)
				.execute();
			pageToken = fileList.getNextPageToken();
			if (pageToken == null && index < page) return new FileList().setFiles(List.of());
		}
		return fileList;
	}

	private Map<String, Long> findSnapshotItemIds(Long userId, ItemSource itemSource, List<String> externalItemIds) {
		if (externalItemIds.isEmpty()) return Map.of();
		Map<String, Long> itemIds = new LinkedHashMap<>();
		scannedItemRepository.findLatestSnapshots(userId, itemSource, externalItemIds)
			.forEach(item -> itemIds.putIfAbsent(item.getExternalItemId(), item.getItemId()));
		return itemIds;
	}

	private StorageItemListItemResponse toGmailListItem(Message message, Long itemId) {
		List<String> labelIds = message.getLabelIds() == null ? List.of() : message.getLabelIds();
		return StorageItemListItemResponse.live(
			itemId,
			ItemSource.GMAIL,
			message.getId(),
			header(message, "Subject"),
			toLong(message.getSizeEstimate()),
			"message/rfc822",
			null,
			toLocalDateTime(message.getInternalDate()),
			null,
			false,
			labelIds.contains("TRASH"),
			null
		);
	}

	private StorageItemListItemResponse toDriveListItem(File file, Long itemId) {
		boolean folder = isDriveFolder(file);
		return StorageItemListItemResponse.live(
			itemId,
			ItemSource.DRIVE,
			file.getId(),
			file.getName(),
			file.getSize(),
			file.getMimeType(),
			folder ? null : extractExtension(file.getName()),
			toLocalDateTime(file.getModifiedTime()),
			toLocalDateTime(file.getViewedByMeTime()),
			Boolean.TRUE.equals(file.getShared()),
			Boolean.TRUE.equals(file.getTrashed()),
			toLocalDateTime(file.getTrashedTime()),
			folder ? "FOLDER" : "FILE",
			folder,
			extractParentFolderId(file),
			extractOwnerEmail(file)
		);
	}

	private String createGmailStorageQuery(Boolean trashed) {
		if (trashed == null) return null;
		return Boolean.TRUE.equals(trashed) ? "in:trash" : "-in:trash";
	}

	private String createDriveStorageQuery(Boolean trashed, String parentId) {
		boolean isTrashed = Boolean.TRUE.equals(trashed);
		StringBuilder query = new StringBuilder("trashed = ").append(isTrashed);
		String normalizedParentId = normalizeDriveParentId(parentId);
		if (!isTrashed || normalizedParentId != null) {
			query.append(" and '")
				.append(escapeDriveQueryValue(normalizedParentId == null ? DRIVE_ROOT_FOLDER_ID : normalizedParentId))
				.append("' in parents");
		}
		return query.toString();
	}

	private List<File> filterMyDriveFiles(List<File> files) {
		if (files == null) return List.of();
		return files.stream()
			.filter(file -> file.getDriveId() == null)
			.toList();
	}

	private String normalizeDriveParentId(String parentId) {
		if (parentId == null || parentId.isBlank()) return null;
		return parentId.trim();
	}

	private String escapeDriveQueryValue(String value) {
		return value.replace("\\", "\\\\").replace("'", "\\'");
	}

	private boolean isDriveFolder(File file) {
		return FOLDER_MIME_TYPE.equals(file.getMimeType());
	}

	private String extractParentFolderId(File file) {
		if (file.getParents() == null || file.getParents().isEmpty()) return null;
		return file.getParents().get(0);
	}

	private String createDriveOrderBy(String sort) {
		return switch (sort == null ? "" : sort) {
			case "size_desc" -> "quotaBytesUsed desc";
			case "created_desc" -> "createdTime desc";
			case "oldest" -> "createdTime";
			default -> "modifiedTime desc";
		};
	}

	private long resolveTotalElements(Long estimatedTotal, int page, int size, int contentSize) {
		if (estimatedTotal != null) return estimatedTotal;
		return (long)page * size + contentSize;
	}

	private void validatePermanentDeleteRequest(StoragePermanentDeleteRequest request) {
		if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		if (!PERMANENT_DELETE_CONFIRMATION_TEXT.equals(request.confirmationText())) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		Set<String> snapshotItemKeys = new HashSet<>();
		for (StoragePermanentDeleteRequest.ItemRequest item : request.items()) {
			validatePermanentDeleteItem(item, snapshotItemKeys);
		}
	}

	private void validatePermanentDeleteItem(StoragePermanentDeleteRequest.ItemRequest item,
		Set<String> snapshotItemKeys) {
		if (item.itemSource() == null) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (item.externalItemId() == null || item.externalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
		if (item.snapshotTitle() == null || item.snapshotTitle().isBlank()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (item.snapshotSizeBytes() < 0) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		String snapshotItemKey = item.itemSource().name() + ":" + item.externalItemId().trim();
		if (!snapshotItemKeys.add(snapshotItemKey)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private List<CleanupJobItem> createPermanentDeleteItems(CleanupJob cleanupJob, Long userId,
		List<StoragePermanentDeleteRequest.ItemRequest> items) {
		return items.stream()
			.map(item -> CleanupJobItem.directSnapshot(
				cleanupJob,
				findOptionalScannedItem(userId, item),
				item.itemSource(),
				item.externalItemId().trim(),
				item.snapshotTitle(),
				item.snapshotSizeBytes()
			))
			.toList();
	}

	private ScannedItem findOptionalScannedItem(Long userId, StoragePermanentDeleteRequest.ItemRequest item) {
		if (item.itemId() == null) return null;
		ScannedItem scannedItem = scannedItemRepository.findDetailByItemIdAndUserId(item.itemId(), userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
		if (scannedItem.getItemSource() != item.itemSource()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return scannedItem;
	}

	private int countBySource(List<StoragePermanentDeleteRequest.ItemRequest> items, ItemSource itemSource) {
		return (int)items.stream()
			.filter(item -> item.itemSource() == itemSource)
			.count();
	}

	private long sumSnapshotBytes(List<StoragePermanentDeleteRequest.ItemRequest> items) {
		return items.stream()
			.mapToLong(StoragePermanentDeleteRequest.ItemRequest::snapshotSizeBytes)
			.sum();
	}

	private void validateMoveToTrashRequest(StorageMoveToTrashRequest request) {
		if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		Set<String> snapshotItemKeys = new HashSet<>();
		for (StorageMoveToTrashRequest.ItemRequest item : request.items()) {
			validateMoveToTrashItem(item, snapshotItemKeys);
		}
	}

	private void validateMoveToTrashItem(StorageMoveToTrashRequest.ItemRequest item, Set<String> snapshotItemKeys) {
		if (item.itemSource() == null) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (item.externalItemId() == null || item.externalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
		if (item.snapshotSizeBytes() != null && item.snapshotSizeBytes() < 0) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		String snapshotItemKey = item.itemSource().name() + ":" + item.externalItemId().trim();
		if (!snapshotItemKeys.add(snapshotItemKey)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private List<CleanupJobItem> createMoveToTrashItems(CleanupJob cleanupJob, Long userId,
		List<StorageMoveToTrashRequest.ItemRequest> items) {
		return items.stream()
			.map(item -> CleanupJobItem.directSnapshot(
				cleanupJob,
				findOptionalScannedItem(userId, item),
				item.itemSource(),
				item.externalItemId().trim(),
				resolveSnapshotTitle(item.snapshotTitle(), item.externalItemId()),
				defaultZero(item.snapshotSizeBytes())
			))
			.toList();
	}

	private ScannedItem findOptionalScannedItem(Long userId, StorageMoveToTrashRequest.ItemRequest item) {
		if (item.itemId() == null) return null;
		ScannedItem scannedItem = scannedItemRepository.findDetailByItemIdAndUserId(item.itemId(), userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
		if (scannedItem.getItemSource() != item.itemSource()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return scannedItem;
	}

	private int countMoveItemsBySource(List<StorageMoveToTrashRequest.ItemRequest> items, ItemSource itemSource) {
		return (int)items.stream()
			.filter(item -> item.itemSource() == itemSource)
			.count();
	}

	private long sumMoveSnapshotBytes(List<StorageMoveToTrashRequest.ItemRequest> items) {
		return items.stream()
			.mapToLong(item -> defaultZero(item.snapshotSizeBytes()))
			.sum();
	}

	private String resolveSnapshotTitle(String snapshotTitle, String externalItemId) {
		if (snapshotTitle != null && !snapshotTitle.isBlank()) return snapshotTitle;
		return externalItemId.trim();
	}

	private void validateRestoreRequest(StorageTrashRestoreRequest request) {
		if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		Set<String> snapshotItemKeys = new HashSet<>();
		for (StorageTrashRestoreRequest.ItemRequest item : request.items()) {
			validateRestoreItem(item, snapshotItemKeys);
		}
	}

	private void validateRestoreItem(StorageTrashRestoreRequest.ItemRequest item, Set<String> snapshotItemKeys) {
		if (item.itemSource() == null) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (item.externalItemId() == null || item.externalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
		if (item.snapshotTitle() == null || item.snapshotTitle().isBlank()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (item.snapshotSizeBytes() != null && item.snapshotSizeBytes() < 0) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		String snapshotItemKey = item.itemSource().name() + ":" + item.externalItemId().trim();
		if (!snapshotItemKeys.add(snapshotItemKey)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private List<CleanupJobItem> createRestoreItems(CleanupJob cleanupJob, Long userId,
		List<StorageTrashRestoreRequest.ItemRequest> items) {
		return items.stream()
			.map(item -> CleanupJobItem.directSnapshot(
				cleanupJob,
				findOptionalScannedItem(userId, item),
				item.itemSource(),
				item.externalItemId().trim(),
				item.snapshotTitle(),
				defaultZero(item.snapshotSizeBytes())
			))
			.toList();
	}

	private ScannedItem findOptionalScannedItem(Long userId, StorageTrashRestoreRequest.ItemRequest item) {
		if (item.itemId() == null) return null;
		ScannedItem scannedItem = scannedItemRepository.findDetailByItemIdAndUserId(item.itemId(), userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
		if (scannedItem.getItemSource() != item.itemSource()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return scannedItem;
	}

	private int countRestoreItemsBySource(List<StorageTrashRestoreRequest.ItemRequest> items, ItemSource itemSource) {
		return (int)items.stream()
			.filter(item -> item.itemSource() == itemSource)
			.count();
	}

	private long sumRestoreSnapshotBytes(List<StorageTrashRestoreRequest.ItemRequest> items) {
		return items.stream()
			.mapToLong(item -> defaultZero(item.snapshotSizeBytes()))
			.sum();
	}

	private void validateEmptyTrashRequest(StorageTrashEmptyRequest request) {
		if (request.targetSource() == null) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		if (!EMPTY_TRASH_CONFIRMATION_TEXT.equals(request.confirmationText())) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private ItemSource firstItemSource(TargetSource targetSource) {
		return targetSource == TargetSource.DRIVE ? ItemSource.DRIVE : ItemSource.GMAIL;
	}

	private ScannedItem findOptionalScannedItem(Long userId, StorageTrashItemResponse item) {
		if (item.itemId() == null) return null;
		ScannedItem scannedItem = scannedItemRepository.findDetailByItemIdAndUserId(item.itemId(), userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
		if (scannedItem.getItemSource() != item.itemSource()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return scannedItem;
	}

	private void validateCleanupExternalItemId(StorageTrashItemResponse item) {
		if (item.externalItemId() == null || item.externalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
	}

	private int countTrashItemsBySource(List<StorageTrashItemResponse> items, ItemSource itemSource) {
		return (int)items.stream()
			.filter(item -> item.itemSource() == itemSource)
			.count();
	}

	private long sumTrashItemBytes(List<StorageTrashItemResponse> items) {
		return items.stream()
			.mapToLong(item -> defaultZero(item.sizeBytes()))
			.sum();
	}

	private int normalizePage(Integer page) {
		return page == null || page < 0 ? DEFAULT_PAGE : page;
	}

	private int normalizeSize(Integer size) {
		if (size == null || size < 1) return DEFAULT_SIZE;
		return Math.min(size, MAX_SIZE);
	}

	private void validateToken(OAuthToken oauthToken, ItemSource itemSource) {
		if (oauthToken.getTokenStatus() != TokenStatus.VALID || oauthToken.getEncryptedRefreshToken() == null
			|| oauthToken.getEncryptedRefreshToken().isBlank()) {
			throw new CustomException(resolvePermissionError(itemSource));
		}
	}

	private void validateGooglePermission(Long userId, ItemSource itemSource) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(resolvePermissionError(itemSource)));
		validateToken(oauthToken, itemSource);
		validateGooglePermission(user, oauthToken, itemSource);
	}

	private void validateGooglePermission(User user, OAuthToken oauthToken, ItemSource itemSource) {
		ServiceType serviceType = toServiceType(itemSource);
		GooglePermission permission = googlePermissionRepository.findByUserAndServiceType(user, serviceType)
			.orElseGet(() -> createPermissionFromToken(user, oauthToken, serviceType));
		if (permission.getPermissionStatus() != PermissionStatus.CONNECTED) {
			throw new CustomException(resolvePermissionError(itemSource));
		}
	}

	private GooglePermission createPermissionFromToken(User user, OAuthToken oauthToken, ServiceType serviceType) {
		GooglePermission permission = GooglePermission.create(user, serviceType);
		LocalDateTime checkedAt = LocalDateTime.now();
		if (hasServiceScope(oauthToken.getScopeText(), serviceType)) {
			permission.connect(oauthToken.getScopeText(), checkedAt);
		} else {
			permission.requireReconnect(oauthToken.getScopeText(), checkedAt);
		}
		return googlePermissionRepository.save(permission);
	}

	private void validatePermanentDeleteItemPermissions(User user, List<StoragePermanentDeleteRequest.ItemRequest> items) {
		Set<ItemSource> itemSources = new HashSet<>();
		items.forEach(item -> itemSources.add(item.itemSource()));
		validateItemSourcePermissions(user, itemSources);
	}

	private void validateMoveItemPermissions(User user, List<StorageMoveToTrashRequest.ItemRequest> items) {
		Set<ItemSource> itemSources = new HashSet<>();
		items.forEach(item -> itemSources.add(item.itemSource()));
		validateItemSourcePermissions(user, itemSources);
	}

	private void validateRestoreItemPermissions(User user, List<StorageTrashRestoreRequest.ItemRequest> items) {
		Set<ItemSource> itemSources = new HashSet<>();
		items.forEach(item -> itemSources.add(item.itemSource()));
		validateItemSourcePermissions(user, itemSources);
	}

	private void validateTargetSourcePermissions(User user, TargetSource targetSource) {
		Set<ItemSource> itemSources = new HashSet<>();
		if (targetSource == TargetSource.GMAIL || targetSource == TargetSource.ALL) {
			itemSources.add(ItemSource.GMAIL);
		}
		if (targetSource == TargetSource.DRIVE || targetSource == TargetSource.ALL) {
			itemSources.add(ItemSource.DRIVE);
		}
		validateItemSourcePermissions(user, itemSources);
	}

	private void validateItemSourcePermissions(User user, Set<ItemSource> itemSources) {
		if (itemSources.isEmpty()) {
			return;
		}
		ItemSource firstItemSource = itemSources.iterator().next();
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(resolvePermissionError(firstItemSource)));
		itemSources.forEach(itemSource -> {
			validateToken(oauthToken, itemSource);
			validateGooglePermission(user, oauthToken, itemSource);
		});
	}

	private ServiceType toServiceType(ItemSource itemSource) {
		return itemSource == ItemSource.GMAIL ? ServiceType.GMAIL : ServiceType.DRIVE;
	}

	private boolean hasServiceScope(String scopeText, ServiceType serviceType) {
		return serviceType == ServiceType.GMAIL ? hasGmailScope(scopeText) : hasScope(scopeText, "drive");
	}

	private boolean hasGmailScope(String scopeText) {
		return hasScope(scopeText, "gmail") || hasScope(scopeText, "mail.google.com");
	}

	private boolean hasScope(String scopeText, String keyword) {
		return scopeText != null && scopeText.toLowerCase(Locale.ROOT).contains(keyword);
	}

	private StorageItemLiveDetailResponse getLiveGmailDetail(String accessToken, String externalItemId) {
		try {
			Message message = getFullGmailMessage(accessToken, externalItemId);
			return new StorageItemLiveDetailResponse(
				null,
				ItemSource.GMAIL,
				message.getId(),
				header(message, "Subject"),
				message.getSnippet(),
				extractGmailBodyText(message),
				"message/rfc822",
				null,
				null,
				null,
				toLong(message.getSizeEstimate()),
				toLocalDateTime(message.getInternalDate()),
				null,
				null,
				false,
				null,
				false,
				true
			);
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_GMAIL_SCAN_FAILED);
		}
	}

	private StorageItemLiveDetailResponse getLiveDriveDetail(String accessToken, String externalItemId) {
		try {
			File file = createDrive(accessToken).files().get(externalItemId)
				.setFields(DRIVE_FILE_FIELDS)
				.setSupportsAllDrives(false)
				.execute();
			if (file.getDriveId() != null) {
				throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED);
			}
			return new StorageItemLiveDetailResponse(
				null,
				ItemSource.DRIVE,
				file.getId(),
				file.getName(),
				null,
				null,
				file.getMimeType(),
				extractExtension(file.getName()),
				null,
				file.getWebViewLink(),
				file.getSize(),
				toLocalDateTime(file.getCreatedTime()),
				toLocalDateTime(file.getModifiedTime()),
				toLocalDateTime(file.getViewedByMeTime()),
				Boolean.TRUE.equals(file.getShared()),
				extractOwnerEmail(file),
				Boolean.TRUE.equals(file.getTrashed()),
				true
			);
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_SCAN_FAILED);
		}
	}

	private String getLiveGmailBodyText(String accessToken, String externalItemId) {
		try {
			return extractGmailBodyText(getFullGmailMessage(accessToken, externalItemId));
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_GMAIL_SCAN_FAILED);
		}
	}

	private Message getFullGmailMessage(String accessToken, String externalItemId) throws IOException {
		return createGmail(accessToken).users().messages().get(GOOGLE_USER_ID, externalItemId)
			.setFormat("full")
			.setFields(GMAIL_DETAIL_FIELDS)
			.execute();
	}

	private String extractGmailBodyText(Message message) {
		if (message.getPayload() == null) return null;
		List<String> plainTextParts = new ArrayList<>();
		List<String> htmlTextParts = new ArrayList<>();
		collectGmailBodyParts(message.getPayload(), plainTextParts, htmlTextParts);
		if (!plainTextParts.isEmpty()) {
			return normalizeBodyText(String.join("\n\n", plainTextParts));
		}
		if (!htmlTextParts.isEmpty()) {
			return normalizeBodyText(convertHtmlToText(String.join("\n\n", htmlTextParts)));
		}
		return null;
	}

	private void collectGmailBodyParts(MessagePart part, List<String> plainTextParts, List<String> htmlTextParts) {
		String mimeType = part.getMimeType() == null ? "" : part.getMimeType().toLowerCase(Locale.ROOT);
		String bodyText = decodeGmailBodyData(part.getBody() == null ? null : part.getBody().getData());
		if (bodyText != null) {
			if (mimeType.startsWith("text/plain")) {
				plainTextParts.add(bodyText);
			} else if (mimeType.startsWith("text/html")) {
				htmlTextParts.add(bodyText);
			}
		}
		if (part.getParts() == null) return;
		part.getParts().forEach(childPart -> collectGmailBodyParts(childPart, plainTextParts, htmlTextParts));
	}

	private String decodeGmailBodyData(String data) {
		if (data == null || data.isBlank()) return null;
		try {
			return new String(Base64.getUrlDecoder().decode(data), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private String convertHtmlToText(String html) {
		String withLineBreaks = html
			.replaceAll("(?is)<script[^>]*>.*?</script>", " ")
			.replaceAll("(?is)<style[^>]*>.*?</style>", " ")
			.replaceAll("(?i)<br\\s*/?>", "\n")
			.replaceAll("(?i)</p>", "\n\n")
			.replaceAll("(?i)</div>", "\n")
			.replaceAll("(?is)<[^>]+>", " ");
		return HtmlUtils.htmlUnescape(withLineBreaks);
	}

	private String normalizeBodyText(String value) {
		if (value == null) return null;
		String normalized = value
			.replace("\r\n", "\n")
			.replace('\r', '\n')
			.replaceAll("[\\t\\x0B\\f ]+", " ")
			.replaceAll("(?m)^\\s+", "")
			.replaceAll("\\n{3,}", "\n\n")
			.trim();
		return normalized.isBlank() ? null : normalized;
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

	private ErrorCode resolvePermissionError(ItemSource itemSource) {
		return itemSource == ItemSource.GMAIL ? ErrorCode.GMAIL_PERMISSION_REQUIRED : ErrorCode.DRIVE_PERMISSION_REQUIRED;
	}

	private LocalDateTime toLocalDateTime(DateTime dateTime) {
		if (dateTime == null) return null;
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime.getValue()), KOREA_ZONE_ID);
	}

	private LocalDateTime toLocalDateTime(Long epochMillis) {
		if (epochMillis == null) return null;
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), KOREA_ZONE_ID);
	}

	private Long toLong(Integer value) {
		return value == null ? 0L : value.longValue();
	}

	private long defaultZero(Long value) {
		return value == null ? 0L : value;
	}

	private String header(Message message, String name) {
		if (message.getPayload() == null || message.getPayload().getHeaders() == null) return null;
		return message.getPayload().getHeaders().stream()
			.filter(header -> name.equalsIgnoreCase(header.getName()))
			.map(MessagePartHeader::getValue)
			.findFirst()
			.orElse(null);
	}

	private String extractOwnerEmail(File file) {
		if (file.getOwners() == null || file.getOwners().isEmpty()) return null;
		return file.getOwners().get(0).getEmailAddress();
	}

	private String extractExtension(String fileName) {
		if (fileName == null || fileName.isBlank()) return null;
		int index = fileName.lastIndexOf('.');
		if (index < 0 || index == fileName.length() - 1) return null;
		return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
	}
}

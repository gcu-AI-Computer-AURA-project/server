package com.AURA.AURA_Service.storage.service;

import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.User;
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
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.repository.ScannedItemRepository;
import com.AURA.AURA_Service.storage.dto.StorageItemDetailResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemListItemResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemLiveDetailResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemPageResponse;
import com.AURA.AURA_Service.storage.dto.StoragePermanentDeleteRequest;
import com.AURA.AURA_Service.storage.dto.StorageTrashEmptyRequest;
import com.AURA.AURA_Service.storage.dto.StorageTrashEmptyRequest.TargetSource;
import com.AURA.AURA_Service.storage.dto.StorageTrashItemResponse;
import com.AURA.AURA_Service.storage.dto.StorageTrashPageResponse;
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
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageItemService {
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final String GOOGLE_USER_ID = "me";
	private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	private static final String GMAIL_LIST_FIELDS = "nextPageToken,resultSizeEstimate,messages(id,threadId)";
	private static final String GMAIL_MESSAGE_FIELDS = "id,threadId,labelIds,snippet,internalDate,sizeEstimate,payload(headers)";
	private static final String DRIVE_FILE_FIELDS = "id,name,mimeType,size,createdTime,modifiedTime,viewedByMeTime,shared,owners(emailAddress),trashed,trashedTime,parents";
	private static final String DRIVE_LIST_FIELDS = "nextPageToken,files(" + DRIVE_FILE_FIELDS + ")";
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 30;
	private static final int MAX_SIZE = 100;
	private static final String PERMANENT_DELETE_CONFIRMATION_TEXT = "\uc601\uad6c\uc0ad\uc81c";
	private static final String EMPTY_TRASH_CONFIRMATION_TEXT = "\ud734\uc9c0\ud1b5\ube44\uc6b0\uae30";

	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final ScannedItemRepository scannedItemRepository;
	private final CleanupJobRepository cleanupJobRepository;
	private final CleanupJobItemRepository cleanupJobItemRepository;

	public StorageItemService(UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		TokenEncryptionService tokenEncryptionService, GoogleOAuthClient googleOAuthClient,
		ScannedItemRepository scannedItemRepository, CleanupJobRepository cleanupJobRepository,
		CleanupJobItemRepository cleanupJobItemRepository) {
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.scannedItemRepository = scannedItemRepository;
		this.cleanupJobRepository = cleanupJobRepository;
		this.cleanupJobItemRepository = cleanupJobItemRepository;
	}

	@Transactional
	public StorageItemPageResponse getItems(Long userId, ItemSource itemSource, Boolean trashed, String sort,
		Integer page, Integer size) {
		GoogleToken googleToken = refreshGoogleAccessToken(userId, itemSource);
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);
		return switch (itemSource) {
			case GMAIL -> getLiveGmailItems(userId, googleToken.accessToken(), trashed, normalizedPage, normalizedSize);
			case DRIVE -> getLiveDriveItems(userId, googleToken.accessToken(), trashed, sort, normalizedPage, normalizedSize);
		};
	}

	@Transactional(readOnly = true)
	public StorageItemDetailResponse getItem(Long userId, Long itemId) {
		if (!userRepository.existsById(userId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		ScannedItem item = scannedItemRepository.findDetailByItemIdAndUserId(itemId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.INVALID_INPUT));
		return StorageItemDetailResponse.fromSnapshot(item);
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
	public StorageTrashPageResponse getTrashItems(Long userId, ItemSource itemSource, Integer page, Integer size) {
		int normalizedPage = normalizePage(page);
		int normalizedSize = normalizeSize(size);
		if (itemSource == null) {
			GoogleToken googleToken = refreshGoogleAccessToken(userId, ItemSource.GMAIL);
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
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, null, ActionType.PERMANENT_DELETE,
			countBySource(request.items(), ItemSource.GMAIL),
			countBySource(request.items(), ItemSource.DRIVE),
			sumSnapshotBytes(request.items()),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = createPermanentDeleteItems(cleanupJob, userId, request.items());
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	@Transactional
	public CleanupJobCreateResponse emptyTrash(Long userId, StorageTrashEmptyRequest request) {
		validateEmptyTrashRequest(request);
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		ItemSource itemSource = toItemSource(request.targetSource());
		List<ScannedItem> trashItems = scannedItemRepository.findAll(createTrashSpecification(userId, itemSource),
			Sort.by(Sort.Order.asc("itemSource"), Sort.Order.asc("itemId")));
		if (trashItems.isEmpty()) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		trashItems.forEach(this::validateCleanupExternalItemId);
		CleanupJob cleanupJob = cleanupJobRepository.save(CleanupJob.create(user, null, ActionType.EMPTY_TRASH,
			countScannedItemsBySource(trashItems, ItemSource.GMAIL),
			countScannedItemsBySource(trashItems, ItemSource.DRIVE),
			sumScannedItemBytes(trashItems),
			LocalDateTime.now()));
		List<CleanupJobItem> cleanupJobItems = trashItems.stream()
			.map(item -> CleanupJobItem.directSnapshot(
				cleanupJob,
				item,
				item.getItemSource(),
				item.getExternalItemId(),
				item.getTitle(),
				item.getEstimatedReclaimBytes()
			))
			.toList();
		cleanupJobItemRepository.saveAll(cleanupJobItems);
		return CleanupJobCreateResponse.from(cleanupJob);
	}

	private GoogleToken refreshGoogleAccessToken(Long userId, ItemSource itemSource) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(resolvePermissionError(itemSource)));
		validateToken(oauthToken, itemSource);
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
		int page, int size) {
		try {
			FileList fileList = getDriveFilePage(createDrive(accessToken), createDriveStorageQuery(trashed),
				createDriveOrderBy(sort), page, size);
			List<File> files = fileList.getFiles() == null ? List.of() : fileList.getFiles();
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
			.map(item -> StorageTrashItemResponse.live(item.itemId(), item.itemSource(), item.externalItemId(),
				item.title(), item.sizeBytes(), item.trashedAt()))
			.toList();
		return StorageTrashPageResponse.live(content, page, size, response.totalElements());
	}

	private StorageTrashPageResponse getLiveDriveTrashItems(Long userId, String accessToken, int page, int size) {
		StorageItemPageResponse response = getLiveDriveItems(userId, accessToken, true, "modified_desc", page, size);
		List<StorageTrashItemResponse> content = response.content().stream()
			.map(item -> StorageTrashItemResponse.live(item.itemId(), item.itemSource(), item.externalItemId(),
				item.title(), item.sizeBytes(), item.trashedAt()))
			.toList();
		return StorageTrashPageResponse.live(content, page, size, response.totalElements());
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
				.setSupportsAllDrives(true)
				.setIncludeItemsFromAllDrives(true)
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
		return StorageItemListItemResponse.live(
			itemId,
			ItemSource.DRIVE,
			file.getId(),
			file.getName(),
			file.getSize(),
			file.getMimeType(),
			extractExtension(file.getName()),
			toLocalDateTime(file.getModifiedTime()),
			toLocalDateTime(file.getViewedByMeTime()),
			Boolean.TRUE.equals(file.getShared()),
			Boolean.TRUE.equals(file.getTrashed()),
			toLocalDateTime(file.getTrashedTime())
		);
	}

	private String createGmailStorageQuery(Boolean trashed) {
		if (trashed == null) return null;
		return Boolean.TRUE.equals(trashed) ? "in:trash" : "-in:trash";
	}

	private String createDriveStorageQuery(Boolean trashed) {
		String query = "mimeType != '" + FOLDER_MIME_TYPE + "'";
		if (trashed == null) return query;
		return query + " and trashed = " + Boolean.TRUE.equals(trashed);
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

	private void validateEmptyTrashRequest(StorageTrashEmptyRequest request) {
		if (!Boolean.TRUE.equals(request.approvalConfirmed())) {
			throw new CustomException(ErrorCode.CLEANUP_EMPTY_TARGET);
		}
		if (!EMPTY_TRASH_CONFIRMATION_TEXT.equals(request.confirmationText())) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private ItemSource toItemSource(TargetSource targetSource) {
		return switch (targetSource) {
			case GMAIL -> ItemSource.GMAIL;
			case DRIVE -> ItemSource.DRIVE;
			case ALL -> null;
		};
	}

	private void validateCleanupExternalItemId(ScannedItem item) {
		if (item.getExternalItemId() == null || item.getExternalItemId().isBlank()) {
			throw new CustomException(ErrorCode.CLEANUP_EXTERNAL_ITEM_ID_REQUIRED);
		}
	}

	private int countScannedItemsBySource(List<ScannedItem> items, ItemSource itemSource) {
		return (int)items.stream()
			.filter(item -> item.getItemSource() == itemSource)
			.count();
	}

	private long sumScannedItemBytes(List<ScannedItem> items) {
		return items.stream()
			.mapToLong(ScannedItem::getEstimatedReclaimBytes)
			.sum();
	}

	private Specification<ScannedItem> createSpecification(Long userId, ItemSource itemSource, Boolean trashed) {
		return (root, query, criteriaBuilder) -> {
			var predicate = criteriaBuilder.and(
				criteriaBuilder.equal(root.get("user").get("userId"), userId),
				criteriaBuilder.equal(root.get("itemSource"), itemSource),
				criteriaBuilder.isNull(root.get("deletedAt"))
			);
			if (trashed == null) return predicate;
			return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isTrashed"), trashed));
		};
	}

	private Specification<ScannedItem> createTrashSpecification(Long userId, ItemSource itemSource) {
		return (root, query, criteriaBuilder) -> {
			var predicate = criteriaBuilder.and(
				criteriaBuilder.equal(root.get("user").get("userId"), userId),
				criteriaBuilder.equal(root.get("isTrashed"), true),
				criteriaBuilder.isNull(root.get("deletedAt"))
			);
			if (itemSource == null) return predicate;
			return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("itemSource"), itemSource));
		};
	}

	private Sort createSort(String sort) {
		return switch (sort == null ? "" : sort) {
			case "size_desc" -> Sort.by(Sort.Order.desc("sizeBytes"), Sort.Order.desc("itemId"));
			case "modified_desc" -> Sort.by(Sort.Order.desc("modifiedTime"), Sort.Order.desc("itemId"));
			case "created_desc" -> Sort.by(Sort.Order.desc("createdTime"), Sort.Order.desc("itemId"));
			case "oldest" -> Sort.by(Sort.Order.asc("createdTime"), Sort.Order.asc("itemId"));
			default -> Sort.by(Sort.Order.desc("itemId"));
		};
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

	private StorageItemLiveDetailResponse getLiveGmailDetail(String accessToken, String externalItemId) {
		try {
			Message message = createGmail(accessToken).users().messages().get(GOOGLE_USER_ID, externalItemId)
				.setFormat("metadata")
				.setMetadataHeaders(List.of("Subject", "From"))
				.execute();
			return new StorageItemLiveDetailResponse(
				null,
				ItemSource.GMAIL,
				message.getId(),
				header(message, "Subject"),
				"message/rfc822",
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
				.setSupportsAllDrives(true)
				.execute();
			return new StorageItemLiveDetailResponse(
				null,
				ItemSource.DRIVE,
				file.getId(),
				file.getName(),
				file.getMimeType(),
				extractExtension(file.getName()),
				null,
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

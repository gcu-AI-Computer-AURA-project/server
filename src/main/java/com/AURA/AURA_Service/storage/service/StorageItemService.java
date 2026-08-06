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
import com.AURA.AURA_Service.storage.dto.StorageTrashItemResponse;
import com.AURA.AURA_Service.storage.dto.StorageTrashPageResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.gmail.Gmail;
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
import java.util.List;
import java.util.Date;
import java.util.Locale;
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
	private static final String DRIVE_FILE_FIELDS = "id,name,mimeType,size,createdTime,modifiedTime,viewedByMeTime,shared,owners(emailAddress),trashed,trashedTime,parents";
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 30;
	private static final int MAX_SIZE = 100;
	private static final String PERMANENT_DELETE_CONFIRMATION_TEXT = "\uc601\uad6c\uc0ad\uc81c";

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

	@Transactional(readOnly = true)
	public StorageItemPageResponse getItems(Long userId, ItemSource itemSource, Boolean trashed, String sort,
		Integer page, Integer size) {
		if (!userRepository.existsById(userId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), createSort(sort));
		Page<ScannedItem> items = scannedItemRepository.findAll(createSpecification(userId, itemSource, trashed), pageable);
		List<StorageItemListItemResponse> content = items.getContent().stream()
			.map(StorageItemListItemResponse::from)
			.toList();
		return StorageItemPageResponse.from(items, content);
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
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(resolvePermissionError(itemSource)));
		validateToken(oauthToken, itemSource);
		GoogleToken googleToken = googleOAuthClient.refreshAccessToken(tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken()));
		oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());
		return switch (itemSource) {
			case GMAIL -> getLiveGmailDetail(googleToken.accessToken(), externalItemId.trim());
			case DRIVE -> getLiveDriveDetail(googleToken.accessToken(), externalItemId.trim());
		};
	}

	@Transactional(readOnly = true)
	public StorageTrashPageResponse getTrashItems(Long userId, ItemSource itemSource, Integer page, Integer size) {
		if (!userRepository.existsById(userId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size),
			Sort.by(Sort.Order.desc("trashedAt"), Sort.Order.desc("itemId")));
		Page<ScannedItem> items = scannedItemRepository.findAll(createTrashSpecification(userId, itemSource), pageable);
		List<StorageTrashItemResponse> content = items.getContent().stream()
			.map(StorageTrashItemResponse::from)
			.toList();
		return StorageTrashPageResponse.from(items, content);
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

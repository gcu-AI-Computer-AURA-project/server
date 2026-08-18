package com.AURA.AURA_Service.storage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient;
import com.AURA.AURA_Service.auth.service.TokenEncryptionService;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobItemRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobRepository;
import com.AURA.AURA_Service.cleanup.service.CleanupJobExecutionLauncher;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.repository.ScannedItemRepository;
import com.AURA.AURA_Service.storage.dto.StorageTrashRestoreRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class StorageItemServiceTest {
	private final UserRepository userRepository = mock(UserRepository.class);
	private final OAuthTokenRepository oauthTokenRepository = mock(OAuthTokenRepository.class);
	private final GooglePermissionRepository googlePermissionRepository = mock(GooglePermissionRepository.class);
	private final TokenEncryptionService tokenEncryptionService = mock(TokenEncryptionService.class);
	private final GoogleOAuthClient googleOAuthClient = mock(GoogleOAuthClient.class);
	private final ScannedItemRepository scannedItemRepository = mock(ScannedItemRepository.class);
	private final CleanupJobRepository cleanupJobRepository = mock(CleanupJobRepository.class);
	private final CleanupJobItemRepository cleanupJobItemRepository = mock(CleanupJobItemRepository.class);
	private final CleanupJobExecutionLauncher cleanupJobExecutionLauncher = mock(CleanupJobExecutionLauncher.class);
	private final StorageItemService storageItemService = new StorageItemService(
		userRepository,
		oauthTokenRepository,
		googlePermissionRepository,
		tokenEncryptionService,
		googleOAuthClient,
		scannedItemRepository,
		cleanupJobRepository,
		cleanupJobItemRepository,
		cleanupJobExecutionLauncher
	);

	@Test
	void restoreTrashItemsUsesExternalIdWhenSnapshotTitleIsMissing() {
		User user = userWithConnectedPermissions();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(scannedItemRepository.findDetailByItemIdAndUserId(999L, 1L)).thenReturn(Optional.empty());
		when(cleanupJobRepository.save(any(CleanupJob.class))).thenAnswer(invocation -> {
			CleanupJob cleanupJob = invocation.getArgument(0);
			ReflectionTestUtils.setField(cleanupJob, "cleanupJobId", 23L);
			return cleanupJob;
		});

		StorageTrashRestoreRequest request = new StorageTrashRestoreRequest(
			List.of(new StorageTrashRestoreRequest.ItemRequest(ItemSource.GMAIL, "gmail-message-1", 999L, null, null)),
			true
		);

		storageItemService.restoreTrashItems(1L, request);

		ArgumentCaptor<CleanupJob> cleanupJobCaptor = ArgumentCaptor.forClass(CleanupJob.class);
		verify(cleanupJobRepository).save(cleanupJobCaptor.capture());
		assertThat(cleanupJobCaptor.getValue().getActionType()).isEqualTo(ActionType.RESTORE_FROM_TRASH);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CleanupJobItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(cleanupJobItemRepository).saveAll(itemsCaptor.capture());
		CleanupJobItem savedItem = itemsCaptor.getValue().get(0);
		assertThat(savedItem.getExternalItemId()).isEqualTo("gmail-message-1");
		assertThat(savedItem.getSnapshotTitle()).isEqualTo("gmail-message-1");
		assertThat(savedItem.getSnapshotSizeBytes()).isZero();
		verify(cleanupJobExecutionLauncher).launch(23L);
	}

	@Test
	void restoreTrashItemsKeepsBulkRestoreWhenStaleItemIdsAreIncluded() {
		User user = userWithConnectedPermissions();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(scannedItemRepository.findDetailByItemIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
		when(scannedItemRepository.findDetailByItemIdAndUserId(200L, 1L)).thenReturn(Optional.empty());
		when(cleanupJobRepository.save(any(CleanupJob.class))).thenAnswer(invocation -> {
			CleanupJob cleanupJob = invocation.getArgument(0);
			ReflectionTestUtils.setField(cleanupJob, "cleanupJobId", 24L);
			return cleanupJob;
		});

		StorageTrashRestoreRequest request = new StorageTrashRestoreRequest(
			List.of(
				new StorageTrashRestoreRequest.ItemRequest(ItemSource.GMAIL, "gmail-message-1", 100L, "", 5L),
				new StorageTrashRestoreRequest.ItemRequest(ItemSource.DRIVE, "drive-file-1", 200L, "Drive file", 10L)
			),
			true
		);

		storageItemService.restoreTrashItems(1L, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CleanupJobItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(cleanupJobItemRepository).saveAll(itemsCaptor.capture());
		List<CleanupJobItem> savedItems = itemsCaptor.getValue();
		assertThat(savedItems).hasSize(2);
		assertThat(savedItems.get(0).getSnapshotTitle()).isEqualTo("gmail-message-1");
		assertThat(savedItems.get(1).getSnapshotTitle()).isEqualTo("Drive file");
		verify(cleanupJobExecutionLauncher).launch(24L);
	}

	@Test
	void restoreTrashItemsPrefersScannedItemExternalId() {
		User user = userWithConnectedPermissions();
		ScannedItem scannedItem = mock(ScannedItem.class);
		when(scannedItem.getItemSource()).thenReturn(ItemSource.GMAIL);
		when(scannedItem.getExternalItemId()).thenReturn("real-gmail-message-id");
		when(scannedItem.getTitle()).thenReturn("Real Gmail title");
		when(scannedItem.getEstimatedReclaimBytes()).thenReturn(15L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(scannedItemRepository.findDetailByItemIdAndUserId(100L, 1L)).thenReturn(Optional.of(scannedItem));
		when(cleanupJobRepository.save(any(CleanupJob.class))).thenAnswer(invocation -> {
			CleanupJob cleanupJob = invocation.getArgument(0);
			ReflectionTestUtils.setField(cleanupJob, "cleanupJobId", 25L);
			return cleanupJob;
		});

		StorageTrashRestoreRequest request = new StorageTrashRestoreRequest(
			List.of(new StorageTrashRestoreRequest.ItemRequest(ItemSource.GMAIL, "100", 100L, null, null)),
			true
		);

		storageItemService.restoreTrashItems(1L, request);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CleanupJobItem>> itemsCaptor = ArgumentCaptor.forClass(List.class);
		verify(cleanupJobItemRepository).saveAll(itemsCaptor.capture());
		CleanupJobItem savedItem = itemsCaptor.getValue().get(0);
		assertThat(savedItem.getExternalItemId()).isEqualTo("real-gmail-message-id");
		assertThat(savedItem.getSnapshotTitle()).isEqualTo("Real Gmail title");
		assertThat(savedItem.getSnapshotSizeBytes()).isEqualTo(15L);
		verify(cleanupJobExecutionLauncher).launch(25L);
	}

	@Test
	void restoreTrashItemsRejectsEmptyItems() {
		StorageTrashRestoreRequest request = new StorageTrashRestoreRequest(List.of(), true);

		assertThatThrownBy(() -> storageItemService.restoreTrashItems(1L, request))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.CLEANUP_EMPTY_TARGET);
	}

	private User userWithConnectedPermissions() {
		User user = User.create("google-provider-id", "aura@example.com", "AURA", null);
		OAuthToken oauthToken = OAuthToken.create(user);
		oauthToken.update("encrypted-refresh-token", 3600L, "gmail drive");
		when(oauthTokenRepository.findByUser(user)).thenReturn(Optional.of(oauthToken));
		when(googlePermissionRepository.findByUserAndServiceType(user, ServiceType.GMAIL))
			.thenReturn(Optional.of(GooglePermission.create(user, ServiceType.GMAIL)));
		when(googlePermissionRepository.findByUserAndServiceType(user, ServiceType.DRIVE))
			.thenReturn(Optional.of(GooglePermission.create(user, ServiceType.DRIVE)));
		return user;
	}
}

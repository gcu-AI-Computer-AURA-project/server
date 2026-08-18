package com.AURA.AURA_Service.cleanup.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.AURA.AURA_Service.auth.domain.Notification;
import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.NotificationRepository;
import com.AURA.AURA_Service.auth.repository.NotificationSettingRepository;
import com.AURA.AURA_Service.auth.service.FcmSendService;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobRepository;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CleanupCompletionNotificationServiceTest {
	private final CleanupJobRepository cleanupJobRepository = mock(CleanupJobRepository.class);
	private final NotificationSettingRepository notificationSettingRepository = mock(NotificationSettingRepository.class);
	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final FcmSendService fcmSendService = mock(FcmSendService.class);
	private final CleanupCompletionNotificationService service = new CleanupCompletionNotificationService(
		cleanupJobRepository,
		notificationSettingRepository,
		notificationRepository,
		fcmSendService
	);

	@Test
	void scanCandidateMoveToTrashSendsNotification() {
		User user = user(1L);
		CleanupJob cleanupJob = completedCleanupJob(7L, user, scanJob(user), ActionType.MOVE_TO_TRASH);
		NotificationSetting notificationSetting = new NotificationSetting(user);
		when(cleanupJobRepository.findById(7L)).thenReturn(Optional.of(cleanupJob));
		when(notificationSettingRepository.findByUser(user)).thenReturn(Optional.of(notificationSetting));

		service.notifyIfCompleted(7L);

		verify(notificationRepository).save(any(Notification.class));
		verify(fcmSendService).sendToUser(any(), any(), any(), any());
	}

	@Test
	void directStorageMoveToTrashDoesNotSendNotification() {
		User user = user(1L);
		CleanupJob cleanupJob = completedCleanupJob(8L, user, null, ActionType.MOVE_TO_TRASH);
		when(cleanupJobRepository.findById(8L)).thenReturn(Optional.of(cleanupJob));

		service.notifyIfCompleted(8L);

		verify(notificationRepository, never()).save(any(Notification.class));
		verify(fcmSendService, never()).sendToUser(any(), any(), any(), any());
	}

	private User user(Long userId) {
		User user = User.create("provider-" + userId, "user" + userId + "@example.com", "사용자", null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}

	private ScanJob scanJob(User user) {
		ScanJob scanJob = new ScanJob(user, null, ScanSource.MAIL_AND_DRIVE, Map.of("scan_source", "MAIL_AND_DRIVE"));
		ReflectionTestUtils.setField(scanJob, "scanJobId", 15L);
		return scanJob;
	}

	private CleanupJob completedCleanupJob(Long cleanupJobId, User user, ScanJob scanJob, ActionType actionType) {
		CleanupJob cleanupJob = CleanupJob.create(user, scanJob, actionType, 2, 1, 1024L, LocalDateTime.now());
		ReflectionTestUtils.setField(cleanupJob, "cleanupJobId", cleanupJobId);
		cleanupJob.complete(3, 0, LocalDateTime.now());
		return cleanupJob;
	}
}

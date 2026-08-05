package com.AURA.AURA_Service.scan.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ScanCompletionNotificationServiceTest {
	private final ScanJobRepository scanJobRepository = mock(ScanJobRepository.class);
	private final NotificationSettingRepository notificationSettingRepository = mock(NotificationSettingRepository.class);
	private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
	private final FcmSendService fcmSendService = mock(FcmSendService.class);
	private final ScanCompletionNotificationService service = new ScanCompletionNotificationService(
		scanJobRepository,
		notificationSettingRepository,
		notificationRepository,
		fcmSendService
	);

	@Test
	void disabledScanCompleteNotificationDoesNotSaveOrSend() {
		User user = user(1L);
		ScanJob scanJob = completedScanJob(15L, user);
		NotificationSetting notificationSetting = new NotificationSetting(user);
		notificationSetting.update(false, true);
		when(scanJobRepository.findById(15L)).thenReturn(Optional.of(scanJob));
		when(notificationSettingRepository.findByUser(user)).thenReturn(Optional.of(notificationSetting));

		service.notifyIfCompleted(15L);

		verify(notificationRepository, never()).save(any(Notification.class));
		verify(fcmSendService, never()).sendToUser(any(), any(), any(), any());
	}

	@Test
	void fcmFailureDoesNotEscape() {
		User user = user(1L);
		ScanJob scanJob = completedScanJob(15L, user);
		NotificationSetting notificationSetting = new NotificationSetting(user);
		when(scanJobRepository.findById(15L)).thenReturn(Optional.of(scanJob));
		when(notificationSettingRepository.findByUser(user)).thenReturn(Optional.of(notificationSetting));
		when(fcmSendService.sendToUser(eq(1L), any(), any(), any())).thenThrow(new CustomException(ErrorCode.FCM_SEND_FAILED));

		assertDoesNotThrow(() -> service.notifyIfCompleted(15L));

		verify(notificationRepository).save(any(Notification.class));
	}

	private User user(Long userId) {
		User user = User.create("provider-" + userId, "user" + userId + "@example.com", "사용자", null);
		ReflectionTestUtils.setField(user, "userId", userId);
		return user;
	}

	private ScanJob completedScanJob(Long scanJobId, User user) {
		ScanJob scanJob = new ScanJob(user, null, ScanSource.MAIL_AND_DRIVE, Map.of("scan_source", "MAIL_AND_DRIVE"));
		ReflectionTestUtils.setField(scanJob, "scanJobId", scanJobId);
		scanJob.markCompleted(3, 1, 1024L);
		return scanJob;
	}
}

package com.AURA.AURA_Service.cleanup.service;

import com.AURA.AURA_Service.auth.domain.Notification;
import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.AURA.AURA_Service.auth.domain.NotificationType;
import com.AURA.AURA_Service.auth.domain.TargetScreen;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.NotificationRepository;
import com.AURA.AURA_Service.auth.repository.NotificationSettingRepository;
import com.AURA.AURA_Service.auth.service.FcmSendService;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.JobStatus;
import com.AURA.AURA_Service.cleanup.repository.CleanupJobRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CleanupCompletionNotificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanupCompletionNotificationService.class);

	private final CleanupJobRepository cleanupJobRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final NotificationRepository notificationRepository;
	private final FcmSendService fcmSendService;

	public CleanupCompletionNotificationService(CleanupJobRepository cleanupJobRepository,
		NotificationSettingRepository notificationSettingRepository, NotificationRepository notificationRepository,
		FcmSendService fcmSendService) {
		this.cleanupJobRepository = cleanupJobRepository;
		this.notificationSettingRepository = notificationSettingRepository;
		this.notificationRepository = notificationRepository;
		this.fcmSendService = fcmSendService;
	}

	public void notifyIfCompleted(Long cleanupJobId) {
		try {
			CleanupJob cleanupJob = cleanupJobRepository.findById(cleanupJobId).orElse(null);
			if (cleanupJob == null || !isNotificationTarget(cleanupJob.getJobStatus())) return;
			User user = cleanupJob.getUser();
			if (!isCleanupCompleteNotificationEnabled(user)) return;

			String title = resolveTitle(cleanupJob);
			String message = resolveMessage(cleanupJob);
			notificationRepository.save(Notification.createCleanupCompleted(user, cleanupJob.getCleanupJobId(), title, message));
			fcmSendService.sendToUser(user.getUserId(), title, message, createData(cleanupJob));
		} catch (RuntimeException exception) {
			LOGGER.warn("Cleanup completion notification failed. cleanupJobId={}", cleanupJobId, exception);
		}
	}

	private boolean isNotificationTarget(JobStatus jobStatus) {
		return jobStatus == JobStatus.COMPLETED || jobStatus == JobStatus.PARTIAL_FAILED;
	}

	private boolean isCleanupCompleteNotificationEnabled(User user) {
		return notificationSettingRepository.findByUser(user)
			.map(NotificationSetting::isScanCompleteEnabled)
			.orElse(true);
	}

	private String resolveTitle(CleanupJob cleanupJob) {
		if (cleanupJob.getJobStatus() == JobStatus.PARTIAL_FAILED) return "정리 작업이 일부 완료되었습니다.";
		return "정리 작업이 완료되었습니다.";
	}

	private String resolveMessage(CleanupJob cleanupJob) {
		return "정리 항목 " + cleanupJob.getSuccessItemCount() + "개를 처리했습니다.";
	}

	private Map<String, String> createData(CleanupJob cleanupJob) {
		return Map.of(
			"notification_type", NotificationType.CLEANUP_COMPLETED.name(),
			"target_screen", TargetScreen.CLEANUP_RESULT.name(),
			"cleanup_job_id", String.valueOf(cleanupJob.getCleanupJobId()),
			"job_status", cleanupJob.getJobStatus().name(),
			"cleaned_item_count", String.valueOf(cleanupJob.getSuccessItemCount()),
			"reclaimed_bytes", String.valueOf(cleanupJob.getTotalSelectedBytes())
		);
	}
}

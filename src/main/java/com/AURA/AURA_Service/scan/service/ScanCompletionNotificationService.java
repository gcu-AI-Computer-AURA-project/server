package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.Notification;
import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.AURA.AURA_Service.auth.domain.NotificationType;
import com.AURA.AURA_Service.auth.domain.TargetScreen;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.NotificationRepository;
import com.AURA.AURA_Service.auth.repository.NotificationSettingRepository;
import com.AURA.AURA_Service.auth.service.FcmSendService;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ScanCompletionNotificationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScanCompletionNotificationService.class);

	private final ScanJobRepository scanJobRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final NotificationRepository notificationRepository;
	private final FcmSendService fcmSendService;

	public ScanCompletionNotificationService(ScanJobRepository scanJobRepository,
		NotificationSettingRepository notificationSettingRepository, NotificationRepository notificationRepository,
		FcmSendService fcmSendService) {
		this.scanJobRepository = scanJobRepository;
		this.notificationSettingRepository = notificationSettingRepository;
		this.notificationRepository = notificationRepository;
		this.fcmSendService = fcmSendService;
	}

	public void notifyIfCompleted(Long scanJobId) {
		try {
			ScanJob scanJob = scanJobRepository.findById(scanJobId).orElse(null);
			if (scanJob == null || !isNotificationTarget(scanJob.getJobStatus())) return;
			User user = scanJob.getUser();
			if (!isScanCompleteNotificationEnabled(user)) return;

			String title = resolveTitle(scanJob);
			String message = resolveMessage(scanJob);
			notificationRepository.save(Notification.createScanCompleted(user, scanJob.getScanJobId(), title, message));
			fcmSendService.sendToUser(user.getUserId(), title, message, createData(scanJob));
		} catch (RuntimeException exception) {
			LOGGER.warn("Scan completion notification failed. scanJobId={}", scanJobId, exception);
		}
	}

	private boolean isNotificationTarget(JobStatus jobStatus) {
		return jobStatus == JobStatus.COMPLETED || jobStatus == JobStatus.PARTIAL_FAILED;
	}

	private boolean isScanCompleteNotificationEnabled(User user) {
		return notificationSettingRepository.findByUser(user)
			.map(NotificationSetting::isScanCompleteEnabled)
			.orElse(true);
	}

	private String resolveTitle(ScanJob scanJob) {
		if (scanJob.getJobStatus() == JobStatus.PARTIAL_FAILED) return "스캔이 일부 완료되었습니다.";
		return "스캔이 완료되었습니다.";
	}

	private String resolveMessage(ScanJob scanJob) {
		return "정리 후보 " + scanJob.getCandidateCount() + "개를 찾았습니다.";
	}

	private Map<String, String> createData(ScanJob scanJob) {
		return Map.of(
			"notification_type", NotificationType.SCAN_COMPLETED.name(),
			"target_screen", TargetScreen.ANALYSIS_SUMMARY.name(),
			"scan_job_id", String.valueOf(scanJob.getScanJobId()),
			"job_status", scanJob.getJobStatus().name(),
			"candidate_count", String.valueOf(scanJob.getCandidateCount()),
			"estimated_reclaim_bytes", String.valueOf(scanJob.getEstimatedReclaimBytes())
		);
	}
}

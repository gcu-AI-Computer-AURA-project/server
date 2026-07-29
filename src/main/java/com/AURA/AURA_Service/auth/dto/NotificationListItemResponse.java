package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.Notification;
import com.AURA.AURA_Service.auth.domain.NotificationType;
import com.AURA.AURA_Service.auth.domain.TargetScreen;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record NotificationListItemResponse(
	@JsonProperty("notification_id")
	Long notificationId,
	@JsonProperty("notification_type")
	NotificationType notificationType,
	String title,
	String message,
	@JsonProperty("target_screen")
	TargetScreen targetScreen,
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("cleanup_job_id")
	Long cleanupJobId,
	@JsonProperty("is_read")
	boolean isRead,
	@JsonProperty("sent_at")
	LocalDateTime sentAt
) {
	public static NotificationListItemResponse from(Notification notification) {
		return new NotificationListItemResponse(
			notification.getNotificationId(),
			notification.getNotificationType(),
			notification.getTitle(),
			notification.getMessage(),
			notification.getTargetScreen(),
			notification.getScanJobId(),
			notification.getCleanupJobId(),
			notification.isRead(),
			notification.getSentAt()
		);
	}
}

package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record NotificationReadResponse(
	@JsonProperty("notification_id")
	Long notificationId,
	@JsonProperty("is_read")
	boolean isRead,
	@JsonProperty("read_at")
	LocalDateTime readAt
) {
}

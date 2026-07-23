package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record NotificationSettingResponse(
	@JsonProperty("is_scan_complete_enabled")
	boolean isScanCompleteEnabled,
	@JsonProperty("is_scan_recommend_enabled")
	boolean isScanRecommendEnabled,
	@JsonProperty("updated_at")
	LocalDateTime updatedAt
) {
	public static NotificationSettingResponse from(NotificationSetting notificationSetting) {
		return new NotificationSettingResponse(
			notificationSetting.isScanCompleteEnabled(),
			notificationSetting.isScanRecommendEnabled(),
			notificationSetting.getUpdatedAt()
		);
	}
}

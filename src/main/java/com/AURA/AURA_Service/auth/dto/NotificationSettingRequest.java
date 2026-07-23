package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record NotificationSettingRequest(
	@NotNull(message = "스캔 완료 알림 수신 여부는 필수 입력값입니다.")
	@JsonProperty("is_scan_complete_enabled")
	Boolean isScanCompleteEnabled,

	@NotNull(message = "스캔 권장 알림 수신 여부는 필수 입력값입니다.")
	@JsonProperty("is_scan_recommend_enabled")
	Boolean isScanRecommendEnabled
) {
}

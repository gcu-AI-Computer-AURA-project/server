package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.FcmToken;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record FcmTokenResponse(
	@JsonProperty("fcm_token_id")
	Long fcmTokenId,
	@JsonProperty("is_active")
	boolean isActive,
	@JsonProperty("updated_at")
	LocalDateTime updatedAt
) {
	public static FcmTokenResponse from(FcmToken fcmToken) {
		return new FcmTokenResponse(fcmToken.getFcmTokenId(), fcmToken.isActive(), fcmToken.getUpdatedAt());
	}
}

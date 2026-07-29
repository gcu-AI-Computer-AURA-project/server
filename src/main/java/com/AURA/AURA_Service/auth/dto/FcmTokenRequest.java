package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FcmTokenRequest(
	@NotBlank(message = "FCM 토큰은 필수 입력값입니다.")
	@Size(max = 500, message = "FCM 토큰은 500자 이하로 입력해야 합니다.")
	@JsonProperty("fcm_token")
	String fcmToken,

	@Size(max = 150, message = "기기 식별자는 150자 이하로 입력해야 합니다.")
	@JsonProperty("device_identifier")
	String deviceIdentifier
) {
}

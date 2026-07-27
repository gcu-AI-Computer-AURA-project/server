package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
	@JsonProperty("refresh_token")
	@NotBlank(message = "Refresh Token은 필수입니다.")
	String refreshToken
) {
}

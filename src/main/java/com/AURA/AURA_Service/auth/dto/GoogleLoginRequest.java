package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
	@JsonProperty("authorization_code") @NotBlank(message = "Authorization Code는 필수입니다.") String authorizationCode,
	@JsonProperty("redirect_uri") @NotBlank(message = "Redirect URI는 필수입니다.") String redirectUri,
	@NotNull(message = "플랫폼은 필수입니다.") Platform platform
) {
	public enum Platform { IOS, ANDROID, WEB }
}

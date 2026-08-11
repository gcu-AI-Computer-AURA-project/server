package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record GoogleLoginRequest(
	@JsonProperty("authorization_code") String authorizationCode,
	@JsonProperty("redirect_uri") String redirectUri,
	@JsonProperty("server_auth_code") String serverAuthCode,
	@NotNull(message = "플랫폼은 필수입니다.") Platform platform
) {
	@AssertTrue(message = "server_auth_code 또는 authorization_code와 redirect_uri를 입력해야 합니다.")
	public boolean isValidCredential() {
		return hasServerAuthCode() || hasAuthorizationCodeFlow();
	}

	public boolean hasServerAuthCode() {
		return hasText(serverAuthCode);
	}

	public boolean hasAuthorizationCodeFlow() {
		return hasText(authorizationCode) && hasText(redirectUri);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	public enum Platform { IOS, ANDROID, WEB }
}

package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.service.JwtTokenService.TokenPair;
import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenRefreshResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("refresh_token") String refreshToken,
	@JsonProperty("token_type") String tokenType,
	@JsonProperty("expires_in") long expiresIn
) {
	public static TokenRefreshResponse from(TokenPair tokenPair, long expiresIn) {
		return new TokenRefreshResponse(tokenPair.accessToken(), tokenPair.refreshToken(), "Bearer", expiresIn);
	}
}

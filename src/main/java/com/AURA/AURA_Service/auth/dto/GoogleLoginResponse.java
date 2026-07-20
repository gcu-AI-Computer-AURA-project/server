package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GoogleLoginResponse(
	@JsonProperty("access_token") String accessToken,
	@JsonProperty("refresh_token") String refreshToken,
	@JsonProperty("token_type") String tokenType,
	@JsonProperty("expires_in") long expiresIn,
	@JsonProperty("is_new_user") boolean isNewUser,
	@JsonProperty("next_step") NextStep nextStep,
	@JsonProperty("is_initial_scan_setup_required") boolean isInitialScanSetupRequired,
	UserResponse user
) {
	public enum NextStep { CONSENT_REQUIRED, SCAN_SETUP_REQUIRED, COMPLETED }

	public record UserResponse(
		@JsonProperty("user_id") Long userId,
		String email,
		@JsonProperty("display_name") String displayName,
		@JsonProperty("profile_image_url") String profileImageUrl
	) { }
}

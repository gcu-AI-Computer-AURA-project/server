package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.User.AccountStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record UserMeResponse(
	@JsonProperty("user_id") Long userId,
	String email,
	@JsonProperty("display_name") String displayName,
	@JsonProperty("profile_image_url") String profileImageUrl,
	@JsonProperty("account_status") AccountStatus accountStatus,
	@JsonProperty("last_login_at") LocalDateTime lastLoginAt,
	@JsonProperty("created_at") LocalDateTime createdAt
) {
	public static UserMeResponse from(User user) {
		return new UserMeResponse(
			user.getUserId(),
			user.getEmail(),
			user.getDisplayName(),
			user.getProfileImageUrl(),
			user.getAccountStatus(),
			user.getLastLoginAt(),
			user.getCreatedAt()
		);
	}
}

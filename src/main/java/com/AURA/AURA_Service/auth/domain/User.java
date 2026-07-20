package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id") private Long userId;
	@Column(name = "google_provider_id", unique = true) private String googleProviderId;
	@Column(unique = true) private String email;
	@Column(name = "display_name") private String displayName;
	@Column(name = "profile_image_url", length = 500) private String profileImageUrl;
	@Enumerated(EnumType.STRING) @Column(name = "account_status", nullable = false) private AccountStatus accountStatus = AccountStatus.ACTIVE;
	@Column(name = "last_login_at") private LocalDateTime lastLoginAt;

	protected User() { }

	public static User create(String providerId, String email, String displayName, String profileImageUrl) {
		User user = new User();
		user.updateGoogleProfile(providerId, email, displayName, profileImageUrl);
		return user;
	}

	public void updateGoogleProfile(String providerId, String email, String displayName, String profileImageUrl) {
		this.googleProviderId = providerId;
		this.email = email;
		this.displayName = displayName;
		this.profileImageUrl = profileImageUrl;
		this.accountStatus = AccountStatus.ACTIVE;
		this.lastLoginAt = LocalDateTime.now();
	}

	public Long getUserId() { return userId; }
	public String getEmail() { return email; }
	public String getDisplayName() { return displayName; }
	public String getProfileImageUrl() { return profileImageUrl; }

	public enum AccountStatus { ACTIVE, DISCONNECTED, WITHDRAWN }
}

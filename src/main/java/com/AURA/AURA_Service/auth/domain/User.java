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
	@Column(name = "withdrawn_at") private LocalDateTime withdrawnAt;
	@Column(name = "anonymized_at") private LocalDateTime anonymizedAt;
	@Column(name = "anonymous_user_key") private String anonymousUserKey;
	@Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;

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

	public void withdraw(String anonymousUserKey, LocalDateTime withdrawnAt) {
		this.googleProviderId = null;
		this.email = null;
		this.displayName = null;
		this.profileImageUrl = null;
		this.accountStatus = AccountStatus.WITHDRAWN;
		this.withdrawnAt = withdrawnAt;
		this.anonymizedAt = withdrawnAt;
		this.anonymousUserKey = anonymousUserKey;
	}

	public void disconnectGoogle() {
		this.accountStatus = AccountStatus.DISCONNECTED;
	}

	public Long getUserId() { return userId; }
	public String getEmail() { return email; }
	public String getDisplayName() { return displayName; }
	public String getProfileImageUrl() { return profileImageUrl; }
	public AccountStatus getAccountStatus() { return accountStatus; }
	public LocalDateTime getLastLoginAt() { return lastLoginAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public enum AccountStatus { ACTIVE, DISCONNECTED, WITHDRAWN }
}

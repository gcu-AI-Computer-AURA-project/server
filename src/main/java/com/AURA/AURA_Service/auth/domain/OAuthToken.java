package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "token_id") private Long tokenId;
	@OneToOne @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
	@Column(name = "encrypted_refresh_token", length = 1000) private String encryptedRefreshToken;
	@Column(name = "access_token_expires_at") private LocalDateTime accessTokenExpiresAt;
	@Column(name = "scope_text", columnDefinition = "TEXT") private String scopeText;
	@Enumerated(EnumType.STRING) @Column(name = "token_status", nullable = false) private TokenStatus tokenStatus;
	@Column(name = "last_refreshed_at") private LocalDateTime lastRefreshedAt;

	protected OAuthToken() { }

	public static OAuthToken create(User user) {
		OAuthToken token = new OAuthToken();
		token.user = user;
		return token;
	}

	public void update(String encryptedRefreshToken, long expiresIn, String scopeText) {
		if (encryptedRefreshToken != null) this.encryptedRefreshToken = encryptedRefreshToken;
		this.accessTokenExpiresAt = LocalDateTime.now().plusSeconds(expiresIn);
		this.scopeText = scopeText;
		this.tokenStatus = TokenStatus.VALID;
		this.lastRefreshedAt = LocalDateTime.now();
	}

	public enum TokenStatus { VALID, EXPIRED, REVOKED, REFRESH_FAILED }
}

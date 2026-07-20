package com.AURA.AURA_Service.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleToken;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GoogleOAuthClientTest {
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void googleTokenIgnoresUnknownFields() throws Exception {
		String response = """
			{
				"access_token": "google-access-token",
				"expires_in": 3600,
				"refresh_token": "google-refresh-token",
				"scope": "openid email profile",
				"token_type": "Bearer",
				"id_token": "google-id-token",
				"refresh_token_expires_in": 604800
			}
			""";

		GoogleToken token = objectMapper.readValue(response, GoogleToken.class);

		assertThat(token.accessToken()).isEqualTo("google-access-token");
		assertThat(token.expiresIn()).isEqualTo(3600);
		assertThat(token.refreshToken()).isEqualTo("google-refresh-token");
		assertThat(token.scope()).isEqualTo("openid email profile");
	}

	@Test
	void googleUserIgnoresUnknownFields() throws Exception {
		String response = """
			{
				"sub": "google-provider-id",
				"email": "user@example.com",
				"name": "AURA User",
				"picture": "https://example.com/profile.png",
				"email_verified": true,
				"given_name": "AURA",
				"family_name": "User",
				"locale": "ko"
			}
			""";

		GoogleUser user = objectMapper.readValue(response, GoogleUser.class);

		assertThat(user.sub()).isEqualTo("google-provider-id");
		assertThat(user.email()).isEqualTo("user@example.com");
		assertThat(user.name()).isEqualTo("AURA User");
		assertThat(user.picture()).isEqualTo("https://example.com/profile.png");
	}
}

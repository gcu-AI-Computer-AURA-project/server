package com.AURA.AURA_Service.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.AURA.AURA_Service.common.CustomException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
	private static final String SECRET = "12345678901234567890123456789012";

	private final JwtTokenService jwtTokenService = new JwtTokenService(SECRET, 3600, 1209600);

	@Test
	void getAccessTokenUserIdReturnsSubjectUserId() {
		String token = createToken("1", "ACCESS", 3600);

		Long userId = jwtTokenService.getAccessTokenUserId(token);

		assertThat(userId).isEqualTo(1L);
	}

	@Test
	void getAccessTokenUserIdRejectsRefreshToken() {
		String token = createToken("1", "REFRESH", 3600);

		assertThatThrownBy(() -> jwtTokenService.getAccessTokenUserId(token))
			.isInstanceOf(CustomException.class);
	}

	@Test
	void getRefreshTokenUserIdReturnsSubjectUserId() {
		String token = createToken("1", "REFRESH", 3600);

		Long userId = jwtTokenService.getRefreshTokenUserId(token);

		assertThat(userId).isEqualTo(1L);
	}

	@Test
	void getRefreshTokenUserIdRejectsAccessToken() {
		String token = createToken("1", "ACCESS", 3600);

		assertThatThrownBy(() -> jwtTokenService.getRefreshTokenUserId(token))
			.isInstanceOf(CustomException.class);
	}

	@Test
	void getAccessTokenUserIdRejectsInvalidToken() {
		assertThatThrownBy(() -> jwtTokenService.getAccessTokenUserId("invalid-token"))
			.isInstanceOf(CustomException.class);
	}

	private String createToken(String subject, String type, long expirationSeconds) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(subject)
			.claim("type", type)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(expirationSeconds)))
			.signWith(createKey())
			.compact();
	}

	private SecretKey createKey() {
		return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
	}
}

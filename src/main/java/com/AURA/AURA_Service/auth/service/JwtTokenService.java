package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
	private static final String ACCESS_TOKEN_TYPE = "ACCESS";
	private static final String REFRESH_TOKEN_TYPE = "REFRESH";

	private final String secret;
	private final long accessExpirationSeconds;
	private final long refreshExpirationSeconds;

	public JwtTokenService(@Value("${aura.jwt.secret}") String secret,
		@Value("${aura.jwt.access-token-expiration-seconds}") long accessExpirationSeconds,
		@Value("${aura.jwt.refresh-token-expiration-seconds}") long refreshExpirationSeconds) {
		this.secret = secret;
		this.accessExpirationSeconds = accessExpirationSeconds;
		this.refreshExpirationSeconds = refreshExpirationSeconds;
	}

	public TokenPair issue(User user) {
		return new TokenPair(create(user, ACCESS_TOKEN_TYPE, accessExpirationSeconds), create(user, REFRESH_TOKEN_TYPE, refreshExpirationSeconds));
	}

	public Long getAccessTokenUserId(String token) {
		return getTokenUserId(token, ACCESS_TOKEN_TYPE);
	}

	public Long getRefreshTokenUserId(String token) {
		return getTokenUserId(token, REFRESH_TOKEN_TYPE);
	}

	private Long getTokenUserId(String token, String expectedType) {
		try {
			Claims claims = parse(token);
			validateTokenType(claims, expectedType);
			return Long.valueOf(claims.getSubject());
		} catch (JwtException | IllegalArgumentException | NullPointerException exception) {
			throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
		}
	}

	private String create(User user, String type, long expirationSeconds) {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		Instant now = Instant.now();
		return Jwts.builder().subject(user.getUserId().toString()).claim("type", type)
			.issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(createKey()).compact();
	}

	private Claims parse(String token) {
		return Jwts.parser().verifyWith(createKey()).build().parseSignedClaims(token).getPayload();
	}

	private void validateTokenType(Claims claims, String expectedType) {
		if (!expectedType.equals(claims.get("type", String.class))) {
			throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
		}
	}

	private SecretKey createKey() {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	public record TokenPair(String accessToken, String refreshToken) { }
}

package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
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
		return new TokenPair(create(user, "ACCESS", accessExpirationSeconds), create(user, "REFRESH", refreshExpirationSeconds));
	}

	private String create(User user, String type, long expirationSeconds) {
		if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		Instant now = Instant.now();
		SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		return Jwts.builder().subject(user.getUserId().toString()).claim("type", type)
			.issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(expirationSeconds))).signWith(key).compact();
	}

	public record TokenPair(String accessToken, String refreshToken) { }
}

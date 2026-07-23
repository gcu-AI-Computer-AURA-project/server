package com.AURA.AURA_Service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.AURA.AURA_Service.auth.service.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthenticationFilterTest {
	private static final String SECRET = "12345678901234567890123456789012";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(
		new JwtTokenService(SECRET, 3600, 1209600),
		new SecurityErrorResponseWriter()
	);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void doFilterInternalSetsAuthenticationWhenAccessTokenIsValid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		request.addHeader("Authorization", "Bearer " + createToken("1", "ACCESS", 3600));

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("1");
	}

	@Test
	void doFilterInternalWritesInvalidAuthTokenErrorResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain filterChain = new MockFilterChain();
		request.addHeader("Authorization", "Bearer invalid-token");

		jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(body.get("status").asInt()).isEqualTo(401);
		assertThat(body.get("code").asText()).isEqualTo("AUTH-002");
		assertThat(body.get("message").asText()).isEqualTo("인증 토큰이 만료되었거나 유효하지 않습니다.");
		assertThat(body.get("path").asText()).isEqualTo("/api/users/me");
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

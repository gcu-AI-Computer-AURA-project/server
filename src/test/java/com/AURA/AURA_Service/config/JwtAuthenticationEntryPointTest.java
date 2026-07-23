package com.AURA.AURA_Service.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthenticationEntryPointTest {
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint =
		new JwtAuthenticationEntryPoint(new SecurityErrorResponseWriter());

	@Test
	void commenceWritesAuthTokenMissingErrorResponse() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
		MockHttpServletResponse response = new MockHttpServletResponse();

		jwtAuthenticationEntryPoint.commence(request, response, null);

		JsonNode body = objectMapper.readTree(response.getContentAsString());
		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(body.get("status").asInt()).isEqualTo(401);
		assertThat(body.get("code").asText()).isEqualTo("AUTH-001");
		assertThat(body.get("message").asText()).isEqualTo("인증 토큰이 없습니다.");
		assertThat(body.get("path").asText()).isEqualTo("/api/users/me");
	}
}

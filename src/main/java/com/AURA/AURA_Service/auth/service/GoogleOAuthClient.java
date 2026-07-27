package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleOAuthClient.class);
	private static final URI TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
	private static final URI USER_INFO_URI = URI.create("https://openidconnect.googleapis.com/v1/userinfo");
	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String clientId;
	private final String clientSecret;

	public GoogleOAuthClient(@Value("${aura.google.client-id}") String clientId, @Value("${aura.google.client-secret}") String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public GoogleToken exchangeCode(String authorizationCode, String redirectUri) {
		validateConfiguration();
		String form = "code=" + encode(authorizationCode) + "&client_id=" + encode(clientId)
			+ "&client_secret=" + encode(clientSecret) + "&redirect_uri=" + encode(redirectUri)
			+ "&grant_type=authorization_code";
		HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(form)).build();
		return send(request, GoogleToken.class, ErrorCode.GOOGLE_AUTHENTICATION_FAILED);
	}

	public GoogleUser getUserInfo(String accessToken) {
		HttpRequest request = HttpRequest.newBuilder(USER_INFO_URI)
			.header("Authorization", "Bearer " + accessToken).GET().build();
		return send(request, GoogleUser.class, ErrorCode.GOOGLE_USER_INFO_FAILED);
	}

	public GoogleToken refreshAccessToken(String refreshToken) {
		validateConfiguration();
		String form = "refresh_token=" + encode(refreshToken) + "&client_id=" + encode(clientId)
			+ "&client_secret=" + encode(clientSecret) + "&grant_type=refresh_token";
		HttpRequest request = HttpRequest.newBuilder(TOKEN_URI)
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(form)).build();
		return send(request, GoogleToken.class, ErrorCode.DRIVE_PERMISSION_REQUIRED);
	}

	private <T> T send(HttpRequest request, Class<T> responseType, ErrorCode fallbackErrorCode) {
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) throw mapGoogleError(response, request.uri().toString(), fallbackErrorCode);
			return objectMapper.readValue(response.body(), responseType);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new CustomException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED);
		} catch (IOException exception) {
			LOGGER.warn("Google OAuth 응답 처리에 실패했습니다. endpoint={}", request.uri(), exception);
			throw new CustomException(fallbackErrorCode);
		}
	}

	private CustomException mapGoogleError(HttpResponse<String> response, String endpoint, ErrorCode fallbackErrorCode) {
		try {
			JsonNode root = objectMapper.readTree(response.body());
			JsonNode errorNode = root.path("error");
			String googleError = errorNode.isTextual() ? errorNode.asText() : errorNode.path("status").asText("unknown_error");
			LOGGER.warn("Google OAuth 요청이 거절되었습니다. endpoint={}, status={}, error={}", endpoint, response.statusCode(), googleError);
			return switch (googleError) {
				case "invalid_grant" -> new CustomException(ErrorCode.GOOGLE_AUTHORIZATION_CODE_INVALID);
				case "invalid_client" -> new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
				case "redirect_uri_mismatch" -> new CustomException(ErrorCode.INVALID_REDIRECT_URI);
				case "unauthorized_client" -> new CustomException(ErrorCode.GOOGLE_OAUTH_CLIENT_UNAUTHORIZED);
				case "invalid_request", "unsupported_grant_type" -> new CustomException(ErrorCode.GOOGLE_OAUTH_REQUEST_INVALID);
				case "access_denied" -> new CustomException(ErrorCode.GOOGLE_ACCESS_DENIED);
				default -> new CustomException(fallbackErrorCode);
			};
		} catch (IOException exception) {
			LOGGER.warn("Google OAuth 요청이 거절되었습니다. endpoint={}, status={}", endpoint, response.statusCode());
			return new CustomException(fallbackErrorCode);
		}
	}

	private void validateConfiguration() {
		if (clientId.isBlank() || clientSecret.isBlank()) throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
	}

	private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GoogleToken(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("expires_in") long expiresIn,
		@JsonProperty("refresh_token") String refreshToken,
		String scope
	) { }

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GoogleUser(String sub, String email, String name, String picture) { }

}

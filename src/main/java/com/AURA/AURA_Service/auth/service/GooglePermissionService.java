package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.GooglePermissionRecheckResponse;
import com.AURA.AURA_Service.auth.dto.GooglePermissionReconnectUrlRequest;
import com.AURA.AURA_Service.auth.dto.GooglePermissionReconnectUrlResponse;
import com.AURA.AURA_Service.auth.dto.GooglePermissionResponse;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GooglePermissionService {
	private static final long RECONNECT_URL_EXPIRES_IN = 300;
	private static final String OPENID_SCOPE = "openid";
	private static final String EMAIL_SCOPE = "email";
	private static final String PROFILE_SCOPE = "profile";
	private static final String GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.modify";
	private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;
	private final String configuredRedirectUri;

	public GooglePermissionService(UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		GooglePermissionRepository googlePermissionRepository, TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient, @Value("${aura.google.redirect-uri:}") String configuredRedirectUri) {
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
		this.configuredRedirectUri = configuredRedirectUri;
	}

	@Transactional(readOnly = true)
	public GooglePermissionResponse get(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return GooglePermissionResponse.from(googlePermissionRepository.findByUserOrderByServiceTypeAsc(user));
	}

	@Transactional
	public GooglePermissionRecheckResponse recheck(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED));
		if (oauthToken.getTokenStatus() == TokenStatus.REVOKED || oauthToken.getEncryptedRefreshToken() == null) {
			throw new CustomException(ErrorCode.GOOGLE_AUTHENTICATION_FAILED);
		}

		var googleToken = googleOAuthClient.refreshAccessToken(tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken()));
		String scopeText = googleToken.scope() == null ? oauthToken.getScopeText() : googleToken.scope();
		oauthToken.update(null, googleToken.expiresIn(), scopeText);
		LocalDateTime checkedAt = LocalDateTime.now();
		PermissionStatus gmailStatus = syncRecheckedPermission(user, ServiceType.GMAIL, hasScope(scopeText, "gmail"), scopeText, checkedAt);
		PermissionStatus driveStatus = syncRecheckedPermission(user, ServiceType.DRIVE, hasScope(scopeText, "drive"), scopeText, checkedAt);
		return new GooglePermissionRecheckResponse(gmailStatus, driveStatus, checkedAt);
	}

	public GooglePermissionReconnectUrlResponse createReconnectUrl(GooglePermissionReconnectUrlRequest request) {
		validateRedirectUri(request.redirectUri());
		List<String> scopes = createScopes(request.serviceTypes());
		String authorizationUrl = googleOAuthClient.buildAuthorizationUrl(request.redirectUri(), scopes);
		return new GooglePermissionReconnectUrlResponse(authorizationUrl, RECONNECT_URL_EXPIRES_IN);
	}

	@Transactional
	public void syncConnectedPermissions(User user, String scopeText) {
		LocalDateTime checkedAt = LocalDateTime.now();
		syncPermission(user, ServiceType.GMAIL, hasScope(scopeText, "gmail"), scopeText, checkedAt);
		syncPermission(user, ServiceType.DRIVE, hasScope(scopeText, "drive"), scopeText, checkedAt);
	}

	@Transactional
	public boolean disconnectAll(User user, LocalDateTime disconnectedAt) {
		var permissions = googlePermissionRepository.findByUser(user);
		permissions.forEach(permission -> permission.disconnect(disconnectedAt));
		return !permissions.isEmpty();
	}

	private void syncPermission(User user, ServiceType serviceType, boolean isConnected, String scopeText,
		LocalDateTime checkedAt) {
		GooglePermission permission = googlePermissionRepository.findByUserAndServiceType(user, serviceType)
			.orElseGet(() -> GooglePermission.create(user, serviceType));
		if (isConnected) {
			permission.connect(scopeText, checkedAt);
		} else {
			permission.deny(scopeText, checkedAt);
		}
		googlePermissionRepository.save(permission);
	}

	private PermissionStatus syncRecheckedPermission(User user, ServiceType serviceType, boolean isConnected,
		String scopeText, LocalDateTime checkedAt) {
		GooglePermission permission = googlePermissionRepository.findByUserAndServiceType(user, serviceType)
			.orElseGet(() -> GooglePermission.create(user, serviceType));
		if (isConnected) {
			permission.connect(scopeText, checkedAt);
		} else {
			permission.requireReconnect(scopeText, checkedAt);
		}
		googlePermissionRepository.save(permission);
		return permission.getPermissionStatus();
	}

	private boolean hasScope(String scopeText, String keyword) {
		return scopeText != null && scopeText.toLowerCase().contains(keyword);
	}

	private void validateRedirectUri(String redirectUri) {
		try {
			URI.create(redirectUri);
		} catch (IllegalArgumentException exception) {
			throw new CustomException(ErrorCode.INVALID_REDIRECT_URI);
		}
		if (!configuredRedirectUri.isBlank() && !configuredRedirectUri.equals(redirectUri)) {
			throw new CustomException(ErrorCode.INVALID_REDIRECT_URI);
		}
	}

	private List<String> createScopes(List<ServiceType> serviceTypes) {
		List<String> scopes = new ArrayList<>();
		scopes.add(OPENID_SCOPE);
		scopes.add(EMAIL_SCOPE);
		scopes.add(PROFILE_SCOPE);
		if (serviceTypes.contains(ServiceType.GMAIL)) scopes.add(GMAIL_SCOPE);
		if (serviceTypes.contains(ServiceType.DRIVE)) scopes.add(DRIVE_SCOPE);
		return scopes;
	}
}

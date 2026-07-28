package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.GooglePermissionRecheckResponse;
import com.AURA.AURA_Service.auth.dto.GooglePermissionResponse;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GooglePermissionService {
	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;

	public GooglePermissionService(UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		GooglePermissionRepository googlePermissionRepository, TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient) {
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
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
}

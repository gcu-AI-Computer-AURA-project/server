package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.AURA.AURA_Service.auth.dto.GoogleLoginRequest;
import com.AURA.AURA_Service.auth.dto.GoogleLoginResponse;
import com.AURA.AURA_Service.auth.repository.NotificationSettingRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.ScanSettingRepository;
import com.AURA.AURA_Service.auth.repository.UserConsentRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleToken;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleUser;
import com.AURA.AURA_Service.auth.service.JwtTokenService.TokenPair;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
	private final GoogleOAuthClient googleOAuthClient;
	private final TokenEncryptionService tokenEncryptionService;
	private final JwtTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final UserConsentRepository userConsentRepository;
	private final NotificationSettingRepository notificationSettingRepository;
	private final ScanSettingRepository scanSettingRepository;
	private final String configuredRedirectUri;

	public AuthService(GoogleOAuthClient googleOAuthClient, TokenEncryptionService tokenEncryptionService,
		JwtTokenService jwtTokenService, UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		UserConsentRepository userConsentRepository, NotificationSettingRepository notificationSettingRepository,
		ScanSettingRepository scanSettingRepository, @Value("${aura.google.redirect-uri:}") String configuredRedirectUri) {
		this.googleOAuthClient = googleOAuthClient;
		this.tokenEncryptionService = tokenEncryptionService;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.userConsentRepository = userConsentRepository;
		this.notificationSettingRepository = notificationSettingRepository;
		this.scanSettingRepository = scanSettingRepository;
		this.configuredRedirectUri = configuredRedirectUri;
	}

	@Transactional
	public GoogleLoginResponse login(GoogleLoginRequest request) {
		validateRedirectUri(request.redirectUri());
		GoogleToken googleToken = googleOAuthClient.exchangeCode(request.authorizationCode(), request.redirectUri());
		GoogleUser googleUser = googleOAuthClient.getUserInfo(googleToken.accessToken());

		User user = userRepository.findByGoogleProviderId(googleUser.sub()).orElse(null);
		boolean isNewUser = user == null;
		if (isNewUser) user = userRepository.save(User.create(googleUser.sub(), googleUser.email(), googleUser.name(), googleUser.picture()));
		else user.updateGoogleProfile(googleUser.sub(), googleUser.email(), googleUser.name(), googleUser.picture());
		User activeUser = user;

		OAuthToken oauthToken = oauthTokenRepository.findByUser(activeUser).orElseGet(() -> OAuthToken.create(activeUser));
		String encryptedRefreshToken = googleToken.refreshToken() == null ? null : tokenEncryptionService.encrypt(googleToken.refreshToken());
		oauthToken.update(encryptedRefreshToken, googleToken.expiresIn(), googleToken.scope());
		oauthTokenRepository.save(oauthToken);

		UserConsent consent = userConsentRepository.findByUser(activeUser).orElseGet(() -> userConsentRepository.save(new UserConsent(activeUser)));
		if (isNewUser) notificationSettingRepository.save(new NotificationSetting(activeUser));
		boolean isInitialScanSetupRequired = !scanSettingRepository.existsByUserUserId(activeUser.getUserId());
		GoogleLoginResponse.NextStep nextStep = determineNextStep(consent, isInitialScanSetupRequired);
		TokenPair auraTokens = jwtTokenService.issue(activeUser);

		return new GoogleLoginResponse(auraTokens.accessToken(), auraTokens.refreshToken(), "Bearer", 3600,
			isNewUser, nextStep, isInitialScanSetupRequired,
			new GoogleLoginResponse.UserResponse(activeUser.getUserId(), activeUser.getEmail(), activeUser.getDisplayName(), activeUser.getProfileImageUrl()));
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

	private GoogleLoginResponse.NextStep determineNextStep(UserConsent consent, boolean isInitialScanSetupRequired) {
		if (!consent.isRequiredConsentCompleted()) return GoogleLoginResponse.NextStep.CONSENT_REQUIRED;
		if (isInitialScanSetupRequired) return GoogleLoginResponse.NextStep.SCAN_SETUP_REQUIRED;
		return GoogleLoginResponse.NextStep.COMPLETED;
	}
}

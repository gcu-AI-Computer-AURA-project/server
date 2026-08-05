package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.User.AccountStatus;
import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.AURA.AURA_Service.auth.domain.UserWithdrawal;
import com.AURA.AURA_Service.auth.dto.UserMeResponse;
import com.AURA.AURA_Service.auth.dto.UserPrivacyDataResponse;
import com.AURA.AURA_Service.auth.dto.UserWithdrawalRequest;
import com.AURA.AURA_Service.auth.dto.UserWithdrawalResponse;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserConsentRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.repository.UserWithdrawalRepository;
import com.AURA.AURA_Service.cleanup.repository.CleanupHistoryRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import com.AURA.AURA_Service.scan.repository.ScannedItemRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final UserConsentRepository userConsentRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final UserWithdrawalRepository userWithdrawalRepository;
	private final GooglePermissionService googlePermissionService;
	private final ScanJobRepository scanJobRepository;
	private final ScannedItemRepository scannedItemRepository;
	private final CleanupHistoryRepository cleanupHistoryRepository;

	public UserService(UserRepository userRepository, UserConsentRepository userConsentRepository,
		OAuthTokenRepository oauthTokenRepository, UserWithdrawalRepository userWithdrawalRepository,
		GooglePermissionService googlePermissionService, ScanJobRepository scanJobRepository,
		ScannedItemRepository scannedItemRepository, CleanupHistoryRepository cleanupHistoryRepository) {
		this.userRepository = userRepository;
		this.userConsentRepository = userConsentRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.userWithdrawalRepository = userWithdrawalRepository;
		this.googlePermissionService = googlePermissionService;
		this.scanJobRepository = scanJobRepository;
		this.scannedItemRepository = scannedItemRepository;
		this.cleanupHistoryRepository = cleanupHistoryRepository;
	}

	@Transactional(readOnly = true)
	public UserMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return UserMeResponse.from(user);
	}

	@Transactional(readOnly = true)
	public UserPrivacyDataResponse getPrivacyData(Long userId) {
		User user = findUser(userId);
		UserConsent consent = userConsentRepository.findByUser(user)
			.orElseGet(() -> new UserConsent(user));
		long scanJobCount = scanJobRepository.countByUserAndDeletedAtIsNull(user);
		long scannedItemCount = scannedItemRepository.countByUserAndDeletedAtIsNull(user);
		long cleanupHistoryCount = cleanupHistoryRepository.countByUser(user);
		return UserPrivacyDataResponse.from(consent, scanJobCount, scannedItemCount, cleanupHistoryCount);
	}

	@Transactional
	public UserWithdrawalResponse withdraw(Long userId, UserWithdrawalRequest request) {
		if (!Boolean.TRUE.equals(request.withdrawalConfirmed())) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		User user = findUser(userId);
		validateWithdrawable(user);
		LocalDateTime now = LocalDateTime.now();
		UserWithdrawal withdrawal = userWithdrawalRepository.save(UserWithdrawal.request(
			user,
			request.reason(),
			request.scanDataPolicy(),
			request.historyDataPolicy(),
			now
		));
		boolean tokenDeleted = revokeToken(user, now);
		boolean googleDisconnected = googlePermissionService.disconnectAll(user, now);
		user.withdraw(createAnonymousUserKey(), now);
		withdrawal.complete(googleDisconnected || tokenDeleted, tokenDeleted, now);
		return UserWithdrawalResponse.from(withdrawal);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateWithdrawable(User user) {
		if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
			throw new CustomException(ErrorCode.USER_ALREADY_WITHDRAWN);
		}
	}

	private boolean revokeToken(User user, LocalDateTime revokedAt) {
		return oauthTokenRepository.findByUser(user)
			.map(token -> revokeToken(token, revokedAt))
			.orElse(false);
	}

	private boolean revokeToken(OAuthToken token, LocalDateTime revokedAt) {
		token.revoke(revokedAt);
		return true;
	}

	private String createAnonymousUserKey() {
		return UUID.randomUUID().toString().replace("-", "");
	}
}

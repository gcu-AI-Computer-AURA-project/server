package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.AURA.AURA_Service.auth.dto.UserMeResponse;
import com.AURA.AURA_Service.auth.dto.UserPrivacyDataResponse;
import com.AURA.AURA_Service.auth.repository.UserConsentRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final UserConsentRepository userConsentRepository;

	public UserService(UserRepository userRepository, UserConsentRepository userConsentRepository) {
		this.userRepository = userRepository;
		this.userConsentRepository = userConsentRepository;
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
		return UserPrivacyDataResponse.from(consent);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}

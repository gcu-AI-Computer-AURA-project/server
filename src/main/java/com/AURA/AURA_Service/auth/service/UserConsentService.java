package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.AURA.AURA_Service.auth.dto.UserConsentRequest;
import com.AURA.AURA_Service.auth.dto.UserConsentResponse;
import com.AURA.AURA_Service.auth.dto.UserConsentSaveResponse;
import com.AURA.AURA_Service.auth.repository.UserConsentRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserConsentService {
	private final UserRepository userRepository;
	private final UserConsentRepository userConsentRepository;

	public UserConsentService(UserRepository userRepository, UserConsentRepository userConsentRepository) {
		this.userRepository = userRepository;
		this.userConsentRepository = userConsentRepository;
	}

	@Transactional(readOnly = true)
	public UserConsentResponse get(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		UserConsent consent = userConsentRepository.findByUser(user)
			.orElseGet(() -> new UserConsent(user));
		return UserConsentResponse.from(consent);
	}

	@Transactional
	public UserConsentSaveResponse save(Long userId, UserConsentRequest request) {
		if (!request.isAllRequiredAgreed()) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		UserConsent consent = userConsentRepository.findByUser(user)
			.orElseGet(() -> new UserConsent(user));
		consent.agree(request.isPrivacyAgreed(), request.isAiAnalysisAgreed(), request.isMetadataOnlyAgreed(),
			request.isUserApprovalRequiredAgreed(), request.consentVersion(), LocalDateTime.now());
		return UserConsentSaveResponse.from(userConsentRepository.save(consent));
	}
}

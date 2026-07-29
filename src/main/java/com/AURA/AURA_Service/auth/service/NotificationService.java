package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.FcmToken;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.FcmTokenRequest;
import com.AURA.AURA_Service.auth.dto.FcmTokenResponse;
import com.AURA.AURA_Service.auth.repository.FcmTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
	private final UserRepository userRepository;
	private final FcmTokenRepository fcmTokenRepository;

	public NotificationService(UserRepository userRepository, FcmTokenRepository fcmTokenRepository) {
		this.userRepository = userRepository;
		this.fcmTokenRepository = fcmTokenRepository;
	}

	@Transactional
	public FcmTokenResponse saveFcmToken(Long userId, FcmTokenRequest request) {
		User user = findUser(userId);
		FcmToken fcmToken = fcmTokenRepository.findByFcmToken(request.fcmToken())
			.orElseGet(() -> new FcmToken(user, request.fcmToken(), request.deviceIdentifier()));
		fcmToken.refresh(user, request.deviceIdentifier());
		return FcmTokenResponse.from(fcmTokenRepository.save(fcmToken));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}

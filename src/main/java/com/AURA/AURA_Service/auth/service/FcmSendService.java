package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.FcmToken;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.FcmTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FcmSendService {
	private static final Logger LOGGER = LoggerFactory.getLogger(FcmSendService.class);

	private final UserRepository userRepository;
	private final FcmTokenRepository fcmTokenRepository;

	public FcmSendService(UserRepository userRepository, FcmTokenRepository fcmTokenRepository) {
		this.userRepository = userRepository;
		this.fcmTokenRepository = fcmTokenRepository;
	}

	@Transactional
	public List<String> sendToUser(Long userId, String title, String message, Map<String, String> data) {
		User user = findUser(userId);
		List<FcmToken> fcmTokens = fcmTokenRepository.findByUserAndIsActiveTrue(user);
		List<String> messageIds = new ArrayList<>();
		for (FcmToken fcmToken : fcmTokens) {
			try {
				messageIds.add(sendAndMarkUsed(fcmToken, title, message, data));
			} catch (CustomException exception) {
				LOGGER.warn("FCM token send failed. fcmTokenId={}", fcmToken.getFcmTokenId(), exception);
			}
		}
		return messageIds;
	}

	public String sendToToken(String fcmToken, String title, String message, Map<String, String> data) {
		return sendMessage(fcmToken, title, message, data);
	}

	private String sendAndMarkUsed(FcmToken fcmToken, String title, String message, Map<String, String> data) {
		String messageId = sendMessage(fcmToken.getFcmToken(), title, message, data);
		fcmToken.markUsedAt(LocalDateTime.now());
		return messageId;
	}

	private String sendMessage(String fcmToken, String title, String message, Map<String, String> data) {
		if (FirebaseApp.getApps().isEmpty()) {
			throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		}
		Message.Builder messageBuilder = Message.builder()
			.setToken(fcmToken)
			.setNotification(Notification.builder()
				.setTitle(title)
				.setBody(message)
				.build());
		if (data != null && !data.isEmpty()) {
			messageBuilder.putAllData(data);
		}
		try {
			return FirebaseMessaging.getInstance().send(messageBuilder.build());
		} catch (FirebaseMessagingException exception) {
			LOGGER.warn("FCM 알림 발송에 실패했습니다. firebase_error_code={}", exception.getErrorCode());
			throw new CustomException(ErrorCode.FCM_SEND_FAILED);
		}
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}

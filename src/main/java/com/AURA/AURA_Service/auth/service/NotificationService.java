package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.FcmToken;
import com.AURA.AURA_Service.auth.domain.Notification;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.FcmTokenRequest;
import com.AURA.AURA_Service.auth.dto.FcmTokenResponse;
import com.AURA.AURA_Service.auth.dto.NotificationListItemResponse;
import com.AURA.AURA_Service.auth.dto.NotificationPageResponse;
import com.AURA.AURA_Service.auth.repository.FcmTokenRepository;
import com.AURA.AURA_Service.auth.repository.NotificationRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
	private static final int MAX_PAGE_SIZE = 100;

	private final UserRepository userRepository;
	private final FcmTokenRepository fcmTokenRepository;
	private final NotificationRepository notificationRepository;

	public NotificationService(UserRepository userRepository,
		FcmTokenRepository fcmTokenRepository,
		NotificationRepository notificationRepository) {
		this.userRepository = userRepository;
		this.fcmTokenRepository = fcmTokenRepository;
		this.notificationRepository = notificationRepository;
	}

	@Transactional
	public FcmTokenResponse saveFcmToken(Long userId, FcmTokenRequest request) {
		User user = findUser(userId);
		FcmToken fcmToken = fcmTokenRepository.findByFcmToken(request.fcmToken())
			.orElseGet(() -> new FcmToken(user, request.fcmToken(), request.deviceIdentifier()));
		fcmToken.refresh(user, request.deviceIdentifier());
		return FcmTokenResponse.from(fcmTokenRepository.save(fcmToken));
	}

	@Transactional(readOnly = true)
	public NotificationPageResponse getList(Long userId, int page, int size, boolean unreadOnly) {
		User user = findUser(userId);
		Pageable pageable = createPageable(page, size);
		Page<Notification> notifications = unreadOnly
			? notificationRepository.findByUserAndIsReadFalse(user, pageable)
			: notificationRepository.findByUser(user, pageable);
		List<NotificationListItemResponse> content = notifications.getContent().stream()
			.map(NotificationListItemResponse::from)
			.toList();
		return NotificationPageResponse.from(notifications, content);
	}

	private Pageable createPageable(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return PageRequest.of(page, size, Sort.by(Sort.Order.desc("sentAt")));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}

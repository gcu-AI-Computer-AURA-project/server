package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.NotificationSettingRequest;
import com.AURA.AURA_Service.auth.dto.NotificationSettingResponse;
import com.AURA.AURA_Service.auth.repository.NotificationSettingRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationSettingService {
	private final UserRepository userRepository;
	private final NotificationSettingRepository notificationSettingRepository;

	public NotificationSettingService(UserRepository userRepository,
		NotificationSettingRepository notificationSettingRepository) {
		this.userRepository = userRepository;
		this.notificationSettingRepository = notificationSettingRepository;
	}

	@Transactional(readOnly = true)
	public NotificationSettingResponse get(Long userId) {
		User user = findUser(userId);
		NotificationSetting notificationSetting = notificationSettingRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_SETTING_NOT_FOUND));
		return NotificationSettingResponse.from(notificationSetting);
	}

	@Transactional
	public NotificationSettingResponse save(Long userId, NotificationSettingRequest request) {
		User user = findUser(userId);
		NotificationSetting notificationSetting = notificationSettingRepository.findByUser(user)
			.orElseGet(() -> new NotificationSetting(user));
		notificationSetting.update(request.isScanCompleteEnabled(), request.isScanRecommendEnabled());
		return NotificationSettingResponse.from(notificationSettingRepository.save(notificationSetting));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}
}

package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.UserMeResponse;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		return UserMeResponse.from(user);
	}
}

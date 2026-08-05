package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.dto.ScanSettingRequest;
import com.AURA.AURA_Service.auth.dto.ScanSettingResponse;
import com.AURA.AURA_Service.auth.repository.ScanSettingRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScanSettingService {
	private final UserRepository userRepository;
	private final ScanSettingRepository scanSettingRepository;
	private final ScanKeywordValidator scanKeywordValidator;

	public ScanSettingService(UserRepository userRepository, ScanSettingRepository scanSettingRepository,
		ScanKeywordValidator scanKeywordValidator) {
		this.userRepository = userRepository;
		this.scanSettingRepository = scanSettingRepository;
		this.scanKeywordValidator = scanKeywordValidator;
	}

	@Transactional(readOnly = true)
	public ScanSettingResponse get(Long userId) {
		User user = findUser(userId);
		ScanSetting scanSetting = scanSettingRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(ErrorCode.SCAN_SETTING_NOT_FOUND));
		return ScanSettingResponse.from(scanSetting);
	}

	@Transactional
	public ScanSettingResponse save(Long userId, ScanSettingRequest request) {
		validateDriveFolder(request);
		scanKeywordValidator.validateNoConflict(request.includeKeywords(), request.excludeKeywords());
		User user = findUser(userId);
		ScanSetting scanSetting = scanSettingRepository.findByUser(user).orElseGet(() -> new ScanSetting(user));
		scanSetting.update(request.scanSource(), normalizeDriveFolderId(request), request.includeSubfolders(),
			request.lastOpenedBeforeMonths(), request.lastModifiedBeforeMonths(), request.createdBeforeMonths(),
			request.excludeRecentDays(), request.includeKeywords(), request.excludeKeywords(), request.fileExtensions(),
			request.includeMailAttachmentSize(), request.applyRecentConditions());
		return ScanSettingResponse.from(scanSettingRepository.save(scanSetting));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private void validateDriveFolder(ScanSettingRequest request) {
		if (request.scanSource() == ScanSource.DRIVE_FOLDER && isBlank(request.driveFolderId())) {
			throw new CustomException(ErrorCode.DRIVE_FOLDER_REQUIRED);
		}
	}

	private String normalizeDriveFolderId(ScanSettingRequest request) {
		if (request.scanSource() != ScanSource.DRIVE_FOLDER) return null;
		return request.driveFolderId().trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record ScanSettingResponse(
	@JsonProperty("setting_id")
	Long settingId,
	@JsonProperty("scan_source")
	ScanSource scanSource,
	@JsonProperty("drive_folder_id")
	String driveFolderId,
	@JsonProperty("include_subfolders")
	boolean includeSubfolders,
	@JsonProperty("last_opened_before_months")
	Integer lastOpenedBeforeMonths,
	@JsonProperty("last_modified_before_months")
	Integer lastModifiedBeforeMonths,
	@JsonProperty("created_before_months")
	Integer createdBeforeMonths,
	@JsonProperty("exclude_recent_days")
	Integer excludeRecentDays,
	@JsonProperty("include_keywords")
	List<String> includeKeywords,
	@JsonProperty("exclude_keywords")
	List<String> excludeKeywords,
	@JsonProperty("file_extensions")
	List<String> fileExtensions,
	@JsonProperty("include_mail_attachment_size")
	boolean includeMailAttachmentSize,
	@JsonProperty("apply_recent_conditions")
	boolean applyRecentConditions,
	@JsonProperty("updated_at")
	LocalDateTime updatedAt
) {
	public static ScanSettingResponse from(ScanSetting scanSetting) {
		return new ScanSettingResponse(
			scanSetting.getSettingId(),
			scanSetting.getScanSource(),
			scanSetting.getDriveFolderId(),
			scanSetting.isIncludeSubfolders(),
			scanSetting.getLastOpenedBeforeMonths(),
			scanSetting.getLastModifiedBeforeMonths(),
			scanSetting.getCreatedBeforeMonths(),
			scanSetting.getExcludeRecentDays(),
			scanSetting.getIncludeKeywords(),
			scanSetting.getExcludeKeywords(),
			scanSetting.getFileExtensions(),
			scanSetting.isIncludeMailAttachmentSize(),
			scanSetting.isApplyRecentConditions(),
			scanSetting.getUpdatedAt()
		);
	}
}

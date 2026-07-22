package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ScanSettingRequest(
	@NotNull(message = "스캔 범위는 필수 입력값입니다.")
	@JsonProperty("scan_source")
	ScanSource scanSource,

	@JsonProperty("drive_folder_id")
	String driveFolderId,

	@NotNull(message = "하위 폴더 포함 여부는 필수 입력값입니다.")
	@JsonProperty("include_subfolders")
	Boolean includeSubfolders,

	@Min(value = 1, message = "마지막 열람 기준 개월 수는 1 이상이어야 합니다.")
	@JsonProperty("last_opened_before_months")
	Integer lastOpenedBeforeMonths,

	@Min(value = 1, message = "마지막 수정 기준 개월 수는 1 이상이어야 합니다.")
	@JsonProperty("last_modified_before_months")
	Integer lastModifiedBeforeMonths,

	@Min(value = 1, message = "파일 저장일 기준 개월 수는 1 이상이어야 합니다.")
	@JsonProperty("created_before_months")
	Integer createdBeforeMonths,

	@NotNull(message = "최근 사용 항목 제외 기준 일수는 필수 입력값입니다.")
	@Min(value = 0, message = "최근 사용 항목 제외 기준 일수는 0 이상이어야 합니다.")
	@JsonProperty("exclude_recent_days")
	Integer excludeRecentDays,

	@JsonProperty("include_keywords")
	List<String> includeKeywords,

	@JsonProperty("exclude_keywords")
	List<String> excludeKeywords,

	@JsonProperty("file_extensions")
	List<String> fileExtensions,

	@NotNull(message = "메일 첨부파일 용량 포함 여부는 필수 입력값입니다.")
	@JsonProperty("include_mail_attachment_size")
	Boolean includeMailAttachmentSize,

	@NotNull(message = "최근 조건 자동 적용 여부는 필수 입력값입니다.")
	@JsonProperty("apply_recent_conditions")
	Boolean applyRecentConditions
) {
}

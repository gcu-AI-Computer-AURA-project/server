package com.AURA.AURA_Service.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ScanCreateRequest(
	@NotNull(message = "저장된 기본 조건 사용 여부는 필수 입력값입니다.")
	@JsonProperty("use_saved_settings")
	Boolean useSavedSettings,

	@Valid
	@JsonProperty("settings_override")
	ScanSettingsOverrideRequest settingsOverride
) {
}

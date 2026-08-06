package com.AURA.AURA_Service.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record StorageTrashEmptyRequest(
	@NotNull(message = "휴지통 비우기 대상 범위는 필수 입력값입니다.")
	@JsonProperty("target_source")
	TargetSource targetSource,

	@NotNull(message = "휴지통 비우기 승인 여부는 필수 입력값입니다.")
	@JsonProperty("approval_confirmed")
	Boolean approvalConfirmed,

	@NotNull(message = "확인 문구는 필수 입력값입니다.")
	@JsonProperty("confirmation_text")
	String confirmationText
) {
	public enum TargetSource {
		GMAIL,
		DRIVE,
		ALL
	}
}

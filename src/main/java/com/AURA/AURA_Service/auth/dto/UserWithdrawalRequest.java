package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.UserWithdrawal.DataPolicy;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserWithdrawalRequest(
	@Size(max = 500, message = "탈퇴 사유는 500자 이하로 입력해야 합니다.")
	String reason,

	@NotNull(message = "스캔 데이터 처리 정책은 필수 입력값입니다.")
	@JsonProperty("scan_data_policy")
	DataPolicy scanDataPolicy,

	@NotNull(message = "이력 데이터 처리 정책은 필수 입력값입니다.")
	@JsonProperty("history_data_policy")
	DataPolicy historyDataPolicy,

	@NotNull(message = "탈퇴 최종 확인 여부는 필수 입력값입니다.")
	@JsonProperty("withdrawal_confirmed")
	Boolean withdrawalConfirmed
) {
}

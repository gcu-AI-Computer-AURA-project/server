package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record CandidateSelectionRequest(
	@NotNull(message = "선택 상태는 필수 입력값입니다.")
	@JsonProperty("selection_status")
	SelectionStatus selectionStatus,

	@NotNull(message = "선택 상태 버전은 필수 입력값입니다.")
	@JsonProperty("selection_version")
	Integer selectionVersion
) {
}

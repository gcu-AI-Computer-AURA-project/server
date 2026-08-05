package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CleanupJobCreateRequest(
	@NotNull(message = "스캔 작업 ID는 필수 입력값입니다.")
	@JsonProperty("scan_job_id")
	Long scanJobId,

	@NotNull(message = "정리 작업 유형은 필수 입력값입니다.")
	@JsonProperty("action_type")
	ActionType actionType,

	@Valid
	@NotEmpty(message = "정리 후보 목록은 필수 입력값입니다.")
	List<CandidateRequest> candidates,

	@NotNull(message = "정리 승인 여부는 필수 입력값입니다.")
	@JsonProperty("approval_confirmed")
	Boolean approvalConfirmed
) {
	public record CandidateRequest(
		@NotNull(message = "후보 ID는 필수 입력값입니다.")
		@JsonProperty("candidate_id")
		Long candidateId,

		@NotNull(message = "선택 상태 버전은 필수 입력값입니다.")
		@JsonProperty("selection_version")
		Integer selectionVersion
	) {
	}
}

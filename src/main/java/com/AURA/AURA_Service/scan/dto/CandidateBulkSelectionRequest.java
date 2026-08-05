package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CandidateBulkSelectionRequest(
	CandidateCategory category,

	@JsonProperty("item_source")
	ItemSource itemSource,

	@JsonProperty("candidate_ids")
	List<Long> candidateIds,

	@NotNull(message = "선택 상태는 필수 입력값입니다.")
	@JsonProperty("selection_status")
	SelectionStatus selectionStatus,

	@NotNull(message = "보호 대상 제외 여부는 필수 입력값입니다.")
	@JsonProperty("exclude_protected")
	Boolean excludeProtected
) {
}

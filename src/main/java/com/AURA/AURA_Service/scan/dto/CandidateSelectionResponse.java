package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record CandidateSelectionResponse(
	@JsonProperty("candidate_id")
	Long candidateId,
	@JsonProperty("selection_status")
	SelectionStatus selectionStatus,
	@JsonProperty("selection_version")
	Integer selectionVersion,
	@JsonProperty("updated_at")
	LocalDateTime updatedAt
) {
	public static CandidateSelectionResponse from(AnalysisCandidate candidate) {
		return new CandidateSelectionResponse(
			candidate.getCandidateId(),
			candidate.getSelectionStatus(),
			candidate.getSelectionVersion(),
			candidate.getUpdatedAt()
		);
	}
}

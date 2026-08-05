package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.repository.CandidateSelectionSummary;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CandidateBulkSelectionResponse(
	@JsonProperty("updated_count")
	int updatedCount,
	@JsonProperty("selected_count")
	Long selectedCount,
	@JsonProperty("selected_estimated_reclaim_bytes")
	Long selectedEstimatedReclaimBytes
) {
	public static CandidateBulkSelectionResponse from(int updatedCount, CandidateSelectionSummary summary) {
		return new CandidateBulkSelectionResponse(
			updatedCount,
			summary.getSelectedCount(),
			summary.getSelectedEstimatedReclaimBytes()
		);
	}
}

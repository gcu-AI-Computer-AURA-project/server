package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.repository.AnalysisCategorySummary;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AnalysisCategorySummaryResponse(
	CandidateCategory category,
	@JsonProperty("display_name")
	String displayName,
	@JsonProperty("item_count")
	Long itemCount,
	@JsonProperty("estimated_reclaim_bytes")
	Long estimatedReclaimBytes,
	@JsonProperty("selected_count")
	Long selectedCount
) {
	public static AnalysisCategorySummaryResponse from(AnalysisCategorySummary summary) {
		return new AnalysisCategorySummaryResponse(
			summary.getCategory(),
			summary.getCategory().getDisplayName(),
			summary.getItemCount(),
			summary.getEstimatedReclaimBytes(),
			summary.getSelectedCount()
		);
	}
}

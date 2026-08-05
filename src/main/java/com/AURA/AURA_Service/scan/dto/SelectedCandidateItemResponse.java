package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.RiskLevel;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SelectedCandidateItemResponse(
	@JsonProperty("candidate_id")
	Long candidateId,
	@JsonProperty("item_source")
	ItemSource itemSource,
	String title,
	@JsonProperty("size_bytes")
	Long sizeBytes,
	CandidateCategory category,
	@JsonProperty("risk_level")
	RiskLevel riskLevel,
	@JsonProperty("selection_version")
	Integer selectionVersion
) {
	public static SelectedCandidateItemResponse from(AnalysisCandidate candidate) {
		ScannedItem item = candidate.getScannedItem();
		return new SelectedCandidateItemResponse(
			candidate.getCandidateId(),
			item.getItemSource(),
			item.getTitle(),
			item.getSizeBytes(),
			candidate.getCategory(),
			candidate.getRiskLevel(),
			candidate.getSelectionVersion()
		);
	}
}

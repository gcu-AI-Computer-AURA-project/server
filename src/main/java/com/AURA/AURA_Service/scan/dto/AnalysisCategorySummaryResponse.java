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
			displayName(summary.getCategory()),
			summary.getItemCount(),
			summary.getEstimatedReclaimBytes(),
			summary.getSelectedCount()
		);
	}

	private static String displayName(CandidateCategory category) {
		return switch (category) {
			case PROMOTION_MAIL -> "광고·프로모션 메일";
			case OLD_MAIL -> "오래된 메일";
			case DUPLICATE_FILE -> "중복 파일";
			case OLD_DRIVE_FILE -> "오래된 Drive 파일";
			case LARGE_FILE -> "대용량 파일";
			case LOW_VALUE_ATTACHMENT -> "낮은 가치의 첨부파일";
			case TEMP_OR_BACKUP -> "임시·백업 파일";
			case PROTECTED -> "보호 대상";
		};
	}
}

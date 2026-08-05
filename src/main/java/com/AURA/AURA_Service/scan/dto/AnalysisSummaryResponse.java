package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AnalysisSummaryResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("total_candidate_count")
	Integer totalCandidateCount,
	@JsonProperty("total_estimated_reclaim_bytes")
	Long totalEstimatedReclaimBytes,
	@JsonProperty("protected_count")
	Integer protectedCount,
	List<AnalysisCategorySummaryResponse> categories
) {
	public static AnalysisSummaryResponse from(ScanJob scanJob, List<AnalysisCategorySummaryResponse> categories) {
		return new AnalysisSummaryResponse(
			scanJob.getScanJobId(),
			scanJob.getCandidateCount(),
			scanJob.getEstimatedReclaimBytes(),
			scanJob.getProtectedCount(),
			categories
		);
	}
}

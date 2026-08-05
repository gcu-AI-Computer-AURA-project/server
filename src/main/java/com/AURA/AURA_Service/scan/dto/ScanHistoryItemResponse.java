package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScanHistoryItemResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("job_status")
	JobStatus jobStatus,
	@JsonProperty("scan_source")
	ScanSource scanSource,
	@JsonProperty("candidate_count")
	Integer candidateCount,
	@JsonProperty("estimated_reclaim_bytes")
	Long estimatedReclaimBytes,
	@JsonProperty("cleanup_done")
	boolean cleanupDone,
	@JsonProperty("reclaimed_bytes")
	Long reclaimedBytes,
	@JsonProperty("estimated_carbon_grams")
	BigDecimal estimatedCarbonGrams,
	@JsonProperty("created_at")
	LocalDateTime createdAt
) {
	public static ScanHistoryItemResponse from(ScanJob scanJob) {
		return new ScanHistoryItemResponse(
			scanJob.getScanJobId(),
			scanJob.getJobStatus(),
			scanJob.getScanSource(),
			scanJob.getCandidateCount(),
			scanJob.getEstimatedReclaimBytes(),
			false,
			0L,
			new BigDecimal("0.0000"),
			scanJob.getCreatedAt()
		);
	}
}

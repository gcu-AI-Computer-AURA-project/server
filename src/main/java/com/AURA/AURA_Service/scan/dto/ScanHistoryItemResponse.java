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
	private static final BigDecimal ZERO_CARBON_GRAMS = new BigDecimal("0.0000");

	public static ScanHistoryItemResponse from(ScanJob scanJob) {
		return from(scanJob, false, 0L, ZERO_CARBON_GRAMS);
	}

	public static ScanHistoryItemResponse from(ScanJob scanJob, boolean cleanupDone, Long reclaimedBytes,
		BigDecimal estimatedCarbonGrams) {
		return new ScanHistoryItemResponse(
			scanJob.getScanJobId(),
			scanJob.getJobStatus(),
			scanJob.getScanSource(),
			scanJob.getCandidateCount(),
			scanJob.getEstimatedReclaimBytes(),
			cleanupDone,
			defaultZero(reclaimedBytes),
			defaultCarbon(estimatedCarbonGrams),
			scanJob.getCreatedAt()
		);
	}

	private static Long defaultZero(Long value) {
		return value == null ? 0L : value;
	}

	private static BigDecimal defaultCarbon(BigDecimal value) {
		return value == null ? ZERO_CARBON_GRAMS : value;
	}
}

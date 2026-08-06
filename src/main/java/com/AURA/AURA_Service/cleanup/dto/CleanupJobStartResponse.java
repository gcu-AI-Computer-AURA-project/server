package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CleanupJobStartResponse(
	@JsonProperty("cleanup_job_id")
	Long cleanupJobId,

	@JsonProperty("job_status")
	JobStatus jobStatus,

	@JsonProperty("progress_percent")
	BigDecimal progressPercent,

	@JsonProperty("approved_at")
	LocalDateTime approvedAt
) {
	public static CleanupJobStartResponse from(CleanupJob cleanupJob) {
		return new CleanupJobStartResponse(
			cleanupJob.getCleanupJobId(),
			cleanupJob.getJobStatus(),
			cleanupJob.getProgressPercent(),
			cleanupJob.getApprovedAt()
		);
	}
}

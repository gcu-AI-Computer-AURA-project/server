package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CleanupJobRetryFailedResponse(
	@JsonProperty("cleanup_job_id")
	Long cleanupJobId,

	@JsonProperty("retry_item_count")
	Integer retryItemCount,

	@JsonProperty("job_status")
	JobStatus jobStatus
) {
	public static CleanupJobRetryFailedResponse from(CleanupJob cleanupJob, int retryItemCount) {
		return new CleanupJobRetryFailedResponse(
			cleanupJob.getCleanupJobId(),
			retryItemCount,
			cleanupJob.getJobStatus()
		);
	}
}

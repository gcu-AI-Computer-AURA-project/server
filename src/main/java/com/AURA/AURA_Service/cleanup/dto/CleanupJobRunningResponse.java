package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CleanupJobRunningResponse(
	@JsonProperty("cleanup_job")
	CleanupJobDetailResponse cleanupJob
) {
	public static CleanupJobRunningResponse empty() {
		return new CleanupJobRunningResponse(null);
	}

	public static CleanupJobRunningResponse from(CleanupJob cleanupJob) {
		return new CleanupJobRunningResponse(CleanupJobDetailResponse.from(cleanupJob));
	}
}

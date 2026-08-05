package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record CleanupJobCreateResponse(
	@JsonProperty("cleanup_job_id")
	Long cleanupJobId,

	@JsonProperty("job_status")
	JobStatus jobStatus,

	@JsonProperty("action_type")
	ActionType actionType,

	@JsonProperty("selected_mail_count")
	Integer selectedMailCount,

	@JsonProperty("selected_drive_count")
	Integer selectedDriveCount,

	@JsonProperty("total_selected_bytes")
	Long totalSelectedBytes,

	@JsonProperty("approved_at")
	LocalDateTime approvedAt
) {
	public static CleanupJobCreateResponse from(CleanupJob cleanupJob) {
		return new CleanupJobCreateResponse(
			cleanupJob.getCleanupJobId(),
			cleanupJob.getJobStatus(),
			cleanupJob.getActionType(),
			cleanupJob.getSelectedMailCount(),
			cleanupJob.getSelectedDriveCount(),
			cleanupJob.getTotalSelectedBytes(),
			cleanupJob.getApprovedAt()
		);
	}
}

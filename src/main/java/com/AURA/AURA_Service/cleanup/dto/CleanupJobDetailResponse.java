package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CleanupJobDetailResponse(
	@JsonProperty("cleanup_job_id")
	Long cleanupJobId,

	@JsonProperty("scan_job_id")
	Long scanJobId,

	@JsonProperty("action_type")
	ActionType actionType,

	@JsonProperty("job_status")
	JobStatus jobStatus,

	@JsonProperty("selected_mail_count")
	Integer selectedMailCount,

	@JsonProperty("selected_drive_count")
	Integer selectedDriveCount,

	@JsonProperty("total_selected_bytes")
	Long totalSelectedBytes,

	@JsonProperty("success_item_count")
	Integer successItemCount,

	@JsonProperty("failed_item_count")
	Integer failedItemCount,

	@JsonProperty("progress_percent")
	BigDecimal progressPercent,

	@JsonProperty("error_message")
	String errorMessage,

	@JsonProperty("approved_at")
	LocalDateTime approvedAt,

	@JsonProperty("completed_at")
	LocalDateTime completedAt
) {
	public static CleanupJobDetailResponse from(CleanupJob cleanupJob) {
		return new CleanupJobDetailResponse(
			cleanupJob.getCleanupJobId(),
			cleanupJob.getScanJobId(),
			cleanupJob.getActionType(),
			cleanupJob.getJobStatus(),
			cleanupJob.getSelectedMailCount(),
			cleanupJob.getSelectedDriveCount(),
			cleanupJob.getTotalSelectedBytes(),
			cleanupJob.getSuccessItemCount(),
			cleanupJob.getFailedItemCount(),
			cleanupJob.getProgressPercent(),
			cleanupJob.getErrorMessage(),
			cleanupJob.getApprovedAt(),
			cleanupJob.getCompletedAt()
		);
	}
}

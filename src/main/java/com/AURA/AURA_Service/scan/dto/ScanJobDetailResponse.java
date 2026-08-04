package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record ScanJobDetailResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("job_status")
	JobStatus jobStatus,
	@JsonProperty("scan_source")
	ScanSource scanSource,
	@JsonProperty("condition_snapshot")
	Map<String, Object> conditionSnapshot,
	@JsonProperty("progress_percent")
	BigDecimal progressPercent,
	@JsonProperty("mail_scanned_count")
	Integer mailScannedCount,
	@JsonProperty("drive_scanned_count")
	Integer driveScannedCount,
	@JsonProperty("candidate_count")
	Integer candidateCount,
	@JsonProperty("protected_count")
	Integer protectedCount,
	@JsonProperty("estimated_reclaim_bytes")
	Long estimatedReclaimBytes,
	@JsonProperty("error_message")
	String errorMessage,
	@JsonProperty("started_at")
	LocalDateTime startedAt,
	@JsonProperty("completed_at")
	LocalDateTime completedAt
) {
	public static ScanJobDetailResponse from(ScanJob scanJob) {
		return new ScanJobDetailResponse(
			scanJob.getScanJobId(),
			scanJob.getJobStatus(),
			scanJob.getScanSource(),
			scanJob.getConditionSnapshot(),
			scanJob.getProgressPercent(),
			scanJob.getMailScannedCount(),
			scanJob.getDriveScannedCount(),
			scanJob.getCandidateCount(),
			scanJob.getProtectedCount(),
			scanJob.getEstimatedReclaimBytes(),
			scanJob.getErrorMessage(),
			scanJob.getStartedAt(),
			scanJob.getCompletedAt()
		);
	}
}

package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScanRunningJobResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("job_status")
	JobStatus jobStatus,
	@JsonProperty("scan_source")
	ScanSource scanSource,
	@JsonProperty("progress_percent")
	BigDecimal progressPercent,
	@JsonProperty("mail_scanned_count")
	Integer mailScannedCount,
	@JsonProperty("drive_scanned_count")
	Integer driveScannedCount,
	@JsonProperty("candidate_count")
	Integer candidateCount,
	@JsonProperty("estimated_remaining_seconds")
	Long estimatedRemainingSeconds,
	@JsonProperty("started_at")
	LocalDateTime startedAt
) {
	public static ScanRunningJobResponse from(ScanJob scanJob, Long estimatedRemainingSeconds) {
		return new ScanRunningJobResponse(
			scanJob.getScanJobId(),
			scanJob.getJobStatus(),
			scanJob.getScanSource(),
			scanJob.getProgressPercent(),
			scanJob.getMailScannedCount(),
			scanJob.getDriveScannedCount(),
			scanJob.getCandidateCount(),
			estimatedRemainingSeconds,
			scanJob.getStartedAt()
		);
	}
}

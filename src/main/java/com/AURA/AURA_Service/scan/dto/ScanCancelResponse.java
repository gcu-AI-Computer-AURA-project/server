package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record ScanCancelResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("job_status")
	JobStatus jobStatus,
	@JsonProperty("canceled_at")
	LocalDateTime canceledAt
) {
	public static ScanCancelResponse from(ScanJob scanJob) {
		return new ScanCancelResponse(
			scanJob.getScanJobId(),
			scanJob.getJobStatus(),
			scanJob.getCanceledAt()
		);
	}
}

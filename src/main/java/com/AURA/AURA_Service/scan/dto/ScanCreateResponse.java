package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ScanCreateResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("job_status")
	JobStatus jobStatus,
	@JsonProperty("scan_source")
	ScanSource scanSource,
	@JsonProperty("progress_percent")
	BigDecimal progressPercent,
	@JsonProperty("created_at")
	LocalDateTime createdAt
) {
	public static ScanCreateResponse from(ScanJob scanJob) {
		return new ScanCreateResponse(
			scanJob.getScanJobId(),
			scanJob.getJobStatus(),
			scanJob.getScanSource(),
			scanJob.getProgressPercent(),
			scanJob.getCreatedAt()
		);
	}
}

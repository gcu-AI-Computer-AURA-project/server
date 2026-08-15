package com.AURA.AURA_Service.home.dto;

import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HomeSummaryResponse(
	@JsonProperty("storage_summary")
	StorageSummaryResponse storageSummary,
	@JsonProperty("latest_scan")
	LatestScanResponse latestScan,
	@JsonProperty("latest_cleanup")
	LatestCleanupResponse latestCleanup,
	PermissionSummaryResponse permissions,
	@JsonProperty("has_running_scan")
	boolean hasRunningScan
) {
	public record StorageSummaryResponse(
		@JsonProperty("estimated_reclaim_bytes")
		Long estimatedReclaimBytes,
		@JsonProperty("latest_remaining_drive_bytes")
		Long latestRemainingDriveBytes,
		@JsonProperty("total_drive_bytes")
		Long totalDriveBytes,
		@JsonProperty("total_reclaimed_bytes")
		Long totalReclaimedBytes,
		@JsonProperty("total_estimated_carbon_grams")
		BigDecimal totalEstimatedCarbonGrams
	) {
	}

	public record LatestScanResponse(
		@JsonProperty("scan_job_id")
		Long scanJobId,
		@JsonProperty("job_status")
		JobStatus jobStatus,
		@JsonProperty("scan_source")
		ScanSource scanSource,
		@JsonProperty("candidate_count")
		Integer candidateCount,
		@JsonProperty("protected_count")
		Integer protectedCount,
		@JsonProperty("estimated_reclaim_bytes")
		Long estimatedReclaimBytes,
		@JsonProperty("started_at")
		LocalDateTime startedAt,
		@JsonProperty("completed_at")
		LocalDateTime completedAt
	) {
		public static LatestScanResponse from(ScanJob scanJob) {
			if (scanJob == null) {
				return null;
			}
			return new LatestScanResponse(
				scanJob.getScanJobId(),
				scanJob.getJobStatus(),
				scanJob.getScanSource(),
				scanJob.getCandidateCount(),
				scanJob.getProtectedCount(),
				scanJob.getEstimatedReclaimBytes(),
				scanJob.getStartedAt(),
				scanJob.getCompletedAt()
			);
		}
	}

	public record LatestCleanupResponse(
		@JsonProperty("cleanup_job_id")
		Long cleanupJobId,
		@JsonProperty("cleaned_item_count")
		Integer cleanedItemCount,
		@JsonProperty("reclaimed_bytes")
		Long reclaimedBytes,
		@JsonProperty("estimated_carbon_grams")
		BigDecimal estimatedCarbonGrams,
		@JsonProperty("completed_at")
		LocalDateTime completedAt
	) {
	}

	public record PermissionSummaryResponse(
		@JsonProperty("gmail_status")
		PermissionStatus gmailStatus,
		@JsonProperty("drive_status")
		PermissionStatus driveStatus
	) {
	}
}

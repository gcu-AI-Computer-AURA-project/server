package com.AURA.AURA_Service.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatisticsSummaryResponse(
	@JsonProperty("total_scan_count")
	long totalScanCount,
	@JsonProperty("total_cleanup_count")
	long totalCleanupCount,
	@JsonProperty("total_trashed_item_count")
	long totalTrashedItemCount,
	@JsonProperty("total_permanently_deleted_item_count")
	long totalPermanentlyDeletedItemCount,
	@JsonProperty("total_reclaimed_bytes")
	long totalReclaimedBytes,
	@JsonProperty("total_estimated_carbon_grams")
	BigDecimal totalEstimatedCarbonGrams,
	@JsonProperty("latest_cleanup_at")
	LocalDateTime latestCleanupAt
) {
}

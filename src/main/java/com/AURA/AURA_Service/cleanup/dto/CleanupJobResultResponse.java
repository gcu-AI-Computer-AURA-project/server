package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CarbonSavingHistory;
import com.AURA.AURA_Service.cleanup.domain.CleanupHistory;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CleanupJobResultResponse(
	@JsonProperty("cleanup_job_id")
	Long cleanupJobId,

	@JsonProperty("history_id")
	Long historyId,

	@JsonProperty("cleaned_item_count")
	Integer cleanedItemCount,

	@JsonProperty("reclaimed_bytes")
	Long reclaimedBytes,

	@JsonProperty("remaining_drive_bytes")
	Long remainingDriveBytes,

	@JsonProperty("estimated_carbon_grams")
	BigDecimal estimatedCarbonGrams,

	@JsonProperty("formula_version")
	String formulaVersion,

	@JsonProperty("completed_at")
	LocalDateTime completedAt
) {
	private static final BigDecimal ZERO_CARBON_GRAMS = new BigDecimal("0.0000");
	private static final String DEFAULT_FORMULA_VERSION = "v1";

	public static CleanupJobResultResponse from(CleanupHistory cleanupHistory,
		CarbonSavingHistory carbonSavingHistory) {
		return new CleanupJobResultResponse(
			cleanupHistory.getCleanupJobId(),
			cleanupHistory.getHistoryId(),
			cleanupHistory.getCleanedItemCount(),
			cleanupHistory.getReclaimedBytes(),
			cleanupHistory.getRemainingDriveBytes(),
			resolveCarbonGrams(carbonSavingHistory),
			resolveFormulaVersion(carbonSavingHistory),
			cleanupHistory.getCompletedAt()
		);
	}

	private static BigDecimal resolveCarbonGrams(CarbonSavingHistory carbonSavingHistory) {
		if (carbonSavingHistory == null || carbonSavingHistory.getEstimatedCarbonGrams() == null) {
			return ZERO_CARBON_GRAMS;
		}
		return carbonSavingHistory.getEstimatedCarbonGrams();
	}

	private static String resolveFormulaVersion(CarbonSavingHistory carbonSavingHistory) {
		if (carbonSavingHistory == null || carbonSavingHistory.getFormulaVersion() == null) {
			return DEFAULT_FORMULA_VERSION;
		}
		return carbonSavingHistory.getFormulaVersion();
	}
}

package com.AURA.AURA_Service.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record StatisticsMonthlyResponse(
	List<MonthlyStatisticResponse> months
) {
	public record MonthlyStatisticResponse(
		@JsonProperty("stat_year_month")
		String statYearMonth,
		@JsonProperty("scan_count")
		long scanCount,
		@JsonProperty("cleanup_count")
		long cleanupCount,
		@JsonProperty("trashed_item_count")
		long trashedItemCount,
		@JsonProperty("permanently_deleted_item_count")
		long permanentlyDeletedItemCount,
		@JsonProperty("reclaimed_bytes")
		long reclaimedBytes,
		@JsonProperty("estimated_carbon_grams")
		BigDecimal estimatedCarbonGrams
	) {
	}
}

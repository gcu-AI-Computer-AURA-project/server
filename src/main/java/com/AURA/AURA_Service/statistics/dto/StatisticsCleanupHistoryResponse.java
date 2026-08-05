package com.AURA.AURA_Service.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StatisticsCleanupHistoryResponse(
	List<CleanupHistoryItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements")
	long totalElements,
	@JsonProperty("total_pages")
	int totalPages
) {
	public record CleanupHistoryItemResponse(
		@JsonProperty("history_id")
		Long historyId,
		@JsonProperty("cleanup_job_id")
		Long cleanupJobId,
		@JsonProperty("scan_job_id")
		Long scanJobId,
		@JsonProperty("action_type")
		String actionType,
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

	public static StatisticsCleanupHistoryResponse empty(int page, int size) {
		return new StatisticsCleanupHistoryResponse(List.of(), page, size, 0L, 0);
	}

	public static StatisticsCleanupHistoryResponse from(List<CleanupHistoryItemResponse> content, int page, int size,
		long totalElements) {
		int totalPages = totalElements == 0 ? 0 : (int)Math.ceil((double)totalElements / size);
		return new StatisticsCleanupHistoryResponse(content, page, size, totalElements, totalPages);
	}
}

package com.AURA.AURA_Service.cleanup.dto;

import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem.ProcessStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record CleanupJobItemListResponse(
	List<CleanupJobItemResponse> items
) {
	public static CleanupJobItemListResponse from(List<CleanupJobItem> cleanupJobItems) {
		return new CleanupJobItemListResponse(cleanupJobItems.stream()
			.map(CleanupJobItemResponse::from)
			.toList());
	}

	public record CleanupJobItemResponse(
		@JsonProperty("cleanup_item_id")
		Long cleanupItemId,

		@JsonProperty("item_source")
		ItemSource itemSource,

		@JsonProperty("external_item_id")
		String externalItemId,

		@JsonProperty("snapshot_item_key")
		String snapshotItemKey,

		@JsonProperty("snapshot_title")
		String snapshotTitle,

		@JsonProperty("snapshot_size_bytes")
		Long snapshotSizeBytes,

		@JsonProperty("process_status")
		ProcessStatus processStatus,

		@JsonProperty("failure_reason")
		String failureReason,

		@JsonProperty("processed_at")
		LocalDateTime processedAt
	) {
		public static CleanupJobItemResponse from(CleanupJobItem cleanupJobItem) {
			return new CleanupJobItemResponse(
				cleanupJobItem.getCleanupItemId(),
				cleanupJobItem.getItemSource(),
				cleanupJobItem.getExternalItemId(),
				cleanupJobItem.getSnapshotItemKey(),
				cleanupJobItem.getSnapshotTitle(),
				cleanupJobItem.getSnapshotSizeBytes(),
				cleanupJobItem.getProcessStatus(),
				cleanupJobItem.getFailureReason(),
				cleanupJobItem.getProcessedAt()
			);
		}
	}
}

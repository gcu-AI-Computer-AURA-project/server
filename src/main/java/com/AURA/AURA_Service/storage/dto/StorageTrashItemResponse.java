package com.AURA.AURA_Service.storage.dto;

import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record StorageTrashItemResponse(
	@JsonProperty("item_id") Long itemId,
	@JsonProperty("item_source") ItemSource itemSource,
	@JsonProperty("external_item_id") String externalItemId,
	String title,
	@JsonProperty("size_bytes") Long sizeBytes,
	@JsonProperty("trashed_at") LocalDateTime trashedAt,
	boolean recoverable
) {
	public static StorageTrashItemResponse from(ScannedItem item) {
		return new StorageTrashItemResponse(
			item.getItemId(),
			item.getItemSource(),
			item.getExternalItemId(),
			item.getTitle(),
			item.getSizeBytes(),
			item.getTrashedAt(),
			item.getExternalItemId() != null && !item.getExternalItemId().isBlank()
		);
	}
}

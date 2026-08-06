package com.AURA.AURA_Service.storage.dto;

import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record StorageItemListItemResponse(
	@JsonProperty("item_id") Long itemId,
	@JsonProperty("item_source") ItemSource itemSource,
	@JsonProperty("external_item_id") String externalItemId,
	String title,
	@JsonProperty("size_bytes") Long sizeBytes,
	@JsonProperty("mime_type") String mimeType,
	@JsonProperty("file_extension") String fileExtension,
	@JsonProperty("modified_time") LocalDateTime modifiedTime,
	@JsonProperty("last_opened_time") LocalDateTime lastOpenedTime,
	@JsonProperty("is_shared") boolean isShared,
	@JsonProperty("is_trashed") boolean isTrashed,
	@JsonProperty("trashed_at") LocalDateTime trashedAt
) {
	public static StorageItemListItemResponse from(ScannedItem item) {
		return new StorageItemListItemResponse(
			item.getItemId(),
			item.getItemSource(),
			item.getExternalItemId(),
			item.getTitle(),
			item.getSizeBytes(),
			item.getMimeType(),
			item.getFileExtension(),
			item.getModifiedTime(),
			item.getLastOpenedTime(),
			item.isShared(),
			item.isTrashed(),
			item.getTrashedAt()
		);
	}

	public static StorageItemListItemResponse live(Long itemId, ItemSource itemSource, String externalItemId,
		String title, Long sizeBytes, String mimeType, String fileExtension, LocalDateTime modifiedTime,
		LocalDateTime lastOpenedTime, boolean isShared, boolean isTrashed, LocalDateTime trashedAt) {
		return new StorageItemListItemResponse(
			itemId,
			itemSource,
			externalItemId,
			title,
			sizeBytes,
			mimeType,
			fileExtension,
			modifiedTime,
			lastOpenedTime,
			isShared,
			isTrashed,
			trashedAt
		);
	}
}

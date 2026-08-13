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
	@JsonProperty("mime_type") String mimeType,
	@JsonProperty("file_extension") String fileExtension,
	@JsonProperty("item_type") String itemType,
	@JsonProperty("is_folder") boolean isFolder,
	@JsonProperty("parent_folder_id") String parentFolderId,
	@JsonProperty("owner_email") String ownerEmail,
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
			item.getMimeType(),
			item.getFileExtension(),
			"FILE",
			false,
			null,
			null,
			item.getTrashedAt(),
			item.getExternalItemId() != null && !item.getExternalItemId().isBlank()
		);
	}

	public static StorageTrashItemResponse live(Long itemId, ItemSource itemSource, String externalItemId,
		String title, Long sizeBytes, LocalDateTime trashedAt) {
		return live(itemId, itemSource, externalItemId, title, sizeBytes, null, null, "FILE", false, null, null, trashedAt);
	}

	public static StorageTrashItemResponse live(StorageItemListItemResponse item) {
		return live(item.itemId(), item.itemSource(), item.externalItemId(), item.title(), item.sizeBytes(),
			item.mimeType(), item.fileExtension(), item.itemType(), item.isFolder(), item.parentFolderId(),
			item.ownerEmail(), item.trashedAt());
	}

	public static StorageTrashItemResponse live(Long itemId, ItemSource itemSource, String externalItemId,
		String title, Long sizeBytes, String mimeType, String fileExtension, String itemType, boolean isFolder,
		String parentFolderId, String ownerEmail, LocalDateTime trashedAt) {
		return new StorageTrashItemResponse(
			itemId,
			itemSource,
			externalItemId,
			title,
			sizeBytes,
			mimeType,
			fileExtension,
			itemType,
			isFolder,
			parentFolderId,
			ownerEmail,
			trashedAt,
			externalItemId != null && !externalItemId.isBlank()
		);
	}
}

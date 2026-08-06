package com.AURA.AURA_Service.storage.dto;

import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record StorageItemDetailResponse(
	@JsonProperty("item_id") Long itemId,
	@JsonProperty("scan_job_id") Long scanJobId,
	@JsonProperty("item_source") ItemSource itemSource,
	@JsonProperty("external_item_id") String externalItemId,
	String title,
	@JsonProperty("mime_type") String mimeType,
	@JsonProperty("file_extension") String fileExtension,
	@JsonProperty("folder_path") String folderPath,
	@JsonProperty("size_bytes") Long sizeBytes,
	@JsonProperty("created_time") LocalDateTime createdTime,
	@JsonProperty("modified_time") LocalDateTime modifiedTime,
	@JsonProperty("last_opened_time") LocalDateTime lastOpenedTime,
	@JsonProperty("is_shared") boolean isShared,
	@JsonProperty("owner_email") String ownerEmail,
	@JsonProperty("is_trashed") boolean isTrashed,
	@JsonProperty("live_metadata_refreshed") boolean liveMetadataRefreshed,
	@JsonProperty("created_at") LocalDateTime createdAt
) {
	public static StorageItemDetailResponse fromSnapshot(ScannedItem item) {
		return new StorageItemDetailResponse(
			item.getItemId(),
			item.getScanJobId(),
			item.getItemSource(),
			item.getExternalItemId(),
			item.getTitle(),
			item.getMimeType(),
			item.getFileExtension(),
			item.getFolderPath(),
			item.getSizeBytes(),
			item.getCreatedTime(),
			item.getModifiedTime(),
			item.getLastOpenedTime(),
			item.isShared(),
			item.getOwnerEmail(),
			item.isTrashed(),
			false,
			item.getCreatedAt()
		);
	}

	public static StorageItemDetailResponse fromLiveMetadata(ScannedItem item, StorageItemLiveDetailResponse liveMetadata) {
		return new StorageItemDetailResponse(
			item.getItemId(),
			item.getScanJobId(),
			item.getItemSource(),
			item.getExternalItemId(),
			liveMetadata.title(),
			liveMetadata.mimeType(),
			liveMetadata.fileExtension(),
			liveMetadata.folderPath(),
			liveMetadata.sizeBytes(),
			liveMetadata.createdTime(),
			liveMetadata.modifiedTime(),
			liveMetadata.lastOpenedTime(),
			liveMetadata.isShared(),
			liveMetadata.ownerEmail(),
			liveMetadata.isTrashed(),
			liveMetadata.liveMetadataRefreshed(),
			item.getCreatedAt()
		);
	}
}

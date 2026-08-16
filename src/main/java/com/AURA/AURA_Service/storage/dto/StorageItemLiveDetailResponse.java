package com.AURA.AURA_Service.storage.dto;

import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record StorageItemLiveDetailResponse(
	@JsonProperty("item_id") Long itemId,
	@JsonProperty("item_source") ItemSource itemSource,
	@JsonProperty("external_item_id") String externalItemId,
	String title,
	String snippet,
	@JsonProperty("body_text") String bodyText,
	@JsonProperty("mime_type") String mimeType,
	@JsonProperty("file_extension") String fileExtension,
	@JsonProperty("folder_path") String folderPath,
	@JsonProperty("web_view_link") String webViewLink,
	@JsonProperty("size_bytes") Long sizeBytes,
	@JsonProperty("created_time") LocalDateTime createdTime,
	@JsonProperty("modified_time") LocalDateTime modifiedTime,
	@JsonProperty("last_opened_time") LocalDateTime lastOpenedTime,
	@JsonProperty("is_shared") boolean isShared,
	@JsonProperty("owner_email") String ownerEmail,
	@JsonProperty("is_trashed") boolean isTrashed,
	@JsonProperty("live_metadata_refreshed") boolean liveMetadataRefreshed
) { }

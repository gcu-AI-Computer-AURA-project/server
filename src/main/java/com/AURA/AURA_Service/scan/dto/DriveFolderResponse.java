package com.AURA.AURA_Service.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record DriveFolderResponse(
	List<DriveFolderItem> folders,
	@JsonProperty("next_page_token")
	String nextPageToken
) {
	public record DriveFolderItem(
		@JsonProperty("folder_id")
		String folderId,
		String name,
		@JsonProperty("parent_id")
		String parentId,
		@JsonProperty("modified_time")
		LocalDateTime modifiedTime
	) {
	}
}

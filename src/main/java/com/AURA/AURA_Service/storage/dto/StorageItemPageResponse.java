package com.AURA.AURA_Service.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record StorageItemPageResponse(
	List<StorageItemListItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements") long totalElements,
	@JsonProperty("total_pages") int totalPages
) {
	public static StorageItemPageResponse from(Page<?> page, List<StorageItemListItemResponse> content) {
		return new StorageItemPageResponse(
			content,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}

package com.AURA.AURA_Service.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record StorageTrashPageResponse(
	List<StorageTrashItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements") long totalElements,
	@JsonProperty("total_pages") int totalPages
) {
	public static StorageTrashPageResponse from(Page<?> page, List<StorageTrashItemResponse> content) {
		return new StorageTrashPageResponse(
			content,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}

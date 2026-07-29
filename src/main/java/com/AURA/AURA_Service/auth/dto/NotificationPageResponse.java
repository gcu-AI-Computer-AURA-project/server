package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record NotificationPageResponse(
	List<NotificationListItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements")
	long totalElements,
	@JsonProperty("total_pages")
	int totalPages
) {
	public static NotificationPageResponse from(Page<?> page, List<NotificationListItemResponse> content) {
		return new NotificationPageResponse(
			content,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}

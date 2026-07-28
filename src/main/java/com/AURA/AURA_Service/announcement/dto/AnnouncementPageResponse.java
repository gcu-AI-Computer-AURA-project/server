package com.AURA.AURA_Service.announcement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record AnnouncementPageResponse(
	List<AnnouncementListItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements")
	long totalElements,
	@JsonProperty("total_pages")
	int totalPages
) {
	public static AnnouncementPageResponse from(Page<?> page, List<AnnouncementListItemResponse> content) {
		return new AnnouncementPageResponse(
			content,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}

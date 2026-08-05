package com.AURA.AURA_Service.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record ScanHistoryResponse(
	List<ScanHistoryItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements")
	long totalElements,
	@JsonProperty("total_pages")
	int totalPages
) {
	public static ScanHistoryResponse from(Page<?> page, List<ScanHistoryItemResponse> content) {
		return new ScanHistoryResponse(
			content,
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages()
		);
	}
}

package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.data.domain.Page;

public record AnalysisCandidatePageResponse(
	List<AnalysisCandidateListItemResponse> content,
	int page,
	int size,
	@JsonProperty("total_elements")
	long totalElements,
	@JsonProperty("total_pages")
	int totalPages
) {
	public static AnalysisCandidatePageResponse from(Page<AnalysisCandidate> candidates) {
		List<AnalysisCandidateListItemResponse> content = candidates.getContent().stream()
			.map(AnalysisCandidateListItemResponse::from)
			.toList();
		return new AnalysisCandidatePageResponse(
			content,
			candidates.getNumber(),
			candidates.getSize(),
			candidates.getTotalElements(),
			candidates.getTotalPages()
		);
	}
}

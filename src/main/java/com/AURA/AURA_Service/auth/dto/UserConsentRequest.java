package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserConsentRequest(
	@NotNull @JsonProperty("is_privacy_agreed") Boolean isPrivacyAgreed,
	@NotNull @JsonProperty("is_ai_analysis_agreed") Boolean isAiAnalysisAgreed,
	@NotNull @JsonProperty("is_metadata_only_agreed") Boolean isMetadataOnlyAgreed,
	@NotNull @JsonProperty("is_user_approval_required_agreed") Boolean isUserApprovalRequiredAgreed,
	@NotBlank @JsonProperty("consent_version") String consentVersion
) {
	public boolean isAllRequiredAgreed() {
		return Boolean.TRUE.equals(isPrivacyAgreed)
			&& Boolean.TRUE.equals(isAiAnalysisAgreed)
			&& Boolean.TRUE.equals(isMetadataOnlyAgreed)
			&& Boolean.TRUE.equals(isUserApprovalRequiredAgreed);
	}
}

package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record UserConsentResponse(
	@JsonProperty("is_privacy_agreed") boolean isPrivacyAgreed,
	@JsonProperty("is_ai_analysis_agreed") boolean isAiAnalysisAgreed,
	@JsonProperty("is_metadata_only_agreed") boolean isMetadataOnlyAgreed,
	@JsonProperty("is_user_approval_required_agreed") boolean isUserApprovalRequiredAgreed,
	@JsonProperty("consent_version") String consentVersion,
	@JsonProperty("consented_at") LocalDateTime consentedAt
) {
	public static UserConsentResponse from(UserConsent consent) {
		return new UserConsentResponse(
			consent.isPrivacyAgreed(),
			consent.isAiAnalysisAgreed(),
			consent.isMetadataOnlyAgreed(),
			consent.isUserApprovalRequiredAgreed(),
			consent.getConsentVersion(),
			consent.getConsentedAt()
		);
	}
}

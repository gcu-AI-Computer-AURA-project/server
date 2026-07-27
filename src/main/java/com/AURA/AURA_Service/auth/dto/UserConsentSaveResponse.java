package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record UserConsentSaveResponse(
	@JsonProperty("consent_id") Long consentId,
	@JsonProperty("consented_at") LocalDateTime consentedAt
) {
	public static UserConsentSaveResponse from(UserConsent consent) {
		return new UserConsentSaveResponse(consent.getConsentId(), consent.getConsentedAt());
	}
}

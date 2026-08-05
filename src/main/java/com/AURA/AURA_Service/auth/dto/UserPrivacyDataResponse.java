package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.UserConsent;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record UserPrivacyDataResponse(
	ConsentsResponse consents,
	@JsonProperty("data_retention") DataRetentionResponse dataRetention,
	@JsonProperty("managed_data_summary") ManagedDataSummaryResponse managedDataSummary
) {
	public static UserPrivacyDataResponse from(UserConsent consent, long scanJobCount, long scannedItemCount,
		long cleanupHistoryCount) {
		return new UserPrivacyDataResponse(
			ConsentsResponse.from(consent),
			DataRetentionResponse.defaultPolicy(),
			new ManagedDataSummaryResponse(scanJobCount, scannedItemCount, cleanupHistoryCount)
		);
	}

	public record ConsentsResponse(
		@JsonProperty("is_privacy_agreed") boolean isPrivacyAgreed,
		@JsonProperty("is_ai_analysis_agreed") boolean isAiAnalysisAgreed,
		@JsonProperty("is_metadata_only_agreed") boolean isMetadataOnlyAgreed,
		@JsonProperty("is_user_approval_required_agreed") boolean isUserApprovalRequiredAgreed,
		@JsonProperty("consent_version") String consentVersion,
		@JsonProperty("consented_at") LocalDateTime consentedAt
	) {
		public static ConsentsResponse from(UserConsent consent) {
			return new ConsentsResponse(
				consent.isPrivacyAgreed(),
				consent.isAiAnalysisAgreed(),
				consent.isMetadataOnlyAgreed(),
				consent.isUserApprovalRequiredAgreed(),
				consent.getConsentVersion(),
				consent.getConsentedAt()
			);
		}
	}

	public record DataRetentionResponse(
		@JsonProperty("scan_data_policy") String scanDataPolicy,
		@JsonProperty("history_data_policy") String historyDataPolicy
	) {
		public static DataRetentionResponse defaultPolicy() {
			return new DataRetentionResponse(
				"ANONYMIZE",
				"ANONYMIZE"
			);
		}
	}

	public record ManagedDataSummaryResponse(
		@JsonProperty("scan_job_count") long scanJobCount,
		@JsonProperty("scanned_item_count") long scannedItemCount,
		@JsonProperty("cleanup_history_count") long cleanupHistoryCount
	) {
	}
}

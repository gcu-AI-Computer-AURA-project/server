package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_consents")
public class UserConsent {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "consent_id") private Long consentId;
	@OneToOne @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
	@Column(name = "is_privacy_agreed", nullable = false) private boolean isPrivacyAgreed;
	@Column(name = "is_ai_analysis_agreed", nullable = false) private boolean isAiAnalysisAgreed;
	@Column(name = "is_metadata_only_agreed", nullable = false) private boolean isMetadataOnlyAgreed;
	@Column(name = "is_user_approval_required_agreed", nullable = false) private boolean isUserApprovalRequiredAgreed;
	@Column(name = "consent_version", nullable = false) private String consentVersion = "v1";

	protected UserConsent() { }
	public UserConsent(User user) { this.user = user; }
	public boolean isRequiredConsentCompleted() {
		return isPrivacyAgreed && isAiAnalysisAgreed && isMetadataOnlyAgreed && isUserApprovalRequiredAgreed;
	}
}

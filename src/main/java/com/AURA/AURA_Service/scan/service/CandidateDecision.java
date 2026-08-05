package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.RiskLevel;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CandidateDecision(
	Long itemId,
	CandidateCategory category,
	RiskLevel riskLevel,
	BigDecimal priorityScore,
	BigDecimal ghostScore,
	boolean isProtected,
	SelectionStatus selectionStatus,
	long estimatedReclaimBytes,
	BigDecimal aiConfidenceScore,
	List<String> semanticTags,
	Map<String, Object> matchedConditions
) {
}

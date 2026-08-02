package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import java.math.BigDecimal;
import java.util.List;

public record GeminiAnalysisResult(
	String clientItemKey,
	CandidateCategory suggestedCategory,
	boolean cleanupHint,
	boolean protectedHint,
	BigDecimal confidenceScore,
	List<String> semanticTags,
	List<KeywordMatch> includeKeywordMatches,
	List<KeywordMatch> excludeKeywordMatches
) {
	public static GeminiAnalysisResult empty(String clientItemKey) {
		return new GeminiAnalysisResult(clientItemKey, null, false, false, BigDecimal.ZERO, List.of(), List.of(), List.of());
	}

	public record KeywordMatch(String keyword, String matchType, BigDecimal confidenceScore) {
	}
}

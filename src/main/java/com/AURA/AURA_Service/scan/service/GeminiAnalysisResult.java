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
	List<KeywordMatch> excludeKeywordMatches,
	ResponseStatus responseStatus
) {
	public static GeminiAnalysisResult empty(String clientItemKey) {
		return new GeminiAnalysisResult(clientItemKey, null, false, false, null, List.of(), List.of(), List.of(),
			ResponseStatus.MISSING);
	}

	public static GeminiAnalysisResult ruleBased(String clientItemKey) {
		return new GeminiAnalysisResult(clientItemKey, null, false, false, null, List.of(), List.of(), List.of(),
			ResponseStatus.RULE_BASED);
	}

	public static GeminiAnalysisResult fallback(String clientItemKey) {
		return new GeminiAnalysisResult(clientItemKey, null, false, false, null, List.of(), List.of(), List.of(),
			ResponseStatus.FALLBACK);
	}

	public record KeywordMatch(String keyword, String matchType, BigDecimal confidenceScore) {
	}

	public enum ResponseStatus {
		SUCCESS,
		MISSING,
		RULE_BASED,
		FALLBACK
	}
}

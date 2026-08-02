package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.ScannedItem;
import java.util.Map;

public record GeminiAnalysisBundle(
	Map<String, GeminiAnalysisResult> results,
	String aiProvider,
	String aiModelName,
	boolean fallback,
	String errorMessage
) {
	public static GeminiAnalysisBundle success(Map<String, GeminiAnalysisResult> results, String aiProvider, String aiModelName) {
		return new GeminiAnalysisBundle(results, aiProvider, aiModelName, false, null);
	}

	public static GeminiAnalysisBundle fallback(Map<String, GeminiAnalysisResult> results, String errorMessage) {
		return new GeminiAnalysisBundle(results, "FALLBACK", null, true, errorMessage);
	}

	public static GeminiAnalysisBundle ruleBased() {
		return new GeminiAnalysisBundle(Map.of(), "RULE_BASED", null, false, null);
	}

	public GeminiAnalysisResult resultFor(ScannedItem item) {
		return results.getOrDefault(item.getClientItemKey(), GeminiAnalysisResult.empty(item.getClientItemKey()));
	}
}

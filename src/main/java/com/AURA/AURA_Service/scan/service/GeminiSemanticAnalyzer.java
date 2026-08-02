package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.ScannedItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiSemanticAnalyzer {
	private final GeminiApiClient geminiApiClient;
	private final String provider;
	private final boolean analysisEnabled;
	private final int batchSize;

	public GeminiSemanticAnalyzer(GeminiApiClient geminiApiClient, @Value("${aura.ai.provider}") String provider,
		@Value("${aura.ai.analysis-enabled}") boolean analysisEnabled, @Value("${aura.ai.batch-size}") int batchSize) {
		this.geminiApiClient = geminiApiClient;
		this.provider = provider;
		this.analysisEnabled = analysisEnabled;
		this.batchSize = Math.max(1, batchSize);
	}

	public boolean isRequiredButNotConfigured() {
		return analysisEnabled && isGeminiProvider() && !geminiApiClient.isConfigured();
	}

	public GeminiAnalysisBundle analyze(List<ScannedItem> items, ScanCondition condition) {
		if (items.isEmpty()) return GeminiAnalysisBundle.success(Map.of(), provider, geminiApiClient.getModel());
		if (!analysisEnabled || !isGeminiProvider()) return GeminiAnalysisBundle.ruleBased();

		Map<String, GeminiAnalysisResult> results = new LinkedHashMap<>();
		try {
			for (int start = 0; start < items.size(); start += batchSize) {
				int end = Math.min(start + batchSize, items.size());
				results.putAll(geminiApiClient.analyzeBatch(items.subList(start, end), condition));
			}
			return GeminiAnalysisBundle.success(results, provider, geminiApiClient.getModel());
		} catch (RuntimeException exception) {
			return GeminiAnalysisBundle.fallback(results, exception.getMessage());
		}
	}

	private boolean isGeminiProvider() {
		return "GEMINI_API".equalsIgnoreCase(provider);
	}
}

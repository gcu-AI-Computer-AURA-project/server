package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ScanKeywordValidator {
	public void validateNoConflict(List<String> includeKeywords, List<String> excludeKeywords) {
		Set<String> normalizedIncludeKeywords = normalize(includeKeywords);
		if (normalizedIncludeKeywords.isEmpty()) return;
		boolean hasConflict = normalize(excludeKeywords).stream().anyMatch(normalizedIncludeKeywords::contains);
		if (hasConflict) {
			throw new CustomException(ErrorCode.SCAN_KEYWORD_CONFLICT);
		}
	}

	private Set<String> normalize(List<String> keywords) {
		Set<String> normalizedKeywords = new HashSet<>();
		if (keywords == null) return normalizedKeywords;
		for (String keyword : keywords) {
			if (keyword == null || keyword.isBlank()) continue;
			normalizedKeywords.add(keyword.trim().toLowerCase(Locale.ROOT));
		}
		return normalizedKeywords;
	}
}

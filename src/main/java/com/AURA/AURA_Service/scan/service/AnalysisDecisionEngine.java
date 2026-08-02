package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.RiskLevel;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AnalysisDecisionEngine {
	private static final BigDecimal ZERO_SCORE = new BigDecimal("0.00");
	private static final BigDecimal GENERAL_SCORE = new BigDecimal("50.00");
	private static final BigDecimal PERIOD_SCORE = new BigDecimal("60.00");
	private static final BigDecimal INCLUDE_SCORE = new BigDecimal("80.00");
	private static final BigDecimal DUPLICATE_SCORE = new BigDecimal("95.00");
	private static final BigDecimal SEMANTIC_THRESHOLD = new BigDecimal("60.00");
	private static final BigDecimal LOW_RISK_PROMOTION_CONFIDENCE = new BigDecimal("90.00");
	private static final long LARGE_FILE_BYTES = 500L * 1024L * 1024L;
	private static final Pattern COPY_SUFFIX_PATTERN = Pattern.compile("\\s*(\\(\\d+\\)|\\[\\d+\\]|-\\s*copy|copy\\s*\\d*)$", Pattern.CASE_INSENSITIVE);

	public List<CandidateDecision> decide(List<ScannedItem> items, ScanCondition condition, GeminiAnalysisBundle bundle,
		String userEmail) {
		Map<Long, DuplicateMatch> duplicateMatches = detectDuplicateMatches(items, userEmail);
		List<CandidateDecision> decisions = new ArrayList<>();
		for (ScannedItem item : items) {
			GeminiAnalysisResult signal = bundle.resultFor(item);
			List<String> directExcludeMatches = directKeywordMatches(condition.getExcludeKeywords(), item);
			List<String> directIncludeMatches = directKeywordMatches(condition.getIncludeKeywords(), item);
			List<String> semanticExcludeMatches = semanticKeywordMatches(signal.excludeKeywordMatches());
			List<String> semanticIncludeMatches = semanticKeywordMatches(signal.includeKeywordMatches());

			DuplicateMatch duplicateMatch = duplicateMatches.get(item.getItemId());
			if (duplicateMatch != null) {
				decisions.add(createDuplicateDecision(item, signal, duplicateMatch, directExcludeMatches,
					semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches));
				continue;
			}
			if (!directExcludeMatches.isEmpty() || !semanticExcludeMatches.isEmpty()) {
				decisions.add(createProtectedDecision(item, signal, "exclude_keyword", directExcludeMatches,
					semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches));
				continue;
			}
			ProtectionSignal protectionSignal = findProtectionSignal(item, condition, userEmail, signal);
			if (protectionSignal.isProtected()) {
				decisions.add(createProtectedDecision(item, signal, protectionSignal.ruleName(), directExcludeMatches,
					semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches));
				continue;
			}
			if (!directIncludeMatches.isEmpty() || !semanticIncludeMatches.isEmpty()) {
				CandidateCategory category = resolveCleanupCategory(item, signal);
				decisions.add(createCandidateDecision(item, signal, category, resolveCandidateRisk(item, signal, category),
					INCLUDE_SCORE, "include_keyword", directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
					semanticIncludeMatches));
				continue;
			}
			if (matchesPeriodCondition(item, condition)) {
				CandidateCategory category = resolveOldCategory(item);
				decisions.add(createCandidateDecision(item, signal, category, resolveCandidateRisk(item, signal, category),
					PERIOD_SCORE, "period_condition", directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
					semanticIncludeMatches));
				continue;
			}
			CandidateCategory generalCategory = resolveGeneralCategory(item, signal);
			if (generalCategory != null) {
				decisions.add(createCandidateDecision(item, signal, generalCategory,
					resolveCandidateRisk(item, signal, generalCategory), GENERAL_SCORE, "general_cleanup_rule",
					directExcludeMatches, semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches));
			} else {
				decisions.add(createProtectedDecision(item, signal, "no_cleanup_signal", directExcludeMatches,
					semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches));
			}
		}
		return decisions;
	}

	private CandidateDecision createDuplicateDecision(ScannedItem item, GeminiAnalysisResult signal, DuplicateMatch duplicateMatch,
		List<String> directExcludeMatches, List<String> semanticExcludeMatches, List<String> directIncludeMatches,
		List<String> semanticIncludeMatches) {
		Map<String, Object> matchedConditions = createMatchedConditions("duplicate_file", signal, directExcludeMatches,
			semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches);
		matchedConditions.put("duplicate_group_key", duplicateMatch.groupKey());
		matchedConditions.put("duplicate_keeper_item_id", duplicateMatch.keeperItemId());
		matchedConditions.put("duplicate_match_type", duplicateMatch.matchType());
		return new CandidateDecision(item.getItemId(), CandidateCategory.DUPLICATE_FILE, duplicateMatch.riskLevel(),
			DUPLICATE_SCORE, DUPLICATE_SCORE, false, item.getEstimatedReclaimBytes(), signal.confidenceScore(),
			signal.semanticTags(), matchedConditions);
	}

	private CandidateDecision createCandidateDecision(ScannedItem item, GeminiAnalysisResult signal, CandidateCategory category,
		RiskLevel riskLevel, BigDecimal priorityScore, String ruleName, List<String> directExcludeMatches,
		List<String> semanticExcludeMatches, List<String> directIncludeMatches, List<String> semanticIncludeMatches) {
		return new CandidateDecision(item.getItemId(), category, riskLevel, priorityScore, priorityScore, false,
			item.getEstimatedReclaimBytes(), signal.confidenceScore(), signal.semanticTags(),
			createMatchedConditions(ruleName, signal, directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
				semanticIncludeMatches));
	}

	private CandidateDecision createProtectedDecision(ScannedItem item, GeminiAnalysisResult signal, String ruleName,
		List<String> directExcludeMatches, List<String> semanticExcludeMatches, List<String> directIncludeMatches,
		List<String> semanticIncludeMatches) {
		return new CandidateDecision(item.getItemId(), CandidateCategory.PROTECTED, RiskLevel.HIGH, ZERO_SCORE,
			ZERO_SCORE, true, 0L, signal.confidenceScore(), signal.semanticTags(),
			createMatchedConditions(ruleName, signal, directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
				semanticIncludeMatches));
	}

	private Map<String, Object> createMatchedConditions(String ruleName, GeminiAnalysisResult signal,
		List<String> directExcludeMatches, List<String> semanticExcludeMatches, List<String> directIncludeMatches,
		List<String> semanticIncludeMatches) {
		Map<String, Object> matchedConditions = new LinkedHashMap<>();
		matchedConditions.put("rule", ruleName);
		matchedConditions.put("gemini_cleanup_hint", signal.cleanupHint());
		matchedConditions.put("gemini_protected_hint", signal.protectedHint());
		matchedConditions.put("gemini_suggested_category", signal.suggestedCategory() == null ? null : signal.suggestedCategory().name());
		matchedConditions.put("direct_exclude_keyword_matches", directExcludeMatches);
		matchedConditions.put("semantic_exclude_keyword_matches", semanticExcludeMatches);
		matchedConditions.put("direct_include_keyword_matches", directIncludeMatches);
		matchedConditions.put("semantic_include_keyword_matches", semanticIncludeMatches);
		return matchedConditions;
	}

	private Map<Long, DuplicateMatch> detectDuplicateMatches(List<ScannedItem> items, String userEmail) {
		Map<Long, DuplicateMatch> duplicateMatches = new LinkedHashMap<>();
		Map<String, List<ScannedItem>> exactGroups = new LinkedHashMap<>();
		Map<String, List<ScannedItem>> fuzzyGroups = new LinkedHashMap<>();
		for (ScannedItem item : items) {
			if (item.getItemSource() != ItemSource.DRIVE) continue;
			if (!isBlank(item.getMd5Checksum())) {
				exactGroups.computeIfAbsent("md5:" + item.getMd5Checksum(), key -> new ArrayList<>()).add(item);
				continue;
			}
			String normalizedTitle = normalizeDuplicateTitle(item.getTitle());
			if (normalizedTitle.length() >= 3 && !isBlank(item.getMimeType())) {
				fuzzyGroups.computeIfAbsent("name:" + normalizedTitle + ":" + item.getMimeType(), key -> new ArrayList<>()).add(item);
			}
		}
		addDuplicateGroupMatches(exactGroups, duplicateMatches, userEmail, RiskLevel.LOW, "EXACT");
		addDuplicateGroupMatches(fuzzyGroups, duplicateMatches, userEmail, RiskLevel.HIGH, "FUZZY_METADATA");
		return duplicateMatches;
	}

	private void addDuplicateGroupMatches(Map<String, List<ScannedItem>> groups, Map<Long, DuplicateMatch> duplicateMatches,
		String userEmail, RiskLevel riskLevel, String matchType) {
		for (Map.Entry<String, List<ScannedItem>> entry : groups.entrySet()) {
			List<ScannedItem> group = entry.getValue();
			if (group.size() < 2) continue;
			ScannedItem keeper = chooseDuplicateKeeper(group, userEmail);
			for (ScannedItem item : group) {
				if (item.getItemId().equals(keeper.getItemId())) continue;
				duplicateMatches.putIfAbsent(item.getItemId(), new DuplicateMatch(entry.getKey(), keeper.getItemId(), riskLevel, matchType));
			}
		}
	}

	private ScannedItem chooseDuplicateKeeper(List<ScannedItem> group, String userEmail) {
		return group.stream()
			.max(Comparator.comparingLong(item -> duplicateKeeperScore(item, userEmail)))
			.orElse(group.get(0));
	}

	private long duplicateKeeperScore(ScannedItem item, String userEmail) {
		long score = 0L;
		if (!isBlank(userEmail) && userEmail.equalsIgnoreCase(item.getOwnerEmail())) score += 100_000_000_000L;
		if (item.isShared()) score += 10_000_000_000L;
		if (containsFinalSignal(item.getTitle())) score += 1_000_000_000L;
		LocalDateTime activityTime = item.getRecentActivityTime();
		if (activityTime != null) score += activityTime.atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
		return score;
	}

	private ProtectionSignal findProtectionSignal(ScannedItem item, ScanCondition condition, String userEmail, GeminiAnalysisResult signal) {
		if (signal.protectedHint()) return new ProtectionSignal(true, "gemini_protected_hint");
		if (item.getItemSource() == ItemSource.GMAIL && (item.isStarred() || item.isImportant())) {
			return new ProtectionSignal(true, "starred_or_important_mail");
		}
		if (item.getItemSource() == ItemSource.DRIVE && item.isShared()) {
			return new ProtectionSignal(true, "shared_drive_file");
		}
		if (item.getItemSource() == ItemSource.DRIVE && !isBlank(item.getOwnerEmail()) && !isBlank(userEmail)
			&& !item.getOwnerEmail().equalsIgnoreCase(userEmail)) {
			return new ProtectionSignal(true, "owner_mismatch");
		}
		if (condition.isApplyRecentConditions() && item.getRecentActivityTime() != null && condition.getExcludeRecentDays() != null) {
			LocalDateTime recentCutoff = LocalDateTime.now().minusDays(condition.getExcludeRecentDays());
			if (item.getRecentActivityTime().isAfter(recentCutoff)) return new ProtectionSignal(true, "recent_activity");
		}
		return new ProtectionSignal(false, null);
	}

	private List<String> directKeywordMatches(List<String> keywords, ScannedItem item) {
		if (keywords == null || keywords.isEmpty()) return List.of();
		String searchText = item.toSearchText().toLowerCase(Locale.ROOT);
		return keywords.stream()
			.filter(keyword -> !isBlank(keyword))
			.filter(keyword -> searchText.contains(keyword.toLowerCase(Locale.ROOT)))
			.distinct()
			.toList();
	}

	private List<String> semanticKeywordMatches(List<GeminiAnalysisResult.KeywordMatch> matches) {
		if (matches == null || matches.isEmpty()) return List.of();
		return matches.stream()
			.filter(match -> match.confidenceScore() != null && match.confidenceScore().compareTo(SEMANTIC_THRESHOLD) >= 0)
			.map(GeminiAnalysisResult.KeywordMatch::keyword)
			.filter(keyword -> !isBlank(keyword))
			.distinct()
			.toList();
	}

	private boolean matchesPeriodCondition(ScannedItem item, ScanCondition condition) {
		if (!condition.isApplyRecentConditions()) return false;
		LocalDateTime now = LocalDateTime.now();
		if (item.getItemSource() == ItemSource.GMAIL) {
			return condition.getCreatedBeforeMonths() != null && item.getReceivedAt() != null
				&& item.getReceivedAt().isBefore(now.minusMonths(condition.getCreatedBeforeMonths()));
		}
		if (condition.getLastOpenedBeforeMonths() != null && item.getLastOpenedTime() != null
			&& item.getLastOpenedTime().isBefore(now.minusMonths(condition.getLastOpenedBeforeMonths()))) return true;
		if (condition.getLastModifiedBeforeMonths() != null && item.getModifiedTime() != null
			&& item.getModifiedTime().isBefore(now.minusMonths(condition.getLastModifiedBeforeMonths()))) return true;
		return condition.getCreatedBeforeMonths() != null && item.getCreatedTime() != null
			&& item.getCreatedTime().isBefore(now.minusMonths(condition.getCreatedBeforeMonths()));
	}

	private CandidateCategory resolveCleanupCategory(ScannedItem item, GeminiAnalysisResult signal) {
		if (signal.suggestedCategory() != null && signal.suggestedCategory() != CandidateCategory.PROTECTED) {
			return signal.suggestedCategory();
		}
		return resolveGeneralCategory(item, signal) == null ? resolveOldCategory(item) : resolveGeneralCategory(item, signal);
	}

	private CandidateCategory resolveOldCategory(ScannedItem item) {
		return item.getItemSource() == ItemSource.GMAIL ? CandidateCategory.OLD_MAIL : CandidateCategory.OLD_DRIVE_FILE;
	}

	private CandidateCategory resolveGeneralCategory(ScannedItem item, GeminiAnalysisResult signal) {
		if (signal.cleanupHint() && signal.suggestedCategory() != null && signal.suggestedCategory() != CandidateCategory.PROTECTED) {
			return signal.suggestedCategory();
		}
		if (item.getItemSource() == ItemSource.GMAIL) {
			if (isPromotionMail(item, signal)) return CandidateCategory.PROMOTION_MAIL;
			if (item.isHasAttachment() && item.getAttachmentSizeBytes() != null && item.getAttachmentSizeBytes() > 0) {
				return CandidateCategory.LOW_VALUE_ATTACHMENT;
			}
			return null;
		}
		if (item.getSizeBytes() != null && item.getSizeBytes() >= LARGE_FILE_BYTES) return CandidateCategory.LARGE_FILE;
		if (isTempOrBackup(item)) return CandidateCategory.TEMP_OR_BACKUP;
		return null;
	}

	private boolean isPromotionMail(ScannedItem item, GeminiAnalysisResult signal) {
		String searchText = item.toSearchText().toLowerCase(Locale.ROOT);
		if (searchText.contains("category_promotions") || searchText.contains("promotion") || searchText.contains("newsletter")) return true;
		if (searchText.contains("noreply") || searchText.contains("no-reply") || searchText.contains("coupon")) return true;
		return signal.semanticTags().stream()
			.map(tag -> tag.toLowerCase(Locale.ROOT))
			.anyMatch(tag -> tag.contains("promotion") || tag.contains("ad") || tag.contains("newsletter"));
	}

	private boolean isTempOrBackup(ScannedItem item) {
		String searchText = item.toSearchText().toLowerCase(Locale.ROOT);
		if (List.of("tmp", "temp", "bak", "backup", "old").contains(item.getFileExtension())) return true;
		return searchText.contains("backup") || searchText.contains("temp") || searchText.contains("~$") || searchText.contains("copy");
	}

	private RiskLevel resolveCandidateRisk(ScannedItem item, GeminiAnalysisResult signal, CandidateCategory category) {
		if (category == CandidateCategory.PROMOTION_MAIL && isLowRiskPromotion(item, signal)) return RiskLevel.LOW;
		if (category == CandidateCategory.TEMP_OR_BACKUP) return RiskLevel.LOW;
		return RiskLevel.MEDIUM;
	}

	private boolean isLowRiskPromotion(ScannedItem item, GeminiAnalysisResult signal) {
		if (signal.confidenceScore() != null && signal.confidenceScore().compareTo(LOW_RISK_PROMOTION_CONFIDENCE) >= 0) return true;
		String searchText = item.toSearchText().toLowerCase(Locale.ROOT);
		return searchText.contains("category_promotions") || searchText.contains("newsletter")
			|| searchText.contains("noreply") || searchText.contains("no-reply");
	}

	private String normalizeDuplicateTitle(String title) {
		if (isBlank(title)) return "";
		String normalized = title.toLowerCase(Locale.ROOT);
		int dotIndex = normalized.lastIndexOf('.');
		if (dotIndex > 0) normalized = normalized.substring(0, dotIndex);
		normalized = COPY_SUFFIX_PATTERN.matcher(normalized).replaceAll("");
		normalized = normalized
			.replace("최종본", "")
			.replace("최종", "")
			.replace("원본", "")
			.replace("복사본", "")
			.replace("사본", "")
			.replace("발표본", "")
			.replace("제출본", "")
			.replaceAll("(?i)\\b(final|original|copy|submission|backup|old|draft)\\b", "")
			.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "");
		return normalized;
	}

	private boolean containsFinalSignal(String title) {
		if (isBlank(title)) return false;
		String normalized = title.toLowerCase(Locale.ROOT);
		return normalized.contains("final") || normalized.contains("original") || normalized.contains("최종")
			|| normalized.contains("원본") || normalized.contains("제출");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private record DuplicateMatch(String groupKey, Long keeperItemId, RiskLevel riskLevel, String matchType) {
	}

	private record ProtectionSignal(boolean isProtected, String ruleName) {
	}
}

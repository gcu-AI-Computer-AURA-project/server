package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.RiskLevel;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
	private static final BigDecimal SEMANTIC_THRESHOLD = new BigDecimal("60.00");
	private static final BigDecimal LOW_RISK_PROMOTION_CONFIDENCE = new BigDecimal("90.00");
	private static final long LARGE_FILE_BYTES = 500L * 1024L * 1024L;
	private static final String DUPLICATE_RULE = "duplicate_file";
	private static final String INCLUDE_KEYWORD_RULE = "include_keyword";
	private static final String PERIOD_CONDITION_RULE = "period_condition";
	private static final String GENERAL_CLEANUP_RULE = "general_cleanup_rule";
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
					INCLUDE_KEYWORD_RULE, directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
					semanticIncludeMatches));
				continue;
			}
			if (matchesPeriodCondition(item, condition)) {
				CandidateCategory category = resolveOldCategory(item);
				decisions.add(createCandidateDecision(item, signal, category, resolveCandidateRisk(item, signal, category),
					PERIOD_CONDITION_RULE, directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
					semanticIncludeMatches));
				continue;
			}
			CandidateCategory generalCategory = resolveGeneralCategory(item, signal);
			if (generalCategory != null) {
				decisions.add(createCandidateDecision(item, signal, generalCategory,
					resolveCandidateRisk(item, signal, generalCategory), GENERAL_CLEANUP_RULE,
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
		BigDecimal ghostScore = calculateDuplicateGhostScore(duplicateMatch);
		BigDecimal priorityScore = calculatePriorityScore(item, signal, CandidateCategory.DUPLICATE_FILE,
			duplicateMatch.riskLevel(), ghostScore, DUPLICATE_RULE, directIncludeMatches, semanticIncludeMatches);
		Map<String, Object> matchedConditions = createMatchedConditions(DUPLICATE_RULE, signal, directExcludeMatches,
			semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches);
		matchedConditions.put("duplicate_group_key", duplicateMatch.groupKey());
		matchedConditions.put("duplicate_keeper_item_id", duplicateMatch.keeperItemId());
		matchedConditions.put("duplicate_match_type", duplicateMatch.matchType());
		addScoreConditions(matchedConditions, ghostScore, priorityScore, duplicateMatch.riskLevel());
		SelectionStatus selectionStatus = "FUZZY_METADATA".equals(duplicateMatch.matchType())
			? SelectionStatus.DESELECTED
			: SelectionStatus.SELECTED;
		return new CandidateDecision(item.getItemId(), CandidateCategory.DUPLICATE_FILE, duplicateMatch.riskLevel(),
			priorityScore, ghostScore, false, selectionStatus, item.getEstimatedReclaimBytes(), signal.confidenceScore(),
			signal.semanticTags(), matchedConditions);
	}

	private CandidateDecision createCandidateDecision(ScannedItem item, GeminiAnalysisResult signal, CandidateCategory category,
		RiskLevel riskLevel, String ruleName, List<String> directExcludeMatches,
		List<String> semanticExcludeMatches, List<String> directIncludeMatches, List<String> semanticIncludeMatches) {
		BigDecimal ghostScore = calculateGhostScore(item, signal, category, ruleName, directIncludeMatches,
			semanticIncludeMatches);
		BigDecimal priorityScore = calculatePriorityScore(item, signal, category, riskLevel, ghostScore, ruleName,
			directIncludeMatches, semanticIncludeMatches);
		Map<String, Object> matchedConditions = createMatchedConditions(ruleName, signal, directExcludeMatches,
			semanticExcludeMatches, directIncludeMatches, semanticIncludeMatches);
		addScoreConditions(matchedConditions, ghostScore, priorityScore, riskLevel);
		return new CandidateDecision(item.getItemId(), category, riskLevel, priorityScore, ghostScore, false,
			SelectionStatus.SELECTED, item.getEstimatedReclaimBytes(), signal.confidenceScore(), signal.semanticTags(),
			matchedConditions);
	}

	private CandidateDecision createProtectedDecision(ScannedItem item, GeminiAnalysisResult signal, String ruleName,
		List<String> directExcludeMatches, List<String> semanticExcludeMatches, List<String> directIncludeMatches,
		List<String> semanticIncludeMatches) {
		return new CandidateDecision(item.getItemId(), CandidateCategory.PROTECTED, RiskLevel.HIGH, ZERO_SCORE,
			ZERO_SCORE, true, SelectionStatus.NONE, 0L, signal.confidenceScore(), signal.semanticTags(),
			createMatchedConditions(ruleName, signal, directExcludeMatches, semanticExcludeMatches, directIncludeMatches,
				semanticIncludeMatches));
	}

	private Map<String, Object> createMatchedConditions(String ruleName, GeminiAnalysisResult signal,
		List<String> directExcludeMatches, List<String> semanticExcludeMatches, List<String> directIncludeMatches,
		List<String> semanticIncludeMatches) {
		Map<String, Object> matchedConditions = new LinkedHashMap<>();
		matchedConditions.put("rule", ruleName);
		matchedConditions.put("gemini_response_status", signal.responseStatus().name());
		matchedConditions.put("gemini_cleanup_hint", signal.cleanupHint());
		matchedConditions.put("gemini_protected_hint", signal.protectedHint());
		matchedConditions.put("gemini_suggested_category", signal.suggestedCategory() == null ? null : signal.suggestedCategory().name());
		matchedConditions.put("direct_exclude_keyword_matches", directExcludeMatches);
		matchedConditions.put("semantic_exclude_keyword_matches", semanticExcludeMatches);
		matchedConditions.put("direct_include_keyword_matches", directIncludeMatches);
		matchedConditions.put("semantic_include_keyword_matches", semanticIncludeMatches);
		return matchedConditions;
	}

	private void addScoreConditions(Map<String, Object> matchedConditions, BigDecimal ghostScore,
		BigDecimal priorityScore, RiskLevel riskLevel) {
		Map<String, Object> scoring = new LinkedHashMap<>();
		scoring.put("ghost_score", ghostScore);
		scoring.put("priority_score", priorityScore);
		scoring.put("risk_level", riskLevel.name());
		matchedConditions.put("scoring", scoring);
	}

	private Map<Long, DuplicateMatch> detectDuplicateMatches(List<ScannedItem> items, String userEmail) {
		Map<Long, DuplicateMatch> duplicateMatches = new LinkedHashMap<>();
		Map<String, List<ScannedItem>> exactGroups = new LinkedHashMap<>();
		Map<String, List<ScannedItem>> fuzzyGroups = new LinkedHashMap<>();
		for (ScannedItem item : items) {
			if (item.getItemSource() != ItemSource.DRIVE) continue;
			if (!isBlank(item.getMd5Checksum())) {
				exactGroups.computeIfAbsent("md5:" + item.getMd5Checksum(), key -> new ArrayList<>()).add(item);
			}
			String normalizedTitle = normalizeDuplicateTitle(item.getTitle());
			if (normalizedTitle.length() >= 3 && !isBlank(item.getMimeType()) && item.getSizeBytes() != null
				&& item.getSizeBytes() > 0) {
				fuzzyGroups.computeIfAbsent("name:" + normalizedTitle + ":" + item.getMimeType(), key -> new ArrayList<>()).add(item);
			}
		}
		addDuplicateGroupMatches(exactGroups, duplicateMatches, userEmail, RiskLevel.LOW, "EXACT");
		addFuzzyDuplicateGroupMatches(fuzzyGroups, duplicateMatches, userEmail);
		return duplicateMatches;
	}

	private void addFuzzyDuplicateGroupMatches(Map<String, List<ScannedItem>> groups,
		Map<Long, DuplicateMatch> duplicateMatches, String userEmail) {
		for (Map.Entry<String, List<ScannedItem>> entry : groups.entrySet()) {
			List<List<ScannedItem>> similarSizeGroups = splitSimilarSizeGroups(entry.getValue());
			for (int index = 0; index < similarSizeGroups.size(); index++) {
				addDuplicateGroupMatch(entry.getKey() + ":size-group:" + index, similarSizeGroups.get(index),
					duplicateMatches, userEmail, RiskLevel.HIGH, "FUZZY_METADATA");
			}
		}
	}

	private List<List<ScannedItem>> splitSimilarSizeGroups(List<ScannedItem> group) {
		List<ScannedItem> remainingItems = new ArrayList<>(group);
		List<List<ScannedItem>> similarGroups = new ArrayList<>();
		while (!remainingItems.isEmpty()) {
			ScannedItem seed = remainingItems.remove(0);
			List<ScannedItem> similarGroup = new ArrayList<>();
			similarGroup.add(seed);
			List<ScannedItem> nextRemainingItems = new ArrayList<>();
			for (ScannedItem item : remainingItems) {
				if (isSimilarSize(seed, item)) similarGroup.add(item);
				else nextRemainingItems.add(item);
			}
			if (similarGroup.size() >= 2) similarGroups.add(similarGroup);
			remainingItems = nextRemainingItems;
		}
		return similarGroups;
	}

	private void addDuplicateGroupMatch(String groupKey, List<ScannedItem> group, Map<Long, DuplicateMatch> duplicateMatches,
		String userEmail, RiskLevel riskLevel, String matchType) {
		if (group.size() < 2) return;
		ScannedItem keeper = chooseDuplicateKeeper(group, userEmail);
		for (ScannedItem item : group) {
			if (item.getItemId().equals(keeper.getItemId())) continue;
			duplicateMatches.putIfAbsent(item.getItemId(), new DuplicateMatch(groupKey, keeper.getItemId(), riskLevel, matchType));
		}
	}

	private void addDuplicateGroupMatches(Map<String, List<ScannedItem>> groups, Map<Long, DuplicateMatch> duplicateMatches,
		String userEmail, RiskLevel riskLevel, String matchType) {
		for (Map.Entry<String, List<ScannedItem>> entry : groups.entrySet()) {
			addDuplicateGroupMatch(entry.getKey(), entry.getValue(), duplicateMatches, userEmail, riskLevel, matchType);
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

	private boolean isSimilarSize(ScannedItem source, ScannedItem target) {
		long sourceSize = source.getSizeBytes() == null ? 0L : source.getSizeBytes();
		long targetSize = target.getSizeBytes() == null ? 0L : target.getSizeBytes();
		if (sourceSize <= 0 || targetSize <= 0) return false;
		long difference = Math.abs(sourceSize - targetSize);
		long maxSize = Math.max(sourceSize, targetSize);
		long oneMb = 1024L * 1024L;
		if (maxSize < oneMb) return difference <= 64L * 1024L;
		return difference * 100L <= maxSize * 5L;
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

	private BigDecimal calculateDuplicateGhostScore(DuplicateMatch duplicateMatch) {
		if ("EXACT".equals(duplicateMatch.matchType())) return scoreOf(95);
		return scoreOf(78);
	}

	private BigDecimal calculateGhostScore(ScannedItem item, GeminiAnalysisResult signal, CandidateCategory category,
		String ruleName, List<String> directIncludeMatches, List<String> semanticIncludeMatches) {
		int score = categoryBaseGhostScore(category);
		if (INCLUDE_KEYWORD_RULE.equals(ruleName)) score += 10;
		if (PERIOD_CONDITION_RULE.equals(ruleName)) score += oldDataBonus(item);
		if (GENERAL_CLEANUP_RULE.equals(ruleName)) score += 2;
		if (signal.cleanupHint()) score += 6;
		score += confidenceBonus(signal.confidenceScore());
		if (!directIncludeMatches.isEmpty()) score += 8;
		else if (!semanticIncludeMatches.isEmpty()) score += 6;
		return scoreOf(score);
	}

	private BigDecimal calculatePriorityScore(ScannedItem item, GeminiAnalysisResult signal, CandidateCategory category,
		RiskLevel riskLevel, BigDecimal ghostScore, String ruleName, List<String> directIncludeMatches,
		List<String> semanticIncludeMatches) {
		int score = ghostScore.intValue();
		score += reclaimSizeBonus(item.getEstimatedReclaimBytes());
		score += categoryPriorityBonus(category);
		if (INCLUDE_KEYWORD_RULE.equals(ruleName)) score += 8;
		if (DUPLICATE_RULE.equals(ruleName) && riskLevel == RiskLevel.LOW) score += 5;
		if (!directIncludeMatches.isEmpty()) score += 4;
		else if (!semanticIncludeMatches.isEmpty()) score += 3;
		if (signal.cleanupHint()) score += 3;
		if (signal.protectedHint()) score -= 15;
		score -= riskPenalty(riskLevel);
		return scoreOf(score);
	}

	private int categoryBaseGhostScore(CandidateCategory category) {
		return switch (category) {
			case PROMOTION_MAIL -> 74;
			case OLD_MAIL -> 58;
			case DUPLICATE_FILE -> 90;
			case OLD_DRIVE_FILE -> 62;
			case LARGE_FILE -> 55;
			case LOW_VALUE_ATTACHMENT -> 60;
			case TEMP_OR_BACKUP -> 82;
			case PROTECTED -> 0;
		};
	}

	private int categoryPriorityBonus(CandidateCategory category) {
		return switch (category) {
			case DUPLICATE_FILE -> 5;
			case LARGE_FILE -> 8;
			case TEMP_OR_BACKUP -> 6;
			case LOW_VALUE_ATTACHMENT -> 4;
			case PROMOTION_MAIL -> 3;
			case OLD_DRIVE_FILE -> 2;
			case OLD_MAIL, PROTECTED -> 0;
		};
	}

	private int oldDataBonus(ScannedItem item) {
		LocalDateTime activityTime = item.getRecentActivityTime();
		if (activityTime == null) return 0;
		long months = Math.max(0, ChronoUnit.MONTHS.between(activityTime, LocalDateTime.now()));
		if (months >= 60) return 12;
		if (months >= 36) return 9;
		if (months >= 24) return 6;
		if (months >= 12) return 3;
		return 0;
	}

	private int confidenceBonus(BigDecimal confidenceScore) {
		if (confidenceScore == null) return 0;
		if (confidenceScore.compareTo(new BigDecimal("95.00")) >= 0) return 8;
		if (confidenceScore.compareTo(new BigDecimal("85.00")) >= 0) return 6;
		if (confidenceScore.compareTo(new BigDecimal("75.00")) >= 0) return 4;
		if (confidenceScore.compareTo(new BigDecimal("60.00")) >= 0) return 2;
		return 0;
	}

	private int reclaimSizeBonus(long reclaimBytes) {
		long oneMb = 1024L * 1024L;
		long oneGb = 1024L * oneMb;
		if (reclaimBytes >= 5L * oneGb) return 20;
		if (reclaimBytes >= oneGb) return 16;
		if (reclaimBytes >= 500L * oneMb) return 12;
		if (reclaimBytes >= 100L * oneMb) return 8;
		if (reclaimBytes >= 10L * oneMb) return 4;
		return 0;
	}

	private int riskPenalty(RiskLevel riskLevel) {
		return switch (riskLevel) {
			case LOW -> 0;
			case MEDIUM -> 10;
			case HIGH -> 25;
		};
	}

	private BigDecimal scoreOf(int score) {
		int boundedScore = Math.max(0, Math.min(100, score));
		return BigDecimal.valueOf(boundedScore).setScale(2);
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

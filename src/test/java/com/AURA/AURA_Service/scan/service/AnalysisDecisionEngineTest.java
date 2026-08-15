package com.AURA.AURA_Service.scan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.RiskLevel;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AnalysisDecisionEngineTest {
	private final AnalysisDecisionEngine analysisDecisionEngine = new AnalysisDecisionEngine();

	@Test
	void promotionMailHasSeparatedGhostPriorityAndLowRisk() {
		ScannedItem item = gmailItem("mail-1", "[야놀자] 주말 특가 쿠폰", "CATEGORY_PROMOTIONS newsletter", 0L);
		GeminiAnalysisResult signal = signal(item, CandidateCategory.PROMOTION_MAIL, true, false, "92.00",
			List.of("광고", "프로모션"));

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), generalCondition(),
			bundle(item, signal), "user@example.com").get(0);

		assertEquals(CandidateCategory.PROMOTION_MAIL, decision.category());
		assertEquals(RiskLevel.LOW, decision.riskLevel());
		assertNotEquals(decision.ghostScore(), decision.priorityScore());
	}

	@Test
	void promotionMailWithNewsletterSemanticTagUsesLowRisk() {
		ScannedItem item = gmailItemFromSender("mail-newsletter-1",
			"Gachon startup college message Vol. 2", "inbox", "school@example.edu", 0L);
		GeminiAnalysisResult signal = signal(item, CandidateCategory.PROMOTION_MAIL, true, false, "85.00",
			List.of("newsletter"));

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), generalCondition(),
			bundle(item, signal), "user@example.com").get(0);

		assertEquals(CandidateCategory.PROMOTION_MAIL, decision.category());
		assertEquals(RiskLevel.LOW, decision.riskLevel());
	}

	@Test
	void oldLargeDriveFileUsesSizeForPriorityWithoutChangingGhostMeaning() {
		LocalDateTime now = LocalDateTime.now();
		ScannedItem item = ScannedItem.create(null, null, ItemSource.DRIVE, "drive-1", null,
			"2학기/운영체제/녹화본", "260730.mp4", null, null, null, "video/mp4", "mp4",
			2L * 1024L * 1024L * 1024L, 0L, null, now.minusYears(4), now.minusYears(3), now.minusYears(4),
			false, false, false, false, null, "user@example.com", false, null, Map.of());
		GeminiAnalysisResult signal = signal(item, CandidateCategory.OLD_DRIVE_FILE, false, false, "80.00",
			List.of("수업", "녹화본"));

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), periodCondition(),
			bundle(item, signal), "user@example.com").get(0);

		assertEquals(CandidateCategory.OLD_DRIVE_FILE, decision.category());
		assertEquals(RiskLevel.MEDIUM, decision.riskLevel());
		assertNotEquals(decision.ghostScore(), decision.priorityScore());
	}

	@Test
	void oldMailPriorityReflectsSmallSizeDifference() {
		LocalDateTime receivedAt = LocalDateTime.now().minusYears(3);
		ScannedItem smallMail = gmailItemFromSender("mail-score-small",
			"Legacy notice small", "inbox", "notice@example.com", 13_000L, receivedAt);
		ScannedItem largeMail = gmailItemFromSender("mail-score-large",
			"Legacy notice large", "inbox", "notice@example.com", 42_000L, receivedAt);

		CandidateDecision smallDecision = analysisDecisionEngine.decide(List.of(smallMail), periodCondition(),
			bundle(smallMail, signal(smallMail, CandidateCategory.OLD_MAIL, false, false, "70.00", List.of("notice"))),
			"user@example.com").get(0);
		CandidateDecision largeDecision = analysisDecisionEngine.decide(List.of(largeMail), periodCondition(),
			bundle(largeMail, signal(largeMail, CandidateCategory.OLD_MAIL, false, false, "70.00", List.of("notice"))),
			"user@example.com").get(0);

		assertEquals(smallDecision.ghostScore(), largeDecision.ghostScore());
		assertTrue(largeDecision.priorityScore().compareTo(smallDecision.priorityScore()) > 0);
	}

	@Test
	void oldMailGhostScoreReflectsAgeContinuously() {
		ScannedItem newerMail = gmailItemFromSender("mail-score-newer",
			"Legacy notice newer", "inbox", "notice@example.com", 30_000L, LocalDateTime.now().minusYears(1));
		ScannedItem olderMail = gmailItemFromSender("mail-score-older",
			"Legacy notice older", "inbox", "notice@example.com", 30_000L, LocalDateTime.now().minusYears(5));

		CandidateDecision newerDecision = analysisDecisionEngine.decide(List.of(newerMail), periodCondition(),
			bundle(newerMail, signal(newerMail, CandidateCategory.OLD_MAIL, false, false, "70.00", List.of("notice"))),
			"user@example.com").get(0);
		CandidateDecision olderDecision = analysisDecisionEngine.decide(List.of(olderMail), periodCondition(),
			bundle(olderMail, signal(olderMail, CandidateCategory.OLD_MAIL, false, false, "70.00", List.of("notice"))),
			"user@example.com").get(0);

		assertTrue(olderDecision.ghostScore().compareTo(newerDecision.ghostScore()) > 0);
		assertTrue(olderDecision.priorityScore().compareTo(newerDecision.priorityScore()) > 0);
	}

	@Test
	void excludeKeywordKeepsProtectedItemOutOfCleanupScoring() {
		ScannedItem item = gmailItem("mail-2", "카드 영수증", "inbox", 0L);
		GeminiAnalysisResult signal = signal(item, CandidateCategory.OLD_MAIL, true, false, "85.00",
			List.of("영수증"));

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), periodCondition(),
			bundle(item, signal), "user@example.com").get(0);

		assertEquals(CandidateCategory.PROTECTED, decision.category());
		assertEquals(RiskLevel.HIGH, decision.riskLevel());
		assertEquals(new BigDecimal("0.00"), decision.ghostScore());
		assertEquals(new BigDecimal("0.00"), decision.priorityScore());
	}

	@Test
	void classExcludeKeywordProtectsCodingTestRecordingWithoutGeminiMatch() {
		ScannedItem codingTestFile = driveVideoItem("drive-class-1",
			"\uCF54\uB529\uD14C\uC2A4\uD2B83-2.mp4");
		ScannedItem recordingFile = driveVideoItem("drive-class-2",
			"\uCF54\uB529\uD14C\uC2A4\uD2B8 09.10 \uB179\uD654\uBCF8.mp4");

		List<CandidateDecision> decisions = analysisDecisionEngine.decide(List.of(codingTestFile, recordingFile),
			classExcludeCondition(), GeminiAnalysisBundle.ruleBased(), "user@example.com");

		assertEquals(2, decisions.size());
		for (CandidateDecision decision : decisions) {
			assertEquals(CandidateCategory.PROTECTED, decision.category());
			assertEquals(true, decision.isProtected());
			assertEquals(SelectionStatus.NONE, decision.selectionStatus());
		}
	}

	@Test
	void fuzzyDuplicateIsShownAsCandidateButNotSelectedByDefault() {
		ScannedItem original = driveItem("drive-2", "AURA 발표 최종.pptx");
		ScannedItem copy = driveItem("drive-3", "AURA 발표 최종(1).pptx");
		ReflectionTestUtils.setField(original, "itemId", 1L);
		ReflectionTestUtils.setField(copy, "itemId", 2L);

		List<CandidateDecision> decisions = analysisDecisionEngine.decide(List.of(original, copy), generalCondition(),
			GeminiAnalysisBundle.ruleBased(), "user@example.com");
		List<CandidateDecision> duplicateDecisions = decisions.stream()
			.filter(decision -> decision.category() == CandidateCategory.DUPLICATE_FILE)
			.toList();

		assertEquals(1, duplicateDecisions.size());
		assertEquals(CandidateCategory.DUPLICATE_FILE, duplicateDecisions.get(0).category());
		assertEquals(RiskLevel.HIGH, duplicateDecisions.get(0).riskLevel());
		assertEquals(SelectionStatus.DESELECTED, duplicateDecisions.get(0).selectionStatus());
	}

	@Test
	void fuzzyDuplicateRequiresSimilarFileSize() {
		ScannedItem smallFile = driveItem("drive-4", "AURA 발표 최종.pptx", 30L * 1024L * 1024L);
		ScannedItem largeFile = driveItem("drive-5", "AURA 발표 최종(1).pptx", 300L * 1024L * 1024L);
		ReflectionTestUtils.setField(smallFile, "itemId", 4L);
		ReflectionTestUtils.setField(largeFile, "itemId", 5L);

		List<CandidateDecision> decisions = analysisDecisionEngine.decide(List.of(smallFile, largeFile), generalCondition(),
			GeminiAnalysisBundle.ruleBased(), "user@example.com");
		long duplicateCount = decisions.stream()
			.filter(decision -> decision.category() == CandidateCategory.DUPLICATE_FILE)
			.count();

		assertEquals(0, duplicateCount);
	}

	@Test
	void missingGeminiItemIsMarkedWithoutZeroConfidence() {
		ScannedItem item = gmailItem("mail-3", "오래된 안내 메일", "inbox", 0L);

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), periodCondition(),
			GeminiAnalysisBundle.success(Map.of(), "GEMINI_API", "test-model"), "user@example.com").get(0);

		assertNull(decision.aiConfidenceScore());
		assertEquals("MISSING", decision.matchedConditions().get("gemini_response_status"));
	}

	@Test
	void zeroConfidenceProtectedHintDoesNotOverridePeriodRule() {
		ScannedItem item = gmailItemFromSender("mail-protected-empty-1",
			"Legacy service update", "inbox", "notice@example.com", 0L);
		GeminiAnalysisResult signal = signal(item, CandidateCategory.PROTECTED, false, true, "0.00", List.of());

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), periodCondition(),
			bundle(item, signal), "user@example.com").get(0);

		assertEquals(CandidateCategory.OLD_MAIL, decision.category());
		assertEquals(false, decision.isProtected());
	}

	@Test
	void protectedHintWithoutExcludeKeywordDoesNotOverridePeriodRule() {
		ScannedItem item = gmailItemFromSender("mail-protected-hint-1",
			"University newsletter archive", "inbox", "notice@example.edu", 0L);
		GeminiAnalysisResult signal = signal(item, CandidateCategory.PROTECTED, false, true, "95.00", List.of("school"));

		CandidateDecision decision = analysisDecisionEngine.decide(List.of(item), periodCondition(),
			bundle(item, signal), "user@example.com").get(0);

		assertEquals(CandidateCategory.OLD_MAIL, decision.category());
		assertEquals(false, decision.isProtected());
	}

	private ScanCondition generalCondition() {
		return ScanCondition.fromSnapshot(Map.of(
			"scan_source", ScanSource.MAIL_AND_DRIVE.name(),
			"include_keywords", List.of("광고"),
			"exclude_keywords", List.of("영수증"),
			"apply_recent_conditions", false
		));
	}

	private ScanCondition periodCondition() {
		return ScanCondition.fromSnapshot(Map.of(
			"scan_source", ScanSource.MAIL_AND_DRIVE.name(),
			"created_before_months", 6,
			"last_opened_before_months", 36,
			"last_modified_before_months", 24,
			"exclude_recent_days", 30,
			"include_keywords", List.of("광고"),
			"exclude_keywords", List.of("영수증"),
			"apply_recent_conditions", true
		));
	}

	private ScanCondition classExcludeCondition() {
		return ScanCondition.fromSnapshot(Map.of(
			"scan_source", ScanSource.MAIL_AND_DRIVE.name(),
			"exclude_keywords", List.of("\uC218\uC5C5"),
			"last_modified_before_months", 6,
			"apply_recent_conditions", true
		));
	}

	private ScannedItem gmailItem(String externalItemId, String title, String labelText, long attachmentSizeBytes) {
		return ScannedItem.create(null, null, ItemSource.GMAIL, externalItemId, null, null, title,
			"noreply@example.com", labelText, "오늘만 제공되는 특가 안내", "message/rfc822", null, 0L,
			attachmentSizeBytes, LocalDateTime.now().minusYears(2), null, null, null, false, false,
			attachmentSizeBytes > 0, false, null, "user@example.com", false, null, Map.of());
	}

	private ScannedItem gmailItemFromSender(String externalItemId, String title, String labelText, String senderEmail,
		long attachmentSizeBytes) {
		return gmailItemFromSender(externalItemId, title, labelText, senderEmail, attachmentSizeBytes,
			LocalDateTime.now().minusYears(2));
	}

	private ScannedItem gmailItemFromSender(String externalItemId, String title, String labelText, String senderEmail,
		long attachmentSizeBytes, LocalDateTime receivedAt) {
		return ScannedItem.create(null, null, ItemSource.GMAIL, externalItemId, null, null, title,
			senderEmail, labelText, "AURA cleanup test mail snippet", "message/rfc822", null, 0L,
			attachmentSizeBytes, receivedAt, null, null, null, false, false,
			attachmentSizeBytes > 0, false, null, "user@example.com", false, null, Map.of());
	}

	private ScannedItem driveItem(String externalItemId, String title) {
		return driveItem(externalItemId, title, 30L * 1024L * 1024L);
	}

	private ScannedItem driveItem(String externalItemId, String title, long sizeBytes) {
		return ScannedItem.create(null, null, ItemSource.DRIVE, externalItemId, null, "AURA/발표", title,
			null, null, null, "application/vnd.openxmlformats-officedocument.presentationml.presentation",
			"pptx", sizeBytes, 0L, null, LocalDateTime.now().minusYears(1),
			LocalDateTime.now().minusMonths(6), LocalDateTime.now().minusMonths(6), false, false, false,
			false, null, "user@example.com", false, null, Map.of());
	}

	private ScannedItem driveVideoItem(String externalItemId, String title) {
		return ScannedItem.create(null, null, ItemSource.DRIVE, externalItemId, null, "\uC218\uC5C5/\uB179\uD654",
			title, null, null, null, "video/mp4", "mp4", 900L * 1024L * 1024L, 0L, null,
			LocalDateTime.now().minusYears(2), LocalDateTime.now().minusYears(1), LocalDateTime.now().minusYears(1),
			false, false, false, false, null, "user@example.com", false, null, Map.of());
	}

	private GeminiAnalysisResult signal(ScannedItem item, CandidateCategory category, boolean cleanupHint,
		boolean protectedHint, String confidenceScore, List<String> semanticTags) {
		return new GeminiAnalysisResult(item.getClientItemKey(), category, cleanupHint, protectedHint,
			new BigDecimal(confidenceScore), semanticTags, List.of(), List.of(), GeminiAnalysisResult.ResponseStatus.SUCCESS);
	}

	private GeminiAnalysisBundle bundle(ScannedItem item, GeminiAnalysisResult signal) {
		return GeminiAnalysisBundle.success(Map.of(item.getClientItemKey(), signal), "GEMINI_API", "test-model");
	}
}

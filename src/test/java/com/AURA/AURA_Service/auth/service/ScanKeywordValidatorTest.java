package com.AURA.AURA_Service.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.AURA.AURA_Service.common.CustomException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScanKeywordValidatorTest {
	private final ScanKeywordValidator scanKeywordValidator = new ScanKeywordValidator();

	@Test
	void sameIncludeExcludeKeywordIsRejected() {
		assertThrows(CustomException.class, () -> scanKeywordValidator.validateNoConflict(
			List.of(" 광고 ", "뉴스레터"),
			List.of("광고")
		));
	}

	@Test
	void differentIncludeExcludeKeywordsAreAccepted() {
		assertDoesNotThrow(() -> scanKeywordValidator.validateNoConflict(
			List.of("광고", "뉴스레터"),
			List.of("영수증", "계약서")
		));
	}
}

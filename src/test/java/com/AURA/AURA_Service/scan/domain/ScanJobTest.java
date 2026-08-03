package com.AURA.AURA_Service.scan.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScanJobTest {
	@Test
	void scanProgressUsesMetadataAndAnalysisRanges() {
		ScanJob scanJob = new ScanJob(null, null, ScanSource.MAIL, Map.of());

		scanJob.markScanning();
		scanJob.updateScanningProgress(new BigDecimal("25.25"));
		scanJob.updateScanningProgress(new BigDecimal("70.00"));

		assertEquals(new BigDecimal("50.00"), scanJob.getProgressPercent());

		scanJob.markAnalyzing(10, 0);
		scanJob.updateAnalyzingProgress(new BigDecimal("75.75"));
		scanJob.updateAnalyzingProgress(new BigDecimal("100.00"));

		assertEquals(new BigDecimal("99.00"), scanJob.getProgressPercent());

		scanJob.markCompleted(8, 2, 1024L);

		assertEquals(new BigDecimal("100.00"), scanJob.getProgressPercent());
	}
}

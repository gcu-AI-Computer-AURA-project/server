package com.AURA.AURA_Service.scan.service;

public interface ScanProgressListener {
	void onMetadataProgress(int collectedCount, int expectedCount);

	void onAnalysisProgress(int analyzedCount, int totalCount);

	static ScanProgressListener none() {
		return new ScanProgressListener() {
			@Override
			public void onMetadataProgress(int collectedCount, int expectedCount) {
			}

			@Override
			public void onAnalysisProgress(int analyzedCount, int totalCount) {
			}
		};
	}
}

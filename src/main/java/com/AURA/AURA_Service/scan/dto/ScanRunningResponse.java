package com.AURA.AURA_Service.scan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScanRunningResponse(
	@JsonProperty("scan_job")
	ScanRunningJobResponse scanJob
) {
	public static ScanRunningResponse empty() {
		return new ScanRunningResponse(null);
	}

	public static ScanRunningResponse from(ScanRunningJobResponse scanJob) {
		return new ScanRunningResponse(scanJob);
	}
}

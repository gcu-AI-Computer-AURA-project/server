package com.AURA.AURA_Service.scan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class ScanJobExecutionLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScanJobExecutionLauncher.class);

	private final TaskExecutor scanTaskExecutor;
	private final ScanJobExecutionService scanJobExecutionService;

	public ScanJobExecutionLauncher(@Qualifier("scanTaskExecutor") TaskExecutor scanTaskExecutor,
		ScanJobExecutionService scanJobExecutionService) {
		this.scanTaskExecutor = scanTaskExecutor;
		this.scanJobExecutionService = scanJobExecutionService;
	}

	public void launch(Long scanJobId) {
		scanTaskExecutor.execute(() -> {
			try {
				scanJobExecutionService.execute(scanJobId);
			} catch (RuntimeException exception) {
				LOGGER.error("Scan job execution failed unexpectedly. scanJobId={}", scanJobId, exception);
			}
		});
	}
}

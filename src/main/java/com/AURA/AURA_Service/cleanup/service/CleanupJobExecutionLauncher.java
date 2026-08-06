package com.AURA.AURA_Service.cleanup.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CleanupJobExecutionLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanupJobExecutionLauncher.class);

	private final TaskExecutor cleanupTaskExecutor;
	private final CleanupJobExecutionService cleanupJobExecutionService;

	public CleanupJobExecutionLauncher(@Qualifier("cleanupTaskExecutor") TaskExecutor cleanupTaskExecutor,
		CleanupJobExecutionService cleanupJobExecutionService) {
		this.cleanupTaskExecutor = cleanupTaskExecutor;
		this.cleanupJobExecutionService = cleanupJobExecutionService;
	}

	public void launch(Long cleanupJobId) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					execute(cleanupJobId);
				}
			});
			return;
		}
		execute(cleanupJobId);
	}

	private void execute(Long cleanupJobId) {
		cleanupTaskExecutor.execute(() -> {
			try {
				cleanupJobExecutionService.execute(cleanupJobId);
			} catch (RuntimeException exception) {
				LOGGER.error("Cleanup job execution failed unexpectedly. cleanupJobId={}", cleanupJobId, exception);
			}
		});
	}
}

package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.JobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupJobRepository extends JpaRepository<CleanupJob, Long> {
	Optional<CleanupJob> findByCleanupJobIdAndUserUserId(Long cleanupJobId, Long userId);
	Optional<CleanupJob> findFirstByUserUserIdAndJobStatusInOrderByCreatedAtDesc(Long userId,
		Collection<JobStatus> jobStatuses);
}

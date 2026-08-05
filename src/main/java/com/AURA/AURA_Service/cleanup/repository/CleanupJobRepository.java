package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupJobRepository extends JpaRepository<CleanupJob, Long> {
	Optional<CleanupJob> findByCleanupJobIdAndUserUserId(Long cleanupJobId, Long userId);
}

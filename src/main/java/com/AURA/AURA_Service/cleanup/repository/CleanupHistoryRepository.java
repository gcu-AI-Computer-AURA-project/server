package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.cleanup.domain.CleanupHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupHistoryRepository extends JpaRepository<CleanupHistory, Long> {
	long countByUser(User user);
	boolean existsByCleanupJobCleanupJobId(Long cleanupJobId);
	Optional<CleanupHistory> findByCleanupJobCleanupJobIdAndUserUserId(Long cleanupJobId, Long userId);
}

package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupJobItemRepository extends JpaRepository<CleanupJobItem, Long> {
	List<CleanupJobItem> findByCleanupJobCleanupJobIdOrderByCleanupItemIdAsc(Long cleanupJobId);
}

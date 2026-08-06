package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem.ProcessStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupJobItemRepository extends JpaRepository<CleanupJobItem, Long> {
	List<CleanupJobItem> findByCleanupJobCleanupJobIdOrderByCleanupItemIdAsc(Long cleanupJobId);
	List<CleanupJobItem> findByCleanupJobCleanupJobIdAndProcessStatusOrderByCleanupItemIdAsc(Long cleanupJobId,
		ProcessStatus processStatus);
}

package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CarbonSavingHistory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarbonSavingHistoryRepository extends JpaRepository<CarbonSavingHistory, Long> {
	Optional<CarbonSavingHistory> findByCleanupHistoryHistoryId(Long historyId);
}

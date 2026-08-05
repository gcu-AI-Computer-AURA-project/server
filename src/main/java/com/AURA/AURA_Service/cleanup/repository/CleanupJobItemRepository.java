package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CleanupJobItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupJobItemRepository extends JpaRepository<CleanupJobItem, Long> {
}

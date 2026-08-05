package com.AURA.AURA_Service.cleanup.repository;

import com.AURA.AURA_Service.cleanup.domain.CleanupJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleanupJobRepository extends JpaRepository<CleanupJob, Long> {
}

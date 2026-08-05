package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.scan.domain.ScanJob;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {
	Optional<ScanJob> findByScanJobIdAndUser_UserIdAndDeletedAtIsNull(Long scanJobId, Long userId);
}

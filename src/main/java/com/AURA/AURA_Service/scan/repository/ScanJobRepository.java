package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {
	boolean existsByUserAndJobStatusInAndDeletedAtIsNull(User user, Collection<JobStatus> jobStatuses);

	Optional<ScanJob> findFirstByUserAndJobStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(User user,
		Collection<JobStatus> jobStatuses);
}

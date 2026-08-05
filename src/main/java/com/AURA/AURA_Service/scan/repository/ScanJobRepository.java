package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanJobRepository extends JpaRepository<ScanJob, Long> {
	boolean existsByUserAndJobStatusInAndDeletedAtIsNull(User user, Collection<JobStatus> jobStatuses);

	Optional<ScanJob> findFirstByUserAndJobStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(User user,
		Collection<JobStatus> jobStatuses);

	Optional<ScanJob> findByScanJobIdAndUserAndDeletedAtIsNull(Long scanJobId, User user);

	Optional<ScanJob> findByScanJobIdAndUser_UserIdAndDeletedAtIsNull(Long scanJobId, Long userId);

	Page<ScanJob> findByUserAndDeletedAtIsNull(User user, Pageable pageable);

	Page<ScanJob> findByUserAndJobStatusAndDeletedAtIsNull(User user, JobStatus jobStatus, Pageable pageable);
}

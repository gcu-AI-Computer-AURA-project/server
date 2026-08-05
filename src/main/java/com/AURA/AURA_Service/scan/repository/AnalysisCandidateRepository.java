package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisCandidateRepository extends JpaRepository<AnalysisCandidate, Long> {
	@Query("""
		select
			candidate.category as category,
			count(candidate) as itemCount,
			coalesce(sum(candidate.estimatedReclaimBytes), 0) as estimatedReclaimBytes,
			coalesce(sum(case when candidate.selectionStatus = :selectedStatus then 1 else 0 end), 0) as selectedCount
		from AnalysisCandidate candidate
		where candidate.scanJob.scanJobId = :scanJobId
		group by candidate.category
		order by candidate.category
		""")
	List<AnalysisCategorySummary> summarizeByScanJobId(@Param("scanJobId") Long scanJobId,
		@Param("selectedStatus") SelectionStatus selectedStatus);
}

package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisCandidateRepository extends JpaRepository<AnalysisCandidate, Long> {
	@Query("""
		select candidate
		from AnalysisCandidate candidate
		join fetch candidate.scanJob scanJob
		join fetch candidate.scannedItem item
		where candidate.candidateId = :candidateId
			and scanJob.user.userId = :userId
			and scanJob.deletedAt is null
		""")
	Optional<AnalysisCandidate> findDetailByCandidateIdAndUserId(@Param("candidateId") Long candidateId,
		@Param("userId") Long userId);

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

	@Query(value = """
		select candidate
		from AnalysisCandidate candidate
		join fetch candidate.scannedItem item
		where candidate.scanJob.scanJobId = :scanJobId
			and (:category is null or candidate.category = :category)
			and (:itemSource is null or item.itemSource = :itemSource)
			and (:selectionStatus is null or candidate.selectionStatus = :selectionStatus)
			and (:includeProtected = true or candidate.isProtected = false)
		order by
			case when :sort = 'size_desc' then item.sizeBytes else null end desc,
			case when :sort = 'date_asc' then coalesce(item.lastOpenedTime, item.modifiedTime, item.receivedAt, item.createdTime) else null end asc,
			case when :sort = 'priority_desc' then candidate.priorityScore else null end desc,
			candidate.candidateId asc
		""",
		countQuery = """
		select count(candidate)
		from AnalysisCandidate candidate
		join candidate.scannedItem item
		where candidate.scanJob.scanJobId = :scanJobId
			and (:category is null or candidate.category = :category)
			and (:itemSource is null or item.itemSource = :itemSource)
			and (:selectionStatus is null or candidate.selectionStatus = :selectionStatus)
			and (:includeProtected = true or candidate.isProtected = false)
		""")
	Page<AnalysisCandidate> findCandidates(@Param("scanJobId") Long scanJobId,
		@Param("category") CandidateCategory category,
		@Param("itemSource") ItemSource itemSource,
		@Param("selectionStatus") SelectionStatus selectionStatus,
		@Param("includeProtected") boolean includeProtected,
		@Param("sort") String sort,
		Pageable pageable);

	@Query("""
		select candidate
		from AnalysisCandidate candidate
		join candidate.scannedItem item
		where candidate.scanJob.scanJobId = :scanJobId
			and (:category is null or candidate.category = :category)
			and (:itemSource is null or item.itemSource = :itemSource)
			and (:hasCandidateIds = false or candidate.candidateId in :candidateIds)
			and (:excludeProtected = false or candidate.isProtected = false)
		order by candidate.candidateId asc
		""")
	List<AnalysisCandidate> findCandidatesForSelection(@Param("scanJobId") Long scanJobId,
		@Param("category") CandidateCategory category,
		@Param("itemSource") ItemSource itemSource,
		@Param("candidateIds") List<Long> candidateIds,
		@Param("hasCandidateIds") boolean hasCandidateIds,
		@Param("excludeProtected") boolean excludeProtected);

	@Query("""
		select
			count(candidate) as selectedCount,
			coalesce(sum(candidate.estimatedReclaimBytes), 0) as selectedEstimatedReclaimBytes
		from AnalysisCandidate candidate
		where candidate.scanJob.scanJobId = :scanJobId
			and candidate.selectionStatus = :selectionStatus
		""")
	CandidateSelectionSummary summarizeSelectedByScanJobId(@Param("scanJobId") Long scanJobId,
		@Param("selectionStatus") SelectionStatus selectionStatus);

	@Query("""
		select candidate
		from AnalysisCandidate candidate
		join fetch candidate.scannedItem item
		where candidate.scanJob.scanJobId = :scanJobId
			and candidate.selectionStatus = :selectionStatus
		order by
			item.itemSource asc,
			candidate.priorityScore desc,
			candidate.candidateId asc
		""")
	List<AnalysisCandidate> findSelectedCandidates(@Param("scanJobId") Long scanJobId,
		@Param("selectionStatus") SelectionStatus selectionStatus);

	@Query("""
		select candidate
		from AnalysisCandidate candidate
		where candidate.scanJob.scanJobId = :scanJobId
			and candidate.isProtected = true
		order by candidate.candidateId asc
		""")
	List<AnalysisCandidate> findProtectedCandidates(@Param("scanJobId") Long scanJobId);
}

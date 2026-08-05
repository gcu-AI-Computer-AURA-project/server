package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record SelectedCandidateResponse(
	@JsonProperty("scan_job_id")
	Long scanJobId,
	@JsonProperty("selected_summary")
	SelectedSummaryResponse selectedSummary,
	@JsonProperty("protected_summary")
	ProtectedSummaryResponse protectedSummary,
	List<SelectedCandidateItemResponse> items
) {
	public static SelectedCandidateResponse from(Long scanJobId, List<AnalysisCandidate> selectedCandidates,
		List<AnalysisCandidate> protectedCandidates, List<String> protectedConditions) {
		return new SelectedCandidateResponse(
			scanJobId,
			SelectedSummaryResponse.from(selectedCandidates),
			new ProtectedSummaryResponse(protectedCandidates.size(), protectedConditions),
			selectedCandidates.stream()
				.map(SelectedCandidateItemResponse::from)
				.toList()
		);
	}

	public record SelectedSummaryResponse(
		@JsonProperty("mail_count")
		long mailCount,
		@JsonProperty("drive_count")
		long driveCount,
		@JsonProperty("total_count")
		long totalCount,
		@JsonProperty("mail_estimated_reclaim_bytes")
		long mailEstimatedReclaimBytes,
		@JsonProperty("drive_estimated_reclaim_bytes")
		long driveEstimatedReclaimBytes,
		@JsonProperty("total_estimated_reclaim_bytes")
		long totalEstimatedReclaimBytes
	) {
		public static SelectedSummaryResponse from(List<AnalysisCandidate> candidates) {
			long mailCount = countBySource(candidates, ItemSource.GMAIL);
			long driveCount = countBySource(candidates, ItemSource.DRIVE);
			long mailEstimatedReclaimBytes = sumEstimatedReclaimBytesBySource(candidates, ItemSource.GMAIL);
			long driveEstimatedReclaimBytes = sumEstimatedReclaimBytesBySource(candidates, ItemSource.DRIVE);
			return new SelectedSummaryResponse(
				mailCount,
				driveCount,
				candidates.size(),
				mailEstimatedReclaimBytes,
				driveEstimatedReclaimBytes,
				mailEstimatedReclaimBytes + driveEstimatedReclaimBytes
			);
		}

		private static long countBySource(List<AnalysisCandidate> candidates, ItemSource itemSource) {
			return candidates.stream()
				.filter(candidate -> candidate.getScannedItem().getItemSource() == itemSource)
				.count();
		}

		private static long sumEstimatedReclaimBytesBySource(List<AnalysisCandidate> candidates, ItemSource itemSource) {
			return candidates.stream()
				.filter(candidate -> candidate.getScannedItem().getItemSource() == itemSource)
				.mapToLong(candidate -> candidate.getEstimatedReclaimBytes() == null ? 0L : candidate.getEstimatedReclaimBytes())
				.sum();
		}
	}

	public record ProtectedSummaryResponse(
		@JsonProperty("protected_count")
		int protectedCount,
		@JsonProperty("protected_conditions")
		List<String> protectedConditions
	) {
	}
}

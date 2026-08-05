package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;

public interface AnalysisCategorySummary {
	CandidateCategory getCategory();
	Long getItemCount();
	Long getEstimatedReclaimBytes();
	Long getSelectedCount();
}

package com.AURA.AURA_Service.scan.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "analysis_candidates")
public class AnalysisCandidate {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "candidate_id") private Long candidateId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scan_job_id", nullable = false) private ScanJob scanJob;
	@OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id", nullable = false, unique = true) private ScannedItem scannedItem;
	@Enumerated(EnumType.STRING) @Column(name = "category", nullable = false) private CandidateCategory category;
	@Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false) private RiskLevel riskLevel;
	@Column(name = "priority_score", nullable = false, precision = 5, scale = 2) private BigDecimal priorityScore;
	@Column(name = "ghost_score", nullable = false, precision = 5, scale = 2) private BigDecimal ghostScore;
	@Column(name = "is_protected", nullable = false) private boolean isProtected;
	@Enumerated(EnumType.STRING) @Column(name = "selection_status", nullable = false) private SelectionStatus selectionStatus;
	@Column(name = "selection_version", nullable = false) private Integer selectionVersion = 1;
	@Column(name = "estimated_reclaim_bytes", nullable = false) private Long estimatedReclaimBytes;
	@Column(name = "ai_provider", length = 50) private String aiProvider;
	@Column(name = "ai_model_name", length = 100) private String aiModelName;
	@Column(name = "ai_confidence_score", precision = 5, scale = 2) private BigDecimal aiConfidenceScore;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "semantic_tags_json", columnDefinition = "json") private List<String> semanticTags;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "matched_conditions_json", columnDefinition = "json") private Map<String, Object> matchedConditions;
	@Column(name = "analyzed_at", nullable = false) private LocalDateTime analyzedAt;
	@UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected AnalysisCandidate() { }

	public static AnalysisCandidate create(ScanJob scanJob, ScannedItem scannedItem, CandidateCategory category,
		RiskLevel riskLevel, BigDecimal priorityScore, BigDecimal ghostScore, boolean isProtected,
		SelectionStatus selectionStatus, long estimatedReclaimBytes, String aiProvider, String aiModelName,
		BigDecimal aiConfidenceScore, List<String> semanticTags, Map<String, Object> matchedConditions) {
		AnalysisCandidate candidate = new AnalysisCandidate();
		candidate.scanJob = scanJob;
		candidate.scannedItem = scannedItem;
		candidate.category = category;
		candidate.riskLevel = riskLevel;
		candidate.priorityScore = priorityScore;
		candidate.ghostScore = ghostScore;
		candidate.isProtected = isProtected;
		candidate.selectionStatus = resolveSelectionStatus(isProtected, selectionStatus);
		candidate.estimatedReclaimBytes = isProtected ? 0L : estimatedReclaimBytes;
		candidate.aiProvider = aiProvider;
		candidate.aiModelName = aiModelName;
		candidate.aiConfidenceScore = aiConfidenceScore;
		candidate.semanticTags = semanticTags;
		candidate.matchedConditions = matchedConditions;
		candidate.analyzedAt = LocalDateTime.now();
		return candidate;
	}

	public boolean isProtected() { return isProtected; }
	public boolean isSelected() { return selectionStatus == SelectionStatus.SELECTED; }
	public Long getEstimatedReclaimBytes() { return estimatedReclaimBytes; }

	private static SelectionStatus resolveSelectionStatus(boolean isProtected, SelectionStatus selectionStatus) {
		if (isProtected) return SelectionStatus.NONE;
		return selectionStatus == null || selectionStatus == SelectionStatus.NONE ? SelectionStatus.SELECTED : selectionStatus;
	}

	public enum CandidateCategory {
		PROMOTION_MAIL,
		OLD_MAIL,
		DUPLICATE_FILE,
		OLD_DRIVE_FILE,
		LARGE_FILE,
		LOW_VALUE_ATTACHMENT,
		TEMP_OR_BACKUP,
		PROTECTED
	}

	public enum RiskLevel {
		LOW,
		MEDIUM,
		HIGH
	}

	public enum SelectionStatus {
		NONE,
		SELECTED,
		DESELECTED
	}
}

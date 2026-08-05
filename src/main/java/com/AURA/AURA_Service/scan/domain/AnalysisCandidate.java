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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "analysis_candidates")
public class AnalysisCandidate {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "candidate_id") private Long candidateId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scan_job_id", nullable = false) private ScanJob scanJob;
	@Column(name = "item_id", nullable = false) private Long itemId;
	@Enumerated(EnumType.STRING) @Column(nullable = false) private CandidateCategory category;
	@Enumerated(EnumType.STRING) @Column(name = "risk_level", nullable = false) private RiskLevel riskLevel = RiskLevel.MEDIUM;
	@Column(name = "priority_score", nullable = false, precision = 5, scale = 2) private BigDecimal priorityScore = BigDecimal.ZERO;
	@Column(name = "ghost_score", nullable = false, precision = 5, scale = 2) private BigDecimal ghostScore = BigDecimal.ZERO;
	@Column(name = "is_protected", nullable = false) private boolean isProtected;
	@Enumerated(EnumType.STRING) @Column(name = "selection_status", nullable = false) private SelectionStatus selectionStatus = SelectionStatus.NONE;
	@Column(name = "selection_version", nullable = false) private Integer selectionVersion = 0;
	@Column(name = "estimated_reclaim_bytes", nullable = false) private Long estimatedReclaimBytes = 0L;
	@Column(name = "ai_provider", length = 50) private String aiProvider;
	@Column(name = "ai_model_name", length = 100) private String aiModelName;
	@Column(name = "ai_confidence_score", precision = 5, scale = 2) private BigDecimal aiConfidenceScore;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "semantic_tags_json", columnDefinition = "json") private String semanticTagsJson;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "matched_conditions_json", columnDefinition = "json") private String matchedConditionsJson;
	@Column(name = "analyzed_at", nullable = false, insertable = false, updatable = false) private LocalDateTime analyzedAt;
	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private LocalDateTime updatedAt;

	protected AnalysisCandidate() { }

	public enum CandidateCategory {
		PROMOTION_MAIL("광고·프로모션 메일"),
		OLD_MAIL("오래된 메일"),
		DUPLICATE_FILE("중복 파일"),
		OLD_DRIVE_FILE("오래된 Drive 파일"),
		LARGE_FILE("대용량 파일"),
		LOW_VALUE_ATTACHMENT("낮은 가치의 첨부파일"),
		TEMP_OR_BACKUP("임시·백업 파일"),
		PROTECTED("보호 대상");

		private final String displayName;

		CandidateCategory(String displayName) {
			this.displayName = displayName;
		}

		public String getDisplayName() { return displayName; }
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

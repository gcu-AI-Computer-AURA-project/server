package com.AURA.AURA_Service.scan.domain;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.User;
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
@Table(name = "scan_jobs")
public class ScanJob {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "scan_job_id") private Long scanJobId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "setting_id") private ScanSetting scanSetting;
	@Enumerated(EnumType.STRING) @Column(name = "job_status", nullable = false) private ScanJobStatus jobStatus = ScanJobStatus.PENDING;
	@Enumerated(EnumType.STRING) @Column(name = "scan_source", nullable = false) private ScanSource scanSource;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "condition_snapshot_json", nullable = false, columnDefinition = "json") private String conditionSnapshotJson;
	@Column(name = "progress_percent", nullable = false, precision = 5, scale = 2) private BigDecimal progressPercent = BigDecimal.ZERO;
	@Column(name = "mail_scanned_count", nullable = false) private Integer mailScannedCount = 0;
	@Column(name = "drive_scanned_count", nullable = false) private Integer driveScannedCount = 0;
	@Column(name = "candidate_count", nullable = false) private Integer candidateCount = 0;
	@Column(name = "protected_count", nullable = false) private Integer protectedCount = 0;
	@Column(name = "estimated_reclaim_bytes", nullable = false) private Long estimatedReclaimBytes = 0L;
	@Column(name = "error_message", length = 500) private String errorMessage;
	@Column(name = "started_at") private LocalDateTime startedAt;
	@Column(name = "completed_at") private LocalDateTime completedAt;
	@Column(name = "canceled_at") private LocalDateTime canceledAt;
	@Column(name = "deleted_at") private LocalDateTime deletedAt;
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private LocalDateTime updatedAt;

	protected ScanJob() { }

	public Long getScanJobId() { return scanJobId; }
	public Integer getCandidateCount() { return candidateCount; }
	public Integer getProtectedCount() { return protectedCount; }
	public Long getEstimatedReclaimBytes() { return estimatedReclaimBytes; }

	public enum ScanJobStatus {
		PENDING,
		SCANNING,
		ANALYZING,
		COMPLETED,
		FAILED,
		CANCELED,
		PARTIAL_FAILED
	}

	public enum ScanSource {
		MAIL,
		DRIVE_ALL,
		DRIVE_FOLDER,
		MAIL_AND_DRIVE
	}
}

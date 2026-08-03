package com.AURA.AURA_Service.scan.domain;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scan_jobs")
public class ScanJob {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "scan_job_id") private Long scanJobId;
	@ManyToOne @JoinColumn(name = "user_id") private User user;
	@ManyToOne @JoinColumn(name = "setting_id") private ScanSetting scanSetting;
	@Enumerated(EnumType.STRING) @Column(name = "job_status", nullable = false) private JobStatus jobStatus = JobStatus.PENDING;
	@Enumerated(EnumType.STRING) @Column(name = "scan_source", nullable = false) private ScanSource scanSource;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "condition_snapshot_json", columnDefinition = "json", nullable = false) private Map<String, Object> conditionSnapshot;
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
	@CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
	@UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected ScanJob() { }

	public ScanJob(User user, ScanSetting scanSetting, ScanSource scanSource, Map<String, Object> conditionSnapshot) {
		this.user = user;
		this.scanSetting = scanSetting;
		this.scanSource = scanSource;
		this.conditionSnapshot = conditionSnapshot;
		this.progressPercent = new BigDecimal("0.00");
	}

	public Long getScanJobId() { return scanJobId; }
	public User getUser() { return user; }
	public JobStatus getJobStatus() { return jobStatus; }
	public ScanSource getScanSource() { return scanSource; }
	public Map<String, Object> getConditionSnapshot() { return conditionSnapshot; }
	public BigDecimal getProgressPercent() { return progressPercent; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	/**
	 * 스캔 시작 상태 변경 메소드
	 * 백그라운드 작업이 실제 Google 메타데이터 조회를 시작했음을 저장한다.
	 *
	 * @return : 없음
	 * @since : 2026.08.02
	 * @version : 0.0.1
	 * @author : 최준혁
	 */
	public void markScanning() {
		this.jobStatus = JobStatus.SCANNING;
		this.progressPercent = new BigDecimal("0.00");
		this.startedAt = LocalDateTime.now();
	}

	public void updateScanningProgress(BigDecimal progressPercent) {
		if (this.jobStatus != JobStatus.SCANNING) return;
		updateProgress(progressPercent, new BigDecimal("0.00"), new BigDecimal("50.00"));
	}

	public void markAnalyzing(int mailScannedCount, int driveScannedCount) {
		this.jobStatus = JobStatus.ANALYZING;
		updateProgress(new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00"));
		this.mailScannedCount = mailScannedCount;
		this.driveScannedCount = driveScannedCount;
	}

	public void updateAnalyzingProgress(BigDecimal progressPercent) {
		if (this.jobStatus != JobStatus.ANALYZING) return;
		updateProgress(progressPercent, new BigDecimal("50.00"), new BigDecimal("99.00"));
	}

	public void markCompleted(int candidateCount, int protectedCount, long estimatedReclaimBytes) {
		this.jobStatus = JobStatus.COMPLETED;
		this.progressPercent = new BigDecimal("100.00");
		this.candidateCount = candidateCount;
		this.protectedCount = protectedCount;
		this.estimatedReclaimBytes = estimatedReclaimBytes;
		this.completedAt = LocalDateTime.now();
	}

	public void markPartialFailed(int candidateCount, int protectedCount, long estimatedReclaimBytes, String errorMessage) {
		this.jobStatus = JobStatus.PARTIAL_FAILED;
		this.progressPercent = new BigDecimal("100.00");
		this.candidateCount = candidateCount;
		this.protectedCount = protectedCount;
		this.estimatedReclaimBytes = estimatedReclaimBytes;
		this.errorMessage = trimErrorMessage(errorMessage);
		this.completedAt = LocalDateTime.now();
	}

	public void markFailed(String errorMessage) {
		this.jobStatus = JobStatus.FAILED;
		this.errorMessage = trimErrorMessage(errorMessage);
		this.completedAt = LocalDateTime.now();
	}

	private String trimErrorMessage(String value) {
		if (value == null) return null;
		return value.length() > 500 ? value.substring(0, 500) : value;
	}

	private void updateProgress(BigDecimal value, BigDecimal min, BigDecimal max) {
		BigDecimal normalized = value == null ? min : value.setScale(2, RoundingMode.HALF_UP);
		if (normalized.compareTo(min) < 0) normalized = min;
		if (normalized.compareTo(max) > 0) normalized = max;
		if (this.progressPercent == null || normalized.compareTo(this.progressPercent) > 0) {
			this.progressPercent = normalized;
		}
	}

	public enum JobStatus {
		PENDING,
		SCANNING,
		ANALYZING,
		COMPLETED,
		FAILED,
		CANCELED,
		PARTIAL_FAILED
	}
}

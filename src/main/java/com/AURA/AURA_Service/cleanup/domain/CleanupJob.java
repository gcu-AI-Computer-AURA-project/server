package com.AURA.AURA_Service.cleanup.domain;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScanJob;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cleanup_jobs")
public class CleanupJob {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cleanup_job_id") private Long cleanupJobId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scan_job_id") private ScanJob scanJob;
	@Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false) private ActionType actionType;
	@Enumerated(EnumType.STRING) @Column(name = "job_status", nullable = false) private JobStatus jobStatus;
	@Column(name = "selected_mail_count", nullable = false) private Integer selectedMailCount;
	@Column(name = "selected_drive_count", nullable = false) private Integer selectedDriveCount;
	@Column(name = "total_selected_bytes", nullable = false) private Long totalSelectedBytes;
	@Column(name = "success_item_count", nullable = false) private Integer successItemCount;
	@Column(name = "failed_item_count", nullable = false) private Integer failedItemCount;
	@Column(name = "progress_percent", nullable = false, precision = 5, scale = 2) private BigDecimal progressPercent;
	@Column(name = "approved_at", nullable = false) private LocalDateTime approvedAt;
	@Column(name = "completed_at") private LocalDateTime completedAt;
	@Column(name = "error_message", length = 500) private String errorMessage;
	@CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
	@UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected CleanupJob() { }

	public static CleanupJob create(User user, ScanJob scanJob, ActionType actionType, int selectedMailCount,
		int selectedDriveCount, long totalSelectedBytes, LocalDateTime approvedAt) {
		CleanupJob cleanupJob = new CleanupJob();
		cleanupJob.user = user;
		cleanupJob.scanJob = scanJob;
		cleanupJob.actionType = actionType;
		cleanupJob.jobStatus = JobStatus.PROCESSING;
		cleanupJob.selectedMailCount = selectedMailCount;
		cleanupJob.selectedDriveCount = selectedDriveCount;
		cleanupJob.totalSelectedBytes = totalSelectedBytes;
		cleanupJob.successItemCount = 0;
		cleanupJob.failedItemCount = 0;
		cleanupJob.progressPercent = new BigDecimal("0.00");
		cleanupJob.approvedAt = approvedAt;
		return cleanupJob;
	}

	public void retryFailedItems(int retryItemCount) {
		this.jobStatus = JobStatus.PROCESSING;
		this.failedItemCount = Math.max(0, this.failedItemCount - retryItemCount);
		this.completedAt = null;
		this.errorMessage = null;
		updateProgressBySuccessCount();
	}

	public Long getCleanupJobId() { return cleanupJobId; }
	public Long getScanJobId() { return scanJob == null ? null : scanJob.getScanJobId(); }
	public ActionType getActionType() { return actionType; }
	public JobStatus getJobStatus() { return jobStatus; }
	public Integer getSelectedMailCount() { return selectedMailCount; }
	public Integer getSelectedDriveCount() { return selectedDriveCount; }
	public Long getTotalSelectedBytes() { return totalSelectedBytes; }
	public Integer getSuccessItemCount() { return successItemCount; }
	public Integer getFailedItemCount() { return failedItemCount; }
	public BigDecimal getProgressPercent() { return progressPercent; }
	public String getErrorMessage() { return errorMessage; }
	public LocalDateTime getApprovedAt() { return approvedAt; }
	public LocalDateTime getCompletedAt() { return completedAt; }

	private void updateProgressBySuccessCount() {
		int totalItemCount = selectedMailCount + selectedDriveCount;
		if (totalItemCount <= 0) {
			this.progressPercent = new BigDecimal("0.00");
			return;
		}
		this.progressPercent = BigDecimal.valueOf(successItemCount)
			.multiply(new BigDecimal("100.00"))
			.divide(BigDecimal.valueOf(totalItemCount), 2, RoundingMode.HALF_UP);
	}

	public enum ActionType {
		MOVE_TO_TRASH,
		PERMANENT_DELETE,
		EMPTY_TRASH
	}

	public enum JobStatus {
		PENDING,
		PROCESSING,
		COMPLETED,
		FAILED,
		PARTIAL_FAILED,
		CANCELED
	}
}

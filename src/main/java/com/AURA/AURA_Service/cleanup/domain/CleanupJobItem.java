package com.AURA.AURA_Service.cleanup.domain;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "cleanup_job_items",
	uniqueConstraints = @UniqueConstraint(name = "uk_cleanup_job_item_snapshot",
		columnNames = {"cleanup_job_id", "snapshot_item_key"}))
public class CleanupJobItem {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cleanup_item_id") private Long cleanupItemId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cleanup_job_id", nullable = false) private CleanupJob cleanupJob;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "candidate_id") private AnalysisCandidate candidate;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "item_id") private ScannedItem item;
	@Enumerated(EnumType.STRING) @Column(name = "item_source", nullable = false) private ItemSource itemSource;
	@Column(name = "external_item_id", nullable = false, length = 255) private String externalItemId;
	@Column(name = "snapshot_item_key", nullable = false, length = 300) private String snapshotItemKey;
	@Column(name = "snapshot_title", length = 500) private String snapshotTitle;
	@Column(name = "snapshot_size_bytes", nullable = false) private Long snapshotSizeBytes;
	@Enumerated(EnumType.STRING) @Column(name = "process_status", nullable = false) private ProcessStatus processStatus;
	@Column(name = "failure_reason", length = 500) private String failureReason;
	@Column(name = "processed_at") private LocalDateTime processedAt;
	@CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

	protected CleanupJobItem() { }

	public static CleanupJobItem snapshot(CleanupJob cleanupJob, AnalysisCandidate candidate) {
		ScannedItem item = candidate.getScannedItem();
		CleanupJobItem cleanupJobItem = new CleanupJobItem();
		cleanupJobItem.cleanupJob = cleanupJob;
		cleanupJobItem.candidate = candidate;
		cleanupJobItem.item = item;
		cleanupJobItem.itemSource = item.getItemSource();
		cleanupJobItem.externalItemId = item.getExternalItemId();
		cleanupJobItem.snapshotItemKey = item.getClientItemKey();
		cleanupJobItem.snapshotTitle = item.getTitle();
		cleanupJobItem.snapshotSizeBytes = item.getEstimatedReclaimBytes();
		cleanupJobItem.processStatus = ProcessStatus.PENDING;
		return cleanupJobItem;
	}

	public static CleanupJobItem directSnapshot(CleanupJob cleanupJob, ScannedItem item, ItemSource itemSource,
		String externalItemId, String snapshotTitle, long snapshotSizeBytes) {
		CleanupJobItem cleanupJobItem = new CleanupJobItem();
		cleanupJobItem.cleanupJob = cleanupJob;
		cleanupJobItem.item = item;
		cleanupJobItem.itemSource = itemSource;
		cleanupJobItem.externalItemId = externalItemId;
		cleanupJobItem.snapshotItemKey = itemSource.name() + ":" + externalItemId;
		cleanupJobItem.snapshotTitle = snapshotTitle;
		cleanupJobItem.snapshotSizeBytes = snapshotSizeBytes;
		cleanupJobItem.processStatus = ProcessStatus.PENDING;
		return cleanupJobItem;
	}

	public void retryPending() {
		this.processStatus = ProcessStatus.PENDING;
		this.failureReason = null;
		this.processedAt = null;
	}

	public void markSuccess(LocalDateTime processedAt) {
		this.processStatus = ProcessStatus.SUCCESS;
		this.failureReason = null;
		this.processedAt = processedAt;
	}

	public void markFailed(String failureReason, LocalDateTime processedAt) {
		this.processStatus = ProcessStatus.FAILED;
		this.failureReason = trimFailureReason(failureReason);
		this.processedAt = processedAt;
	}

	public void markSkipped(String failureReason, LocalDateTime processedAt) {
		this.processStatus = ProcessStatus.SKIPPED;
		this.failureReason = trimFailureReason(failureReason);
		this.processedAt = processedAt;
	}

	public Long getCleanupItemId() { return cleanupItemId; }
	public ItemSource getItemSource() { return itemSource; }
	public String getExternalItemId() { return externalItemId; }
	public String getSnapshotItemKey() { return snapshotItemKey; }
	public String getSnapshotTitle() { return snapshotTitle; }
	public Long getSnapshotSizeBytes() { return snapshotSizeBytes; }
	public ProcessStatus getProcessStatus() { return processStatus; }
	public String getFailureReason() { return failureReason; }
	public LocalDateTime getProcessedAt() { return processedAt; }

	private String trimFailureReason(String failureReason) {
		if (failureReason == null || failureReason.isBlank()) return "Cleanup item processing failed.";
		return failureReason.length() > 500 ? failureReason.substring(0, 500) : failureReason;
	}

	public enum ProcessStatus {
		PENDING,
		SUCCESS,
		FAILED,
		SKIPPED
	}
}

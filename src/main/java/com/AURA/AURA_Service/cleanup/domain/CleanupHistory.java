package com.AURA.AURA_Service.cleanup.domain;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.cleanup.domain.CleanupJob.ActionType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "cleanup_histories")
public class CleanupHistory {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id") private Long historyId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
	@OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "cleanup_job_id", nullable = false, unique = true) private CleanupJob cleanupJob;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scan_job_id") private ScanJob scanJob;
	@Enumerated(EnumType.STRING) @Column(name = "action_type", nullable = false) private ActionType actionType;
	@Column(name = "cleaned_item_count", nullable = false) private Integer cleanedItemCount;
	@Column(name = "reclaimed_bytes", nullable = false) private Long reclaimedBytes;
	@Column(name = "remaining_drive_bytes") private Long remainingDriveBytes;
	@Column(name = "completed_at", nullable = false) private LocalDateTime completedAt;

	protected CleanupHistory() { }
}

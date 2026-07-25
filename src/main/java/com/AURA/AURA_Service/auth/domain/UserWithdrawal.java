package com.AURA.AURA_Service.auth.domain;

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
import java.time.LocalDateTime;

@Entity
@Table(name = "user_withdrawals")
public class UserWithdrawal {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "withdrawal_id") private Long withdrawalId;
	@ManyToOne @JoinColumn(name = "user_id", nullable = false) private User user;
	@Column(length = 500) private String reason;
	@Column(name = "google_disconnected", nullable = false) private boolean googleDisconnected;
	@Column(name = "token_deleted", nullable = false) private boolean tokenDeleted;
	@Enumerated(EnumType.STRING) @Column(name = "scan_data_policy", nullable = false) private DataPolicy scanDataPolicy;
	@Enumerated(EnumType.STRING) @Column(name = "history_data_policy", nullable = false) private DataPolicy historyDataPolicy;
	@Enumerated(EnumType.STRING) @Column(name = "processed_status", nullable = false) private ProcessedStatus processedStatus = ProcessedStatus.PENDING;
	@Column(name = "failure_reason", length = 500) private String failureReason;
	@Column(name = "withdrawn_at", nullable = false) private LocalDateTime withdrawnAt;
	@Column(name = "processed_at") private LocalDateTime processedAt;

	protected UserWithdrawal() { }

	public static UserWithdrawal request(User user, String reason, DataPolicy scanDataPolicy, DataPolicy historyDataPolicy,
		LocalDateTime withdrawnAt) {
		UserWithdrawal withdrawal = new UserWithdrawal();
		withdrawal.user = user;
		withdrawal.reason = reason;
		withdrawal.scanDataPolicy = scanDataPolicy;
		withdrawal.historyDataPolicy = historyDataPolicy;
		withdrawal.withdrawnAt = withdrawnAt;
		return withdrawal;
	}

	public void complete(boolean googleDisconnected, boolean tokenDeleted, LocalDateTime processedAt) {
		this.googleDisconnected = googleDisconnected;
		this.tokenDeleted = tokenDeleted;
		this.processedStatus = ProcessedStatus.COMPLETED;
		this.processedAt = processedAt;
	}

	public Long getWithdrawalId() { return withdrawalId; }
	public boolean isGoogleDisconnected() { return googleDisconnected; }
	public boolean isTokenDeleted() { return tokenDeleted; }
	public ProcessedStatus getProcessedStatus() { return processedStatus; }
	public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
	public LocalDateTime getProcessedAt() { return processedAt; }

	public enum DataPolicy { DELETE, ANONYMIZE, KEEP }
	public enum ProcessedStatus { PENDING, COMPLETED, FAILED }
}

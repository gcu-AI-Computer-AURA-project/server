package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.UserWithdrawal;
import com.AURA.AURA_Service.auth.domain.UserWithdrawal.ProcessedStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record UserWithdrawalResponse(
	@JsonProperty("withdrawal_id") Long withdrawalId,
	@JsonProperty("processed_status") ProcessedStatus processedStatus,
	@JsonProperty("google_disconnected") boolean googleDisconnected,
	@JsonProperty("token_deleted") boolean tokenDeleted,
	@JsonProperty("withdrawn_at") LocalDateTime withdrawnAt,
	@JsonProperty("processed_at") LocalDateTime processedAt
) {
	public static UserWithdrawalResponse from(UserWithdrawal withdrawal) {
		return new UserWithdrawalResponse(
			withdrawal.getWithdrawalId(),
			withdrawal.getProcessedStatus(),
			withdrawal.isGoogleDisconnected(),
			withdrawal.isTokenDeleted(),
			withdrawal.getWithdrawnAt(),
			withdrawal.getProcessedAt()
		);
	}
}

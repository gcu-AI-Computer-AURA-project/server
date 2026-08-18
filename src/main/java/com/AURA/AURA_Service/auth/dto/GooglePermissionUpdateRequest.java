package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record GooglePermissionUpdateRequest(
	@NotNull(message = "연결 여부는 필수입니다.")
	@JsonProperty("is_connected")
	Boolean isConnected
) {
}

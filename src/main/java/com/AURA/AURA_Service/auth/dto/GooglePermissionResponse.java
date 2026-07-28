package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record GooglePermissionResponse(
	List<PermissionResponse> permissions
) {
	public static GooglePermissionResponse from(List<GooglePermission> permissions) {
		return new GooglePermissionResponse(permissions.stream()
			.map(PermissionResponse::from)
			.toList());
	}

	public record PermissionResponse(
		@JsonProperty("service_type") ServiceType serviceType,
		@JsonProperty("permission_status") PermissionStatus permissionStatus,
		@JsonProperty("connected_at") LocalDateTime connectedAt,
		@JsonProperty("last_checked_at") LocalDateTime lastCheckedAt
	) {
		public static PermissionResponse from(GooglePermission permission) {
			return new PermissionResponse(
				permission.getServiceType(),
				permission.getPermissionStatus(),
				permission.getConnectedAt(),
				permission.getLastCheckedAt()
			);
		}
	}
}

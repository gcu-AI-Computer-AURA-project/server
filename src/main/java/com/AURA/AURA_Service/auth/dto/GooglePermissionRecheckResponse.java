package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record GooglePermissionRecheckResponse(
	@JsonProperty("gmail_status") PermissionStatus gmailStatus,
	@JsonProperty("drive_status") PermissionStatus driveStatus,
	@JsonProperty("checked_at") LocalDateTime checkedAt
) { }

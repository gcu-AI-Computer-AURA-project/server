package com.AURA.AURA_Service.announcement.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AnnouncementReadResponse(
	@JsonProperty("announcement_id")
	Long announcementId,
	@JsonProperty("read_at")
	LocalDateTime readAt
) {
}

package com.AURA.AURA_Service.announcement.dto;

import com.AURA.AURA_Service.announcement.domain.Announcement;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record AnnouncementDetailResponse(
	@JsonProperty("announcement_id")
	Long announcementId,
	String title,
	String category,
	String content,
	@JsonProperty("is_pinned")
	boolean isPinned,
	@JsonProperty("is_read")
	boolean isRead,
	@JsonProperty("published_at")
	LocalDateTime publishedAt,
	@JsonProperty("read_at")
	LocalDateTime readAt
) {
	public static AnnouncementDetailResponse from(Announcement announcement, LocalDateTime readAt) {
		return new AnnouncementDetailResponse(
			announcement.getAnnouncementId(),
			announcement.getTitle(),
			announcement.getCategory().name(),
			announcement.getContent(),
			announcement.isPinned(),
			readAt != null,
			announcement.getPublishedAt(),
			readAt
		);
	}
}

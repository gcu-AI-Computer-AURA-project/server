package com.AURA.AURA_Service.announcement.dto;

import com.AURA.AURA_Service.announcement.domain.Announcement;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Set;

public record AnnouncementListItemResponse(
	@JsonProperty("announcement_id")
	Long announcementId,
	String title,
	String category,
	@JsonProperty("is_pinned")
	boolean isPinned,
	@JsonProperty("is_read")
	boolean isRead,
	@JsonProperty("published_at")
	LocalDateTime publishedAt
) {
	public static AnnouncementListItemResponse from(Announcement announcement, Set<Long> readAnnouncementIds) {
		return new AnnouncementListItemResponse(
			announcement.getAnnouncementId(),
			announcement.getTitle(),
			announcement.getCategory().name(),
			announcement.isPinned(),
			readAnnouncementIds.contains(announcement.getAnnouncementId()),
			announcement.getPublishedAt()
		);
	}
}

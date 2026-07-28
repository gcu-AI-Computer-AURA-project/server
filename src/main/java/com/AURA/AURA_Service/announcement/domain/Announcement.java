package com.AURA.AURA_Service.announcement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "announcement_id") private Long announcementId;
	@Column(nullable = false, length = 200) private String title;
	@Enumerated(EnumType.STRING) @Column(nullable = false) private AnnouncementCategory category = AnnouncementCategory.SERVICE;
	@Column(nullable = false, columnDefinition = "TEXT") private String content;
	@Column(name = "is_pinned", nullable = false) private boolean isPinned;
	@Column(name = "published_at", nullable = false, insertable = false, updatable = false) private LocalDateTime publishedAt;
	@Column(name = "created_at", nullable = false, insertable = false, updatable = false) private LocalDateTime createdAt;
	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false) private LocalDateTime updatedAt;

	protected Announcement() { }

	public Long getAnnouncementId() { return announcementId; }
	public String getTitle() { return title; }
	public AnnouncementCategory getCategory() { return category; }
	public String getContent() { return content; }
	public boolean isPinned() { return isPinned; }
	public LocalDateTime getPublishedAt() { return publishedAt; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}

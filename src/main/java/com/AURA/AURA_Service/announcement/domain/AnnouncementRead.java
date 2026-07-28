package com.AURA.AURA_Service.announcement.domain;

import com.AURA.AURA_Service.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
	name = "announcement_reads",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_announcement_reads_user_announcement",
		columnNames = {"user_id", "announcement_id"}
	)
)
public class AnnouncementRead {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "read_id") private Long readId;
	@ManyToOne @JoinColumn(name = "user_id", nullable = false) private User user;
	@ManyToOne @JoinColumn(name = "announcement_id", nullable = false) private Announcement announcement;
	@CreationTimestamp @Column(name = "read_at", nullable = false, updatable = false) private LocalDateTime readAt;

	protected AnnouncementRead() { }

	public AnnouncementRead(User user, Announcement announcement) {
		this.user = user;
		this.announcement = announcement;
	}

	public Announcement getAnnouncement() { return announcement; }
	public LocalDateTime getReadAt() { return readAt; }
}

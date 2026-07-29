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
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notifications")
public class Notification {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id") private Long notificationId;
	@ManyToOne @JoinColumn(name = "user_id") private User user;
	@Column(name = "scan_job_id") private Long scanJobId;
	@Column(name = "cleanup_job_id") private Long cleanupJobId;
	@Enumerated(EnumType.STRING) @Column(name = "notification_type", nullable = false) private NotificationType notificationType;
	@Column(nullable = false, length = 150) private String title;
	@Column(nullable = false, length = 500) private String message;
	@Enumerated(EnumType.STRING) @Column(name = "target_screen", nullable = false) private TargetScreen targetScreen = TargetScreen.HOME;
	@Column(name = "is_read", nullable = false) private boolean isRead;
	@CreationTimestamp @Column(name = "sent_at", nullable = false) private LocalDateTime sentAt;
	@Column(name = "read_at") private LocalDateTime readAt;

	protected Notification() { }

	/**
	 * 알림 읽음 처리 메소드
	 * 읽지 않은 알림을 사용자가 확인한 시간으로 기록한다.
	 *
	 * @return : 없음
	 * @since : 2026.07.29
	 * @version : 0.0.1
	 * @author : 정효림
	 */
	public void markAsRead(LocalDateTime readAt) {
		if (this.isRead) {
			return;
		}
		this.isRead = true;
		this.readAt = readAt;
	}

	public Long getNotificationId() { return notificationId; }
	public Long getScanJobId() { return scanJobId; }
	public Long getCleanupJobId() { return cleanupJobId; }
	public NotificationType getNotificationType() { return notificationType; }
	public String getTitle() { return title; }
	public String getMessage() { return message; }
	public TargetScreen getTargetScreen() { return targetScreen; }
	public boolean isRead() { return isRead; }
	public LocalDateTime getSentAt() { return sentAt; }
	public LocalDateTime getReadAt() { return readAt; }
}

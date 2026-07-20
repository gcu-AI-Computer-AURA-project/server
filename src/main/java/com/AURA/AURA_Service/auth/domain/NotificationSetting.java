package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_settings")
public class NotificationSetting {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_setting_id") private Long notificationSettingId;
	@OneToOne @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
	@Column(name = "is_scan_complete_enabled", nullable = false) private boolean isScanCompleteEnabled = true;
	@Column(name = "is_scan_recommend_enabled", nullable = false) private boolean isScanRecommendEnabled = true;

	protected NotificationSetting() { }
	public NotificationSetting(User user) { this.user = user; }
}

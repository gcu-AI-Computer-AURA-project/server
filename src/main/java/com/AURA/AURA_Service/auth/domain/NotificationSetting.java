package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "notification_settings")
public class NotificationSetting {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_setting_id") private Long notificationSettingId;
	@OneToOne @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
	@Column(name = "is_scan_complete_enabled", nullable = false) private boolean isScanCompleteEnabled = true;
	@Column(name = "is_scan_recommend_enabled", nullable = false) private boolean isScanRecommendEnabled = true;
	@UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected NotificationSetting() { }
	public NotificationSetting(User user) { this.user = user; }

	/**
	 * 알림 수신 설정 갱신 메소드
	 * 사용자가 설정 화면에서 변경한 알림 유형별 수신 여부를 저장하기 위해 엔티티 상태를 변경한다.
	 *
	 * @return : 없음
	 * @since : 2026.07.23
	 * @version : 0.0.1
	 * @author : 정효림
	 */
	public void update(boolean scanCompleteEnabled, boolean scanRecommendEnabled) {
		this.isScanCompleteEnabled = scanCompleteEnabled;
		this.isScanRecommendEnabled = scanRecommendEnabled;
	}

	public boolean isScanCompleteEnabled() { return isScanCompleteEnabled; }
	public boolean isScanRecommendEnabled() { return isScanRecommendEnabled; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}

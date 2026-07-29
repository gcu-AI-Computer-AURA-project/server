package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "fcm_tokens")
public class FcmToken {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "fcm_token_id") private Long fcmTokenId;
	@ManyToOne @JoinColumn(name = "user_id", nullable = false) private User user;
	@Column(name = "device_identifier", length = 150) private String deviceIdentifier;
	@Column(name = "fcm_token", nullable = false, unique = true, length = 500) private String fcmToken;
	@Column(name = "is_active", nullable = false) private boolean isActive = true;
	@Column(name = "last_used_at") private LocalDateTime lastUsedAt;
	@CreationTimestamp @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
	@UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected FcmToken() { }

	public FcmToken(User user, String fcmToken, String deviceIdentifier) {
		this.user = user;
		this.fcmToken = fcmToken;
		this.deviceIdentifier = deviceIdentifier;
	}

	/**
	 * FCM 토큰 등록 정보 갱신 메소드
	 * 앱 재설치 또는 토큰 재발급 시 같은 토큰의 사용자와 기기 식별자를 최신 상태로 맞춘다.
	 *
	 * @return : 없음
	 * @since : 2026.07.29
	 * @version : 0.0.1
	 * @author : 정효림
	 */
	public void refresh(User user, String deviceIdentifier) {
		this.user = user;
		this.deviceIdentifier = deviceIdentifier;
		this.isActive = true;
	}

	public Long getFcmTokenId() { return fcmTokenId; }
	public boolean isActive() { return isActive; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }
}

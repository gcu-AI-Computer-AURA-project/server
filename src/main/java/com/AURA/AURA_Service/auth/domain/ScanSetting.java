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
@Table(name = "scan_settings")
public class ScanSetting {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "setting_id") private Long settingId;
	@OneToOne @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;

	protected ScanSetting() { }
}

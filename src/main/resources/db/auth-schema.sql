CREATE TABLE IF NOT EXISTS users (
	user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	google_provider_id VARCHAR(100) NULL,
	email VARCHAR(150) NULL,
	display_name VARCHAR(100) NULL,
	profile_image_url VARCHAR(500) NULL,
	account_status ENUM('ACTIVE', 'DISCONNECTED', 'WITHDRAWN') NOT NULL DEFAULT 'ACTIVE',
	last_login_at DATETIME NULL,
	withdrawn_at DATETIME NULL,
	anonymized_at DATETIME NULL,
	anonymous_user_key VARCHAR(64) NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (user_id),
	UNIQUE KEY uk_users_google_provider_id (google_provider_id),
	UNIQUE KEY uk_users_email (email),
	UNIQUE KEY uk_users_anonymous_user_key (anonymous_user_key),
	KEY idx_users_account_status (account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS oauth_tokens (
	token_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	encrypted_refresh_token VARCHAR(1000) NULL,
	access_token_expires_at DATETIME NULL,
	scope_text TEXT NULL,
	token_status ENUM('VALID', 'EXPIRED', 'REVOKED', 'REFRESH_FAILED') NOT NULL DEFAULT 'VALID',
	last_refreshed_at DATETIME NULL,
	refresh_failed_at DATETIME NULL,
	failure_reason VARCHAR(500) NULL,
	revoked_at DATETIME NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (token_id),
	UNIQUE KEY uk_oauth_tokens_user_id (user_id),
	KEY idx_oauth_tokens_token_status (token_status),
	KEY idx_oauth_tokens_revoked_at (revoked_at),
	CONSTRAINT fk_oauth_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_consents (
	consent_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	is_privacy_agreed TINYINT(1) NOT NULL DEFAULT 0,
	is_ai_analysis_agreed TINYINT(1) NOT NULL DEFAULT 0,
	is_metadata_only_agreed TINYINT(1) NOT NULL DEFAULT 0,
	is_user_approval_required_agreed TINYINT(1) NOT NULL DEFAULT 0,
	consent_version VARCHAR(30) NOT NULL DEFAULT 'v1',
	consented_at DATETIME NULL,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (consent_id),
	UNIQUE KEY uk_user_consents_user_id (user_id),
	CONSTRAINT fk_user_consents_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_settings (
	notification_setting_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	is_scan_complete_enabled TINYINT(1) NOT NULL DEFAULT 1,
	is_scan_recommend_enabled TINYINT(1) NOT NULL DEFAULT 1,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (notification_setting_id),
	UNIQUE KEY uk_notification_settings_user_id (user_id),
	CONSTRAINT fk_notification_settings_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scan_settings (
	setting_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	scan_source ENUM('MAIL', 'DRIVE_ALL', 'DRIVE_FOLDER', 'MAIL_AND_DRIVE') NOT NULL DEFAULT 'MAIL_AND_DRIVE',
	drive_folder_id VARCHAR(255) NULL,
	include_subfolders TINYINT(1) NOT NULL DEFAULT 1,
	last_opened_before_months INT UNSIGNED NULL,
	last_modified_before_months INT UNSIGNED NULL,
	created_before_months INT UNSIGNED NULL,
	exclude_recent_days INT UNSIGNED NOT NULL DEFAULT 30,
	include_keywords JSON NULL,
	exclude_keywords JSON NULL,
	file_extensions JSON NULL,
	include_mail_attachment_size TINYINT(1) NOT NULL DEFAULT 1,
	apply_recent_conditions TINYINT(1) NOT NULL DEFAULT 1,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (setting_id),
	UNIQUE KEY uk_scan_settings_user_id (user_id),
	CONSTRAINT fk_scan_settings_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_withdrawals (
	withdrawal_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	reason VARCHAR(500) NULL,
	google_disconnected TINYINT(1) NOT NULL DEFAULT 0,
	token_deleted TINYINT(1) NOT NULL DEFAULT 0,
	scan_data_policy ENUM('DELETE', 'ANONYMIZE', 'KEEP') NOT NULL DEFAULT 'ANONYMIZE',
	history_data_policy ENUM('DELETE', 'ANONYMIZE', 'KEEP') NOT NULL DEFAULT 'ANONYMIZE',
	processed_status ENUM('PENDING', 'COMPLETED', 'FAILED') NOT NULL DEFAULT 'PENDING',
	failure_reason VARCHAR(500) NULL,
	withdrawn_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	processed_at DATETIME NULL,
	PRIMARY KEY (withdrawal_id),
	KEY idx_user_withdrawals_user_withdrawn_at (user_id, withdrawn_at),
	KEY idx_user_withdrawals_processed_status (processed_status),
	CONSTRAINT fk_user_withdrawals_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

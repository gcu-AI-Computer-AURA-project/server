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

CREATE TABLE IF NOT EXISTS google_permissions (
	permission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	service_type ENUM('GMAIL', 'DRIVE') NOT NULL,
	permission_status ENUM('CONNECTED', 'DENIED', 'EXPIRED', 'RECONNECT_REQUIRED', 'DISCONNECTED') NOT NULL DEFAULT 'CONNECTED',
	scope_text TEXT NULL,
	connected_at DATETIME NULL,
	disconnected_at DATETIME NULL,
	last_checked_at DATETIME NULL,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (permission_id),
	UNIQUE KEY uk_google_permissions_user_service (user_id, service_type),
	KEY idx_google_permissions_status (permission_status),
	CONSTRAINT fk_google_permissions_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
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

CREATE TABLE IF NOT EXISTS fcm_tokens (
	fcm_token_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	device_identifier VARCHAR(150) NULL,
	fcm_token VARCHAR(500) NOT NULL,
	is_active TINYINT(1) NOT NULL DEFAULT 1,
	last_used_at DATETIME NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (fcm_token_id),
	UNIQUE KEY uk_fcm_tokens_fcm_token (fcm_token),
	KEY idx_fcm_tokens_user_active (user_id, is_active),
	CONSTRAINT fk_fcm_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notifications (
	notification_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NULL,
	scan_job_id BIGINT UNSIGNED NULL,
	cleanup_job_id BIGINT UNSIGNED NULL,
	notification_type ENUM('SCAN_COMPLETED', 'SCAN_RECOMMENDED', 'DRIVE_STORAGE_LOW', 'CLEANUP_COMPLETED') NOT NULL,
	title VARCHAR(150) NOT NULL,
	message VARCHAR(500) NOT NULL,
	target_screen ENUM('HOME', 'ANALYSIS_SUMMARY', 'STATISTICS', 'CLEANUP_RESULT') NOT NULL DEFAULT 'HOME',
	is_read TINYINT(1) NOT NULL DEFAULT 0,
	sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	read_at DATETIME NULL,
	PRIMARY KEY (notification_id),
	KEY idx_notifications_user_sent_at (user_id, sent_at),
	KEY idx_notifications_notification_type (notification_type),
	CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
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

CREATE TABLE IF NOT EXISTS announcements (
	announcement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	title VARCHAR(200) NOT NULL,
	category ENUM('POLICY', 'SERVICE', 'FEATURE') NOT NULL DEFAULT 'SERVICE',
	content TEXT NOT NULL,
	is_pinned TINYINT(1) NOT NULL DEFAULT 0,
	published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (announcement_id),
	KEY idx_announcements_category (category),
	KEY idx_announcements_pinned_published_at (is_pinned, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS announcement_reads (
	read_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NOT NULL,
	announcement_id BIGINT UNSIGNED NOT NULL,
	read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (read_id),
	UNIQUE KEY uk_announcement_reads_user_announcement (user_id, announcement_id),
	KEY idx_announcement_reads_announcement_id (announcement_id),
	CONSTRAINT fk_announcement_reads_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
	CONSTRAINT fk_announcement_reads_announcement FOREIGN KEY (announcement_id) REFERENCES announcements (announcement_id) ON DELETE CASCADE
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

CREATE TABLE IF NOT EXISTS scan_jobs (
	scan_job_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	user_id BIGINT UNSIGNED NULL,
	setting_id BIGINT UNSIGNED NULL,
	job_status ENUM('PENDING', 'SCANNING', 'ANALYZING', 'COMPLETED', 'FAILED', 'CANCELED', 'PARTIAL_FAILED') NOT NULL DEFAULT 'PENDING',
	scan_source ENUM('MAIL', 'DRIVE_ALL', 'DRIVE_FOLDER', 'MAIL_AND_DRIVE') NOT NULL,
	condition_snapshot_json JSON NOT NULL,
	progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
	mail_scanned_count INT UNSIGNED NOT NULL DEFAULT 0,
	drive_scanned_count INT UNSIGNED NOT NULL DEFAULT 0,
	candidate_count INT UNSIGNED NOT NULL DEFAULT 0,
	protected_count INT UNSIGNED NOT NULL DEFAULT 0,
	estimated_reclaim_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
	error_message VARCHAR(500) NULL,
	started_at DATETIME NULL,
	completed_at DATETIME NULL,
	canceled_at DATETIME NULL,
	deleted_at DATETIME NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (scan_job_id),
	KEY idx_scan_jobs_user_created_at (user_id, created_at),
	KEY idx_scan_jobs_job_status (job_status),
	CONSTRAINT fk_scan_jobs_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL,
	CONSTRAINT fk_scan_jobs_setting FOREIGN KEY (setting_id) REFERENCES scan_settings (setting_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS scanned_items (
	item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	scan_job_id BIGINT UNSIGNED NOT NULL,
	user_id BIGINT UNSIGNED NULL,
	item_source ENUM('GMAIL', 'DRIVE') NOT NULL,
	external_item_id VARCHAR(255) NULL,
	parent_external_id VARCHAR(255) NULL,
	folder_path VARCHAR(1000) NULL,
	title VARCHAR(500) NULL,
	sender_domain VARCHAR(255) NULL,
	label_text VARCHAR(500) NULL,
	snippet VARCHAR(500) NULL,
	mime_type VARCHAR(150) NULL,
	file_extension VARCHAR(20) NULL,
	size_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
	attachment_size_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
	received_at DATETIME NULL,
	created_time DATETIME NULL,
	modified_time DATETIME NULL,
	last_opened_time DATETIME NULL,
	is_starred TINYINT(1) NOT NULL DEFAULT 0,
	is_important TINYINT(1) NOT NULL DEFAULT 0,
	has_attachment TINYINT(1) NOT NULL DEFAULT 0,
	is_shared TINYINT(1) NOT NULL DEFAULT 0,
	md5_checksum VARCHAR(100) NULL,
	owner_email VARCHAR(150) NULL,
	is_trashed TINYINT(1) NOT NULL DEFAULT 0,
	trashed_at DATETIME NULL,
	metadata_json JSON NULL,
	deleted_at DATETIME NULL,
	created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	PRIMARY KEY (item_id),
	UNIQUE KEY uk_scanned_items_job_source_external (scan_job_id, item_source, external_item_id),
	KEY idx_scanned_items_user_source (user_id, item_source),
	KEY idx_scanned_items_scan_source (scan_job_id, item_source),
	KEY idx_scanned_items_size_bytes (size_bytes),
	KEY idx_scanned_items_md5_checksum (md5_checksum),
	CONSTRAINT fk_scanned_items_scan_job FOREIGN KEY (scan_job_id) REFERENCES scan_jobs (scan_job_id) ON DELETE CASCADE,
	CONSTRAINT fk_scanned_items_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS analysis_candidates (
	candidate_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	scan_job_id BIGINT UNSIGNED NOT NULL,
	item_id BIGINT UNSIGNED NOT NULL,
	category ENUM('PROMOTION_MAIL', 'OLD_MAIL', 'DUPLICATE_FILE', 'OLD_DRIVE_FILE', 'LARGE_FILE', 'LOW_VALUE_ATTACHMENT', 'TEMP_OR_BACKUP', 'PROTECTED') NOT NULL,
	risk_level ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',
	priority_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
	ghost_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
	is_protected TINYINT(1) NOT NULL DEFAULT 0,
	selection_status ENUM('NONE', 'SELECTED', 'DESELECTED') NOT NULL DEFAULT 'NONE',
	selection_version INT UNSIGNED NOT NULL DEFAULT 0,
	estimated_reclaim_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
	ai_provider VARCHAR(50) NULL,
	ai_model_name VARCHAR(100) NULL,
	ai_confidence_score DECIMAL(5,2) NULL,
	semantic_tags_json JSON NULL,
	matched_conditions_json JSON NULL,
	analyzed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	PRIMARY KEY (candidate_id),
	UNIQUE KEY uk_analysis_candidates_item_id (item_id),
	KEY idx_analysis_candidates_scan_category (scan_job_id, category),
	KEY idx_analysis_candidates_scan_selection (scan_job_id, selection_status),
	KEY idx_analysis_candidates_ai_provider_model (ai_provider, ai_model_name),
	CONSTRAINT fk_analysis_candidates_scan_job FOREIGN KEY (scan_job_id) REFERENCES scan_jobs (scan_job_id) ON DELETE CASCADE,
	CONSTRAINT fk_analysis_candidates_item FOREIGN KEY (item_id) REFERENCES scanned_items (item_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

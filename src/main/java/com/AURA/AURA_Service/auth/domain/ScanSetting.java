package com.AURA.AURA_Service.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scan_settings")
public class ScanSetting {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "setting_id") private Long settingId;
	@OneToOne @JoinColumn(name = "user_id", nullable = false, unique = true) private User user;
	@Enumerated(EnumType.STRING) @Column(name = "scan_source", nullable = false) private ScanSource scanSource = ScanSource.MAIL_AND_DRIVE;
	@Column(name = "drive_folder_id") private String driveFolderId;
	@Column(name = "include_subfolders", nullable = false) private boolean includeSubfolders = true;
	@Column(name = "last_opened_before_months") private Integer lastOpenedBeforeMonths;
	@Column(name = "last_modified_before_months") private Integer lastModifiedBeforeMonths;
	@Column(name = "created_before_months") private Integer createdBeforeMonths;
	@Column(name = "exclude_recent_days", nullable = false) private Integer excludeRecentDays = 30;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "include_keywords", columnDefinition = "json") private List<String> includeKeywords;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "exclude_keywords", columnDefinition = "json") private List<String> excludeKeywords;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "file_extensions", columnDefinition = "json") private List<String> fileExtensions;
	@Column(name = "include_mail_attachment_size", nullable = false) private boolean includeMailAttachmentSize = true;
	@Column(name = "apply_recent_conditions", nullable = false) private boolean applyRecentConditions = true;
	@CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
	@UpdateTimestamp @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

	protected ScanSetting() { }

	public ScanSetting(User user) {
		this.user = user;
	}

	/**
	 * 기본 스캔 조건 갱신 메소드
	 * 사용자가 저장한 조건을 다음 스캔의 기본값으로 적용하기 위해 엔티티 상태를 변경한다.
	 *
	 * @return : 없음
	 * @since : 2026.07.22
	 * @version : 0.0.1
	 * @author : 정효림
	 */
	public void update(ScanSource scanSource, String driveFolderId, boolean includeSubfolders,
		Integer lastOpenedBeforeMonths, Integer lastModifiedBeforeMonths, Integer createdBeforeMonths,
		Integer excludeRecentDays, List<String> includeKeywords, List<String> excludeKeywords,
		List<String> fileExtensions, boolean includeMailAttachmentSize, boolean applyRecentConditions) {
		this.scanSource = scanSource;
		this.driveFolderId = driveFolderId;
		this.includeSubfolders = includeSubfolders;
		this.lastOpenedBeforeMonths = lastOpenedBeforeMonths;
		this.lastModifiedBeforeMonths = lastModifiedBeforeMonths;
		this.createdBeforeMonths = createdBeforeMonths;
		this.excludeRecentDays = excludeRecentDays;
		this.includeKeywords = includeKeywords;
		this.excludeKeywords = excludeKeywords;
		this.fileExtensions = fileExtensions;
		this.includeMailAttachmentSize = includeMailAttachmentSize;
		this.applyRecentConditions = applyRecentConditions;
	}

	public Long getSettingId() { return settingId; }
	public ScanSource getScanSource() { return scanSource; }
	public String getDriveFolderId() { return driveFolderId; }
	public boolean isIncludeSubfolders() { return includeSubfolders; }
	public Integer getLastOpenedBeforeMonths() { return lastOpenedBeforeMonths; }
	public Integer getLastModifiedBeforeMonths() { return lastModifiedBeforeMonths; }
	public Integer getCreatedBeforeMonths() { return createdBeforeMonths; }
	public Integer getExcludeRecentDays() { return excludeRecentDays; }
	public List<String> getIncludeKeywords() { return includeKeywords; }
	public List<String> getExcludeKeywords() { return excludeKeywords; }
	public List<String> getFileExtensions() { return fileExtensions; }
	public boolean isIncludeMailAttachmentSize() { return includeMailAttachmentSize; }
	public boolean isApplyRecentConditions() { return applyRecentConditions; }
	public LocalDateTime getUpdatedAt() { return updatedAt; }

	public enum ScanSource {
		MAIL,
		DRIVE_ALL,
		DRIVE_FOLDER,
		MAIL_AND_DRIVE
	}
}

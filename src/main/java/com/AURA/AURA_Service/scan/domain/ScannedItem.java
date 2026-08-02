package com.AURA.AURA_Service.scan.domain;

import com.AURA.AURA_Service.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "scanned_items")
public class ScannedItem {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "item_id") private Long itemId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "scan_job_id", nullable = false) private ScanJob scanJob;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
	@Enumerated(EnumType.STRING) @Column(name = "item_source", nullable = false) private ItemSource itemSource;
	@Column(name = "external_item_id", nullable = false, length = 255) private String externalItemId;
	@Column(name = "parent_external_id", length = 255) private String parentExternalId;
	@Column(name = "folder_path", length = 1000) private String folderPath;
	@Column(name = "title", length = 500) private String title;
	@Column(name = "sender_domain", length = 255) private String senderDomain;
	@Column(name = "label_text", length = 500) private String labelText;
	@Column(name = "snippet", length = 1000) private String snippet;
	@Column(name = "mime_type", length = 255) private String mimeType;
	@Column(name = "file_extension", length = 50) private String fileExtension;
	@Column(name = "size_bytes") private Long sizeBytes;
	@Column(name = "attachment_size_bytes") private Long attachmentSizeBytes;
	@Column(name = "received_at") private LocalDateTime receivedAt;
	@Column(name = "created_time") private LocalDateTime createdTime;
	@Column(name = "modified_time") private LocalDateTime modifiedTime;
	@Column(name = "last_opened_time") private LocalDateTime lastOpenedTime;
	@Column(name = "is_starred", nullable = false) private boolean isStarred;
	@Column(name = "is_important", nullable = false) private boolean isImportant;
	@Column(name = "has_attachment", nullable = false) private boolean hasAttachment;
	@Column(name = "is_shared", nullable = false) private boolean isShared;
	@Column(name = "md5_checksum", length = 64) private String md5Checksum;
	@Column(name = "owner_email", length = 150) private String ownerEmail;
	@Column(name = "is_trashed", nullable = false) private boolean isTrashed;
	@Column(name = "trashed_at") private LocalDateTime trashedAt;
	@JdbcTypeCode(SqlTypes.JSON) @Column(name = "metadata_json", columnDefinition = "json") private Map<String, Object> metadataJson;
	@Column(name = "deleted_at") private LocalDateTime deletedAt;
	@CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;

	protected ScannedItem() { }

	public static ScannedItem create(ScanJob scanJob, User user, ItemSource itemSource, String externalItemId,
		String parentExternalId, String folderPath, String title, String senderDomain, String labelText,
		String snippet, String mimeType, String fileExtension, Long sizeBytes, Long attachmentSizeBytes,
		LocalDateTime receivedAt, LocalDateTime createdTime, LocalDateTime modifiedTime, LocalDateTime lastOpenedTime,
		boolean isStarred, boolean isImportant, boolean hasAttachment, boolean isShared, String md5Checksum,
		String ownerEmail, boolean isTrashed, LocalDateTime trashedAt, Map<String, Object> metadataJson) {
		ScannedItem scannedItem = new ScannedItem();
		scannedItem.scanJob = scanJob;
		scannedItem.user = user;
		scannedItem.itemSource = itemSource;
		scannedItem.externalItemId = externalItemId;
		scannedItem.parentExternalId = parentExternalId;
		scannedItem.folderPath = folderPath;
		scannedItem.title = title;
		scannedItem.senderDomain = senderDomain;
		scannedItem.labelText = labelText;
		scannedItem.snippet = snippet;
		scannedItem.mimeType = mimeType;
		scannedItem.fileExtension = normalizeExtension(fileExtension);
		scannedItem.sizeBytes = defaultZero(sizeBytes);
		scannedItem.attachmentSizeBytes = defaultZero(attachmentSizeBytes);
		scannedItem.receivedAt = receivedAt;
		scannedItem.createdTime = createdTime;
		scannedItem.modifiedTime = modifiedTime;
		scannedItem.lastOpenedTime = lastOpenedTime;
		scannedItem.isStarred = isStarred;
		scannedItem.isImportant = isImportant;
		scannedItem.hasAttachment = hasAttachment;
		scannedItem.isShared = isShared;
		scannedItem.md5Checksum = blankToNull(md5Checksum);
		scannedItem.ownerEmail = blankToNull(ownerEmail);
		scannedItem.isTrashed = isTrashed;
		scannedItem.trashedAt = trashedAt;
		scannedItem.metadataJson = metadataJson;
		return scannedItem;
	}

	public Long getItemId() { return itemId; }
	public ItemSource getItemSource() { return itemSource; }
	public String getExternalItemId() { return externalItemId; }
	public String getParentExternalId() { return parentExternalId; }
	public String getFolderPath() { return folderPath; }
	public String getTitle() { return title; }
	public String getSenderDomain() { return senderDomain; }
	public String getLabelText() { return labelText; }
	public String getSnippet() { return snippet; }
	public String getMimeType() { return mimeType; }
	public String getFileExtension() { return fileExtension; }
	public Long getSizeBytes() { return sizeBytes; }
	public Long getAttachmentSizeBytes() { return attachmentSizeBytes; }
	public LocalDateTime getReceivedAt() { return receivedAt; }
	public LocalDateTime getCreatedTime() { return createdTime; }
	public LocalDateTime getModifiedTime() { return modifiedTime; }
	public LocalDateTime getLastOpenedTime() { return lastOpenedTime; }
	public boolean isStarred() { return isStarred; }
	public boolean isImportant() { return isImportant; }
	public boolean isHasAttachment() { return hasAttachment; }
	public boolean isShared() { return isShared; }
	public String getMd5Checksum() { return md5Checksum; }
	public String getOwnerEmail() { return ownerEmail; }
	public boolean isTrashed() { return isTrashed; }
	public Map<String, Object> getMetadataJson() { return metadataJson; }

	public String getClientItemKey() {
		return itemSource.name() + ":" + externalItemId;
	}

	public long getEstimatedReclaimBytes() {
		if (itemSource == ItemSource.GMAIL) return Math.max(defaultZero(sizeBytes), defaultZero(attachmentSizeBytes));
		return defaultZero(sizeBytes);
	}

	public LocalDateTime getRecentActivityTime() {
		if (lastOpenedTime != null) return lastOpenedTime;
		if (modifiedTime != null) return modifiedTime;
		if (receivedAt != null) return receivedAt;
		return createdTime;
	}

	public String toSearchText() {
		return String.join(" ",
			blankToEmpty(title),
			blankToEmpty(folderPath),
			blankToEmpty(senderDomain),
			blankToEmpty(labelText),
			blankToEmpty(snippet),
			blankToEmpty(fileExtension)
		);
	}

	private static Long defaultZero(Long value) {
		return value == null ? 0L : value;
	}

	private static String normalizeExtension(String value) {
		if (value == null || value.isBlank()) return null;
		String normalized = value.trim().toLowerCase(Locale.ROOT);
		while (normalized.startsWith(".")) normalized = normalized.substring(1);
		return normalized;
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) return null;
		return value.trim();
	}

	private static String blankToEmpty(String value) {
		return value == null ? "" : value;
	}

	public enum ItemSource {
		GMAIL,
		DRIVE
	}
}

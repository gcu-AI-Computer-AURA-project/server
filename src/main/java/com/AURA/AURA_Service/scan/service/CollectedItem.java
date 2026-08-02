package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import java.time.LocalDateTime;
import java.util.Map;

public record CollectedItem(
	ItemSource itemSource,
	String externalItemId,
	String parentExternalId,
	String folderPath,
	String title,
	String senderDomain,
	String labelText,
	String snippet,
	String mimeType,
	String fileExtension,
	Long sizeBytes,
	Long attachmentSizeBytes,
	LocalDateTime receivedAt,
	LocalDateTime createdTime,
	LocalDateTime modifiedTime,
	LocalDateTime lastOpenedTime,
	boolean isStarred,
	boolean isImportant,
	boolean hasAttachment,
	boolean isShared,
	String md5Checksum,
	String ownerEmail,
	boolean isTrashed,
	LocalDateTime trashedAt,
	Map<String, Object> metadataJson
) {
	public ScannedItem toScannedItem(ScanJob scanJob, User user) {
		return ScannedItem.create(scanJob, user, itemSource, externalItemId, parentExternalId, folderPath, title,
			senderDomain, labelText, snippet, mimeType, fileExtension, sizeBytes, attachmentSizeBytes, receivedAt,
			createdTime, modifiedTime, lastOpenedTime, isStarred, isImportant, hasAttachment, isShared, md5Checksum,
			ownerEmail, isTrashed, trashedAt, metadataJson);
	}
}

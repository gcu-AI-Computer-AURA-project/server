package com.AURA.AURA_Service.scan.dto;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.RiskLevel;
import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.SelectionStatus;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AnalysisCandidateListItemResponse(
	@JsonProperty("candidate_id")
	Long candidateId,
	@JsonProperty("item_id")
	Long itemId,
	@JsonProperty("item_source")
	ItemSource itemSource,
	@JsonProperty("external_item_id")
	String externalItemId,
	String title,
	@JsonProperty("sender_domain")
	String senderDomain,
	@JsonProperty("label_text")
	String labelText,
	String snippet,
	@JsonProperty("mime_type")
	String mimeType,
	@JsonProperty("file_extension")
	String fileExtension,
	@JsonProperty("size_bytes")
	Long sizeBytes,
	@JsonProperty("attachment_size_bytes")
	Long attachmentSizeBytes,
	@JsonProperty("received_at")
	LocalDateTime receivedAt,
	@JsonProperty("created_time")
	LocalDateTime createdTime,
	@JsonProperty("modified_time")
	LocalDateTime modifiedTime,
	@JsonProperty("last_opened_time")
	LocalDateTime lastOpenedTime,
	@JsonProperty("folder_path")
	String folderPath,
	@JsonProperty("has_attachment")
	boolean hasAttachment,
	@JsonProperty("is_starred")
	boolean isStarred,
	@JsonProperty("is_important")
	boolean isImportant,
	@JsonProperty("is_shared")
	boolean isShared,
	@JsonProperty("owner_email")
	String ownerEmail,
	@JsonProperty("md5_checksum")
	String md5Checksum,
	CandidateCategory category,
	@JsonProperty("risk_level")
	RiskLevel riskLevel,
	@JsonProperty("priority_score")
	BigDecimal priorityScore,
	@JsonProperty("ghost_score")
	BigDecimal ghostScore,
	@JsonProperty("is_protected")
	boolean isProtected,
	@JsonProperty("selection_status")
	SelectionStatus selectionStatus,
	@JsonProperty("selection_version")
	Integer selectionVersion,
	@JsonProperty("estimated_reclaim_bytes")
	Long estimatedReclaimBytes,
	@JsonProperty("semantic_tags")
	List<String> semanticTags,
	@JsonProperty("matched_conditions")
	Map<String, Object> matchedConditions,
	@JsonProperty("ai_confidence_score")
	BigDecimal aiConfidenceScore
) {
	public static AnalysisCandidateListItemResponse from(AnalysisCandidate candidate) {
		ScannedItem item = candidate.getScannedItem();
		return new AnalysisCandidateListItemResponse(
			candidate.getCandidateId(),
			item.getItemId(),
			item.getItemSource(),
			item.getExternalItemId(),
			item.getTitle(),
			item.getSenderDomain(),
			item.getLabelText(),
			item.getSnippet(),
			item.getMimeType(),
			item.getFileExtension(),
			item.getSizeBytes(),
			item.getAttachmentSizeBytes(),
			item.getReceivedAt(),
			item.getCreatedTime(),
			item.getModifiedTime(),
			item.getLastOpenedTime(),
			item.getFolderPath(),
			item.isHasAttachment(),
			item.isStarred(),
			item.isImportant(),
			item.isShared(),
			item.getOwnerEmail(),
			item.getMd5Checksum(),
			candidate.getCategory(),
			candidate.getRiskLevel(),
			candidate.getPriorityScore(),
			candidate.getGhostScore(),
			candidate.isProtected(),
			candidate.getSelectionStatus(),
			candidate.getSelectionVersion(),
			candidate.getEstimatedReclaimBytes(),
			candidate.getSemanticTags(),
			candidate.getMatchedConditions(),
			candidate.getAiConfidenceScore()
		);
	}
}

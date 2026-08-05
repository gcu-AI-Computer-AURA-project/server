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

public record AnalysisCandidateDetailResponse(
	@JsonProperty("candidate_id")
	Long candidateId,
	@JsonProperty("scan_job_id")
	Long scanJobId,
	CandidateItemResponse item,
	CandidateAnalysisResponse analysis,
	@JsonProperty("selection_status")
	SelectionStatus selectionStatus,
	@JsonProperty("selection_version")
	Integer selectionVersion
) {
	public static AnalysisCandidateDetailResponse from(AnalysisCandidate candidate) {
		return new AnalysisCandidateDetailResponse(
			candidate.getCandidateId(),
			candidate.getScanJob().getScanJobId(),
			CandidateItemResponse.from(candidate.getScannedItem()),
			CandidateAnalysisResponse.from(candidate),
			candidate.getSelectionStatus(),
			candidate.getSelectionVersion()
		);
	}

	public record CandidateItemResponse(
		@JsonProperty("item_id")
		Long itemId,
		@JsonProperty("item_source")
		ItemSource itemSource,
		@JsonProperty("external_item_id")
		String externalItemId,
		String title,
		@JsonProperty("size_bytes")
		Long sizeBytes,
		@JsonProperty("attachment_size_bytes")
		Long attachmentSizeBytes,
		@JsonProperty("sender_domain")
		String senderDomain,
		@JsonProperty("label_text")
		String labelText,
		String snippet,
		@JsonProperty("has_attachment")
		boolean hasAttachment,
		@JsonProperty("is_starred")
		boolean isStarred,
		@JsonProperty("is_important")
		boolean isImportant,
		@JsonProperty("mime_type")
		String mimeType,
		@JsonProperty("file_extension")
		String fileExtension,
		@JsonProperty("folder_path")
		String folderPath,
		@JsonProperty("created_time")
		LocalDateTime createdTime,
		@JsonProperty("modified_time")
		LocalDateTime modifiedTime,
		@JsonProperty("last_opened_time")
		LocalDateTime lastOpenedTime,
		@JsonProperty("received_at")
		LocalDateTime receivedAt,
		@JsonProperty("is_shared")
		boolean isShared,
		@JsonProperty("owner_email")
		String ownerEmail,
		@JsonProperty("md5_checksum")
		String md5Checksum,
		@JsonProperty("is_trashed")
		boolean isTrashed
	) {
		public static CandidateItemResponse from(ScannedItem item) {
			return new CandidateItemResponse(
				item.getItemId(),
				item.getItemSource(),
				item.getExternalItemId(),
				item.getTitle(),
				item.getSizeBytes(),
				item.getAttachmentSizeBytes(),
				item.getSenderDomain(),
				item.getLabelText(),
				item.getSnippet(),
				item.isHasAttachment(),
				item.isStarred(),
				item.isImportant(),
				item.getMimeType(),
				item.getFileExtension(),
				item.getFolderPath(),
				item.getCreatedTime(),
				item.getModifiedTime(),
				item.getLastOpenedTime(),
				item.getReceivedAt(),
				item.isShared(),
				item.getOwnerEmail(),
				item.getMd5Checksum(),
				item.isTrashed()
			);
		}
	}

	public record CandidateAnalysisResponse(
		CandidateCategory category,
		@JsonProperty("risk_level")
		RiskLevel riskLevel,
		@JsonProperty("priority_score")
		BigDecimal priorityScore,
		@JsonProperty("ghost_score")
		BigDecimal ghostScore,
		@JsonProperty("is_protected")
		boolean isProtected,
		@JsonProperty("estimated_reclaim_bytes")
		Long estimatedReclaimBytes,
		@JsonProperty("ai_provider")
		String aiProvider,
		@JsonProperty("ai_model_name")
		String aiModelName,
		@JsonProperty("ai_confidence_score")
		BigDecimal aiConfidenceScore,
		@JsonProperty("semantic_tags")
		List<String> semanticTags,
		@JsonProperty("matched_conditions")
		Map<String, Object> matchedConditions,
		@JsonProperty("analyzed_at")
		LocalDateTime analyzedAt
	) {
		public static CandidateAnalysisResponse from(AnalysisCandidate candidate) {
			return new CandidateAnalysisResponse(
				candidate.getCategory(),
				candidate.getRiskLevel(),
				candidate.getPriorityScore(),
				candidate.getGhostScore(),
				candidate.isProtected(),
				candidate.getEstimatedReclaimBytes(),
				candidate.getAiProvider(),
				candidate.getAiModelName(),
				candidate.getAiConfidenceScore(),
				candidate.getSemanticTags(),
				candidate.getMatchedConditions(),
				candidate.getAnalyzedAt()
			);
		}
	}
}

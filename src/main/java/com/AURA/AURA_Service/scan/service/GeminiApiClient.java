package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.scan.domain.AnalysisCandidate.CandidateCategory;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiApiClient {
	private static final int ERROR_BODY_MAX_LENGTH = 500;

	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String apiUrl;
	private final String model;
	private final Duration requestTimeout;
	private final HttpClient httpClient;

	public GeminiApiClient(@Value("${aura.ai.gemini.api-key}") String apiKey,
		@Value("${aura.ai.gemini.api-url}") String apiUrl, @Value("${aura.ai.gemini.model}") String model,
		@Value("${aura.ai.request-timeout-seconds}") long requestTimeoutSeconds) {
		this.objectMapper = new ObjectMapper();
		this.apiKey = apiKey;
		this.apiUrl = apiUrl;
		this.model = model;
		this.requestTimeout = Duration.ofSeconds(Math.max(1, requestTimeoutSeconds));
		this.httpClient = HttpClient.newBuilder().connectTimeout(this.requestTimeout).build();
	}

	public boolean isConfigured() {
		return !isBlank(apiKey) && !isBlank(apiUrl) && !isBlank(model);
	}

	public String getModel() {
		return model;
	}

	public Map<String, GeminiAnalysisResult> analyzeBatch(List<ScannedItem> items, ScanCondition condition) {
		if (!isConfigured()) throw new IllegalStateException("Gemini API configuration is empty.");
		try {
			String requestBody = objectMapper.writeValueAsString(createRequestBody(items, condition));
			HttpRequest request = HttpRequest.newBuilder(createRequestUri())
				.timeout(requestTimeout)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(requestBody))
				.build();
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				throw new IllegalStateException("Gemini API rejected request. status=" + response.statusCode()
					+ ", body=" + abbreviate(response.body()));
			}
			return parseResponse(response.body());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Gemini API request interrupted.", exception);
		} catch (IOException exception) {
			throw new IllegalStateException("Gemini API request failed.", exception);
		}
	}

	private Map<String, Object> createRequestBody(List<ScannedItem> items, ScanCondition condition) throws JsonProcessingException {
		Map<String, Object> requestBody = new LinkedHashMap<>();
		requestBody.put("model", model);
		requestBody.put("input", createPrompt(items, condition));
		requestBody.put("response_format", createResponseFormat());
		requestBody.put("generation_config", Map.of("temperature", 0, "max_output_tokens", 8192));
		return requestBody;
	}

	private Map<String, Object> createResponseFormat() {
		Map<String, Object> keywordMatchSchema = new LinkedHashMap<>();
		keywordMatchSchema.put("type", "object");
		keywordMatchSchema.put("properties", Map.of(
			"keyword", Map.of("type", "string"),
			"match_type", Map.of("type", "string"),
			"confidence_score", Map.of("type", "number")
		));
		keywordMatchSchema.put("required", List.of("keyword", "match_type", "confidence_score"));

		Map<String, Object> itemSchema = new LinkedHashMap<>();
		itemSchema.put("type", "object");
		itemSchema.put("properties", Map.of(
			"client_item_key", Map.of("type", "string"),
			"suggested_category", Map.of("type", "string"),
			"cleanup_hint", Map.of("type", "boolean"),
			"protected_hint", Map.of("type", "boolean"),
			"confidence_score", Map.of("type", "number"),
			"semantic_tags", Map.of("type", "array", "items", Map.of("type", "string")),
			"include_keyword_matches", Map.of("type", "array", "items", keywordMatchSchema),
			"exclude_keyword_matches", Map.of("type", "array", "items", keywordMatchSchema)
		));
		itemSchema.put("required", List.of("client_item_key", "suggested_category", "cleanup_hint",
			"protected_hint", "confidence_score", "semantic_tags", "include_keyword_matches",
			"exclude_keyword_matches"));

		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", Map.of("items", Map.of("type", "array", "items", itemSchema)));
		schema.put("required", List.of("items"));

		Map<String, Object> responseFormat = new LinkedHashMap<>();
		responseFormat.put("type", "text");
		responseFormat.put("mime_type", "application/json");
		responseFormat.put("schema", schema);
		return responseFormat;
	}

	private String createPrompt(List<ScannedItem> items, ScanCondition condition) throws JsonProcessingException {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("conditions", createConditionPayload(condition));
		payload.put("items", items.stream().map(this::createItemPayload).toList());
		return """
			You are AURA's metadata-only cleanup classifier.
			Read only the JSON metadata below. Do not infer from private content that is not present.
			Return JSON only. Do not include markdown, prose, reasons, explanations, or comments.
			Each output item must preserve client_item_key.
			Allowed suggested_category values are PROMOTION_MAIL, OLD_MAIL, DUPLICATE_FILE, OLD_DRIVE_FILE, LARGE_FILE, LOW_VALUE_ATTACHMENT, TEMP_OR_BACKUP, PROTECTED.
			Use include_keyword_matches and exclude_keyword_matches for direct or semantic keyword relations.
			Schema:
			{"items":[{"client_item_key":"GMAIL:id or DRIVE:id","suggested_category":"OLD_DRIVE_FILE","cleanup_hint":true,"protected_hint":false,"confidence_score":82.5,"semantic_tags":["class"],"include_keyword_matches":[{"keyword":"class","match_type":"SEMANTIC","confidence_score":84.0}],"exclude_keyword_matches":[]}]}
			Input:
			""" + objectMapper.writeValueAsString(payload);
	}

	private Map<String, Object> createConditionPayload(ScanCondition condition) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("scan_source", condition.getScanSource().name());
		payload.put("drive_folder_id", condition.getDriveFolderId());
		payload.put("include_subfolders", condition.isIncludeSubfolders());
		payload.put("last_opened_before_months", condition.getLastOpenedBeforeMonths());
		payload.put("last_modified_before_months", condition.getLastModifiedBeforeMonths());
		payload.put("created_before_months", condition.getCreatedBeforeMonths());
		payload.put("exclude_recent_days", condition.getExcludeRecentDays());
		payload.put("include_keywords", condition.getIncludeKeywords());
		payload.put("exclude_keywords", condition.getExcludeKeywords());
		payload.put("file_extensions", condition.getFileExtensions());
		payload.put("include_mail_attachment_size", condition.isIncludeMailAttachmentSize());
		payload.put("apply_recent_conditions", condition.isApplyRecentConditions());
		return payload;
	}

	private Map<String, Object> createItemPayload(ScannedItem item) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("client_item_key", item.getClientItemKey());
		payload.put("item_source", item.getItemSource().name());
		payload.put("title", item.getTitle());
		payload.put("folder_path", item.getFolderPath());
		payload.put("sender_domain", item.getSenderDomain());
		payload.put("label_text", item.getLabelText());
		payload.put("snippet", item.getSnippet());
		payload.put("mime_type", item.getMimeType());
		payload.put("file_extension", item.getFileExtension());
		payload.put("size_bytes", item.getSizeBytes());
		payload.put("attachment_size_bytes", item.getAttachmentSizeBytes());
		payload.put("received_at", toString(item.getReceivedAt()));
		payload.put("created_time", toString(item.getCreatedTime()));
		payload.put("modified_time", toString(item.getModifiedTime()));
		payload.put("last_opened_time", toString(item.getLastOpenedTime()));
		payload.put("is_starred", item.isStarred());
		payload.put("is_important", item.isImportant());
		payload.put("has_attachment", item.isHasAttachment());
		payload.put("is_shared", item.isShared());
		payload.put("owner_email", item.getOwnerEmail());
		return payload;
	}

	private Map<String, GeminiAnalysisResult> parseResponse(String body) throws JsonProcessingException {
		String outputText = extractModelOutputText(objectMapper.readTree(body));
		if (isBlank(outputText)) return Map.of();
		JsonNode root = objectMapper.readTree(stripJsonFence(outputText));
		JsonNode itemsNode = root.path("items");
		if (!itemsNode.isArray()) return Map.of();
		Map<String, GeminiAnalysisResult> results = new LinkedHashMap<>();
		for (JsonNode itemNode : itemsNode) {
			GeminiAnalysisResult result = parseResult(itemNode);
			if (!isBlank(result.clientItemKey())) results.put(result.clientItemKey(), result);
		}
		return results;
	}

	private GeminiAnalysisResult parseResult(JsonNode itemNode) {
		String clientItemKey = itemNode.path("client_item_key").asText(null);
		return new GeminiAnalysisResult(
			clientItemKey,
			parseCategory(itemNode.path("suggested_category").asText(null)),
			itemNode.path("cleanup_hint").asBoolean(false),
			itemNode.path("protected_hint").asBoolean(false),
			parseScore(itemNode.path("confidence_score")),
			parseStringList(itemNode.path("semantic_tags")),
			parseKeywordMatches(itemNode.path("include_keyword_matches")),
			parseKeywordMatches(itemNode.path("exclude_keyword_matches"))
		);
	}

	private String extractModelOutputText(JsonNode root) {
		String outputText = root.path("output_text").asText(null);
		if (!isBlank(outputText)) return outputText;
		StringBuilder builder = new StringBuilder();
		for (JsonNode stepNode : root.path("steps")) {
			if (!"model_output".equals(stepNode.path("type").asText())) continue;
			for (JsonNode contentNode : stepNode.path("content")) {
				String text = contentNode.isTextual() ? contentNode.asText() : contentNode.path("text").asText(null);
				if (!isBlank(text)) builder.append(text);
			}
		}
		return builder.toString();
	}

	private List<String> parseStringList(JsonNode arrayNode) {
		if (!arrayNode.isArray()) return List.of();
		List<String> values = new ArrayList<>();
		for (JsonNode node : arrayNode) {
			String value = node.asText(null);
			if (!isBlank(value)) values.add(value);
		}
		return values;
	}

	private List<GeminiAnalysisResult.KeywordMatch> parseKeywordMatches(JsonNode arrayNode) {
		if (!arrayNode.isArray()) return List.of();
		List<GeminiAnalysisResult.KeywordMatch> matches = new ArrayList<>();
		for (JsonNode node : arrayNode) {
			String keyword = node.path("keyword").asText(null);
			if (isBlank(keyword)) continue;
			matches.add(new GeminiAnalysisResult.KeywordMatch(
				keyword,
				node.path("match_type").asText("SEMANTIC"),
				parseScore(node.path("confidence_score"))
			));
		}
		return matches;
	}

	private CandidateCategory parseCategory(String value) {
		if (isBlank(value)) return null;
		try {
			return CandidateCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	private BigDecimal parseScore(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) return BigDecimal.ZERO;
		return BigDecimal.valueOf(node.asDouble(0.0)).setScale(2, java.math.RoundingMode.HALF_UP);
	}

	private URI createRequestUri() {
		String separator = apiUrl.contains("?") ? "&" : "?";
		return URI.create(apiUrl + separator + "key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
	}

	private String stripJsonFence(String value) {
		String stripped = value.trim();
		if (stripped.startsWith("```")) {
			stripped = stripped.replaceFirst("^```[a-zA-Z]*", "").trim();
			if (stripped.endsWith("```")) stripped = stripped.substring(0, stripped.length() - 3).trim();
		}
		return stripped;
	}

	private String toString(LocalDateTime value) {
		return value == null ? null : value.toString();
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private String abbreviate(String value) {
		if (isBlank(value)) return "";
		String normalized = value.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= ERROR_BODY_MAX_LENGTH) return normalized;
		return normalized.substring(0, ERROR_BODY_MAX_LENGTH) + "...";
	}
}

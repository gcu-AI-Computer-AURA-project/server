package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.dto.ScanSettingsOverrideRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScanCondition {
	private static final boolean DEFAULT_INCLUDE_SUBFOLDERS = true;
	private static final int DEFAULT_EXCLUDE_RECENT_DAYS = 30;
	private static final boolean DEFAULT_INCLUDE_MAIL_ATTACHMENT_SIZE = true;
	private static final boolean DEFAULT_APPLY_RECENT_CONDITIONS = true;

	private final ScanSource scanSource;
	private final String driveFolderId;
	private final boolean includeSubfolders;
	private final Integer lastOpenedBeforeMonths;
	private final Integer lastModifiedBeforeMonths;
	private final Integer createdBeforeMonths;
	private final Integer excludeRecentDays;
	private final List<String> includeKeywords;
	private final List<String> excludeKeywords;
	private final List<String> fileExtensions;
	private final boolean includeMailAttachmentSize;
	private final boolean applyRecentConditions;

	private ScanCondition(ScanSource scanSource, String driveFolderId, boolean includeSubfolders,
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

	public static ScanCondition from(ScanSetting scanSetting) {
		return new ScanCondition(
			scanSetting.getScanSource(),
			normalizeDriveFolderId(scanSetting.getScanSource(), scanSetting.getDriveFolderId()),
			scanSetting.isIncludeSubfolders(),
			scanSetting.getLastOpenedBeforeMonths(),
			scanSetting.getLastModifiedBeforeMonths(),
			scanSetting.getCreatedBeforeMonths(),
			scanSetting.getExcludeRecentDays(),
			normalizeWords(scanSetting.getIncludeKeywords(), false),
			normalizeWords(scanSetting.getExcludeKeywords(), false),
			normalizeWords(scanSetting.getFileExtensions(), true),
			scanSetting.isIncludeMailAttachmentSize(),
			scanSetting.isApplyRecentConditions()
		);
	}

	public static ScanCondition fromOverride(ScanSettingsOverrideRequest request) {
		return new ScanCondition(
			request.scanSource(),
			normalizeDriveFolderId(request.scanSource(), request.driveFolderId()),
			valueOrDefault(request.includeSubfolders(), DEFAULT_INCLUDE_SUBFOLDERS),
			request.lastOpenedBeforeMonths(),
			request.lastModifiedBeforeMonths(),
			request.createdBeforeMonths(),
			valueOrDefault(request.excludeRecentDays(), DEFAULT_EXCLUDE_RECENT_DAYS),
			normalizeWords(request.includeKeywords(), false),
			normalizeWords(request.excludeKeywords(), false),
			normalizeWords(request.fileExtensions(), true),
			valueOrDefault(request.includeMailAttachmentSize(), DEFAULT_INCLUDE_MAIL_ATTACHMENT_SIZE),
			valueOrDefault(request.applyRecentConditions(), DEFAULT_APPLY_RECENT_CONDITIONS)
		);
	}

	public static ScanCondition fromSnapshot(Map<String, Object> snapshot) {
		return new ScanCondition(
			ScanSource.valueOf(readString(snapshot, "scan_source", ScanSource.MAIL_AND_DRIVE.name())),
			readString(snapshot, "drive_folder_id", null),
			readBoolean(snapshot, "include_subfolders", DEFAULT_INCLUDE_SUBFOLDERS),
			readInteger(snapshot, "last_opened_before_months"),
			readInteger(snapshot, "last_modified_before_months"),
			readInteger(snapshot, "created_before_months"),
			readInteger(snapshot, "exclude_recent_days", DEFAULT_EXCLUDE_RECENT_DAYS),
			normalizeWords(readStringList(snapshot, "include_keywords"), false),
			normalizeWords(readStringList(snapshot, "exclude_keywords"), false),
			normalizeWords(readStringList(snapshot, "file_extensions"), true),
			readBoolean(snapshot, "include_mail_attachment_size", DEFAULT_INCLUDE_MAIL_ATTACHMENT_SIZE),
			readBoolean(snapshot, "apply_recent_conditions", DEFAULT_APPLY_RECENT_CONDITIONS)
		);
	}

	public ScanCondition merge(ScanSettingsOverrideRequest request) {
		if (request == null) return this;
		ScanSource mergedScanSource = request.scanSource() == null ? scanSource : request.scanSource();
		return new ScanCondition(
			mergedScanSource,
			normalizeDriveFolderId(mergedScanSource, request.driveFolderId() == null ? driveFolderId : request.driveFolderId()),
			valueOrDefault(request.includeSubfolders(), includeSubfolders),
			request.lastOpenedBeforeMonths() == null ? lastOpenedBeforeMonths : request.lastOpenedBeforeMonths(),
			request.lastModifiedBeforeMonths() == null ? lastModifiedBeforeMonths : request.lastModifiedBeforeMonths(),
			request.createdBeforeMonths() == null ? createdBeforeMonths : request.createdBeforeMonths(),
			valueOrDefault(request.excludeRecentDays(), excludeRecentDays),
			request.includeKeywords() == null ? includeKeywords : normalizeWords(request.includeKeywords(), false),
			request.excludeKeywords() == null ? excludeKeywords : normalizeWords(request.excludeKeywords(), false),
			request.fileExtensions() == null ? fileExtensions : normalizeWords(request.fileExtensions(), true),
			valueOrDefault(request.includeMailAttachmentSize(), includeMailAttachmentSize),
			valueOrDefault(request.applyRecentConditions(), applyRecentConditions)
		);
	}

	public Map<String, Object> toSnapshot() {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("scan_source", scanSource.name());
		snapshot.put("drive_folder_id", driveFolderId);
		snapshot.put("include_subfolders", includeSubfolders);
		snapshot.put("last_opened_before_months", lastOpenedBeforeMonths);
		snapshot.put("last_modified_before_months", lastModifiedBeforeMonths);
		snapshot.put("created_before_months", createdBeforeMonths);
		snapshot.put("exclude_recent_days", excludeRecentDays);
		snapshot.put("include_keywords", includeKeywords);
		snapshot.put("exclude_keywords", excludeKeywords);
		snapshot.put("file_extensions", fileExtensions);
		snapshot.put("include_mail_attachment_size", includeMailAttachmentSize);
		snapshot.put("apply_recent_conditions", applyRecentConditions);
		return snapshot;
	}

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

	private static List<String> normalizeWords(List<String> values, boolean isExtension) {
		if (values == null) return List.of();
		return values.stream()
			.filter(value -> value != null && !value.isBlank())
			.map(String::trim)
			.map(value -> isExtension ? normalizeExtension(value) : value)
			.distinct()
			.toList();
	}

	private static String normalizeExtension(String value) {
		String normalized = value.toLowerCase();
		while (normalized.startsWith(".")) {
			normalized = normalized.substring(1);
		}
		return normalized;
	}

	private static String normalizeDriveFolderId(ScanSource scanSource, String driveFolderId) {
		if (scanSource != ScanSource.DRIVE_FOLDER) return null;
		if (driveFolderId == null || driveFolderId.isBlank()) return null;
		return driveFolderId.trim();
	}

	private static boolean valueOrDefault(Boolean value, boolean defaultValue) {
		return value == null ? defaultValue : value;
	}

	private static int valueOrDefault(Integer value, int defaultValue) {
		return value == null ? defaultValue : value;
	}

	private static String readString(Map<String, Object> snapshot, String key, String defaultValue) {
		Object value = snapshot.get(key);
		if (value == null) return defaultValue;
		String stringValue = String.valueOf(value);
		return stringValue.isBlank() ? defaultValue : stringValue;
	}

	private static Integer readInteger(Map<String, Object> snapshot, String key) {
		Object value = snapshot.get(key);
		if (value == null) return null;
		if (value instanceof Number number) return number.intValue();
		String stringValue = String.valueOf(value);
		if (stringValue.isBlank()) return null;
		return Integer.valueOf(stringValue);
	}

	private static int readInteger(Map<String, Object> snapshot, String key, int defaultValue) {
		Integer value = readInteger(snapshot, key);
		return value == null ? defaultValue : value;
	}

	private static boolean readBoolean(Map<String, Object> snapshot, String key, boolean defaultValue) {
		Object value = snapshot.get(key);
		if (value == null) return defaultValue;
		if (value instanceof Boolean booleanValue) return booleanValue;
		return Boolean.parseBoolean(String.valueOf(value));
	}

	@SuppressWarnings("unchecked")
	private static List<String> readStringList(Map<String, Object> snapshot, String key) {
		Object value = snapshot.get(key);
		if (value instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		return List.of();
	}
}

package com.AURA.AURA_Service.storage.controller;

import com.AURA.AURA_Service.cleanup.dto.CleanupJobCreateResponse;
import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.storage.dto.StoragePermanentDeleteRequest;
import com.AURA.AURA_Service.storage.dto.StorageTrashEmptyRequest;
import com.AURA.AURA_Service.storage.dto.StorageTrashPageResponse;
import com.AURA.AURA_Service.storage.dto.StorageTrashRestoreRequest;
import com.AURA.AURA_Service.storage.service.StorageItemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage/trash")
public class StorageTrashController {
	private final StorageItemService storageItemService;

	public StorageTrashController(StorageItemService storageItemService) {
		this.storageItemService = storageItemService;
	}

	@Operation(summary = "휴지통 항목 목록 조회", description = "현재 로그인한 사용자의 Gmail/Drive 휴지통 항목 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<StorageTrashPageResponse>> getTrashItems(@AuthenticationPrincipal String userId,
		@RequestParam(value = "item_source", required = false) ItemSource itemSource,
		@RequestParam(value = "page", required = false) Integer page,
		@RequestParam(value = "size", required = false) Integer size) {
		return ResponseEntity.ok(ApiResponse.success(storageItemService.getTrashItems(Long.valueOf(userId),
			itemSource, page, size)));
	}

	@Operation(summary = "휴지통 선택 항목 복구", description = "사용자가 승인한 휴지통 항목을 Gmail/Drive 원래 위치로 복구하는 작업을 생성합니다.")
	@PostMapping("/restore")
	public ResponseEntity<ApiResponse<CleanupJobCreateResponse>> restore(@AuthenticationPrincipal String userId,
		@Valid @RequestBody StorageTrashRestoreRequest request) {
		CleanupJobCreateResponse response = storageItemService.restoreTrashItems(Long.valueOf(userId), request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/api/cleanup-jobs/" + response.cleanupJobId()))
			.body(ApiResponse.success(response));
	}

	@Operation(summary = "휴지통 선택 항목 영구 삭제", description = "사용자가 최종 승인한 휴지통 항목을 영구 삭제 작업으로 생성합니다.")
	@PostMapping("/permanent-delete")
	public ResponseEntity<ApiResponse<CleanupJobCreateResponse>> permanentDelete(@AuthenticationPrincipal String userId,
		@Valid @RequestBody StoragePermanentDeleteRequest request) {
		CleanupJobCreateResponse response = storageItemService.permanentDeleteTrashItems(Long.valueOf(userId), request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/api/cleanup-jobs/" + response.cleanupJobId()))
			.body(ApiResponse.success(response));
	}

	@Operation(summary = "휴지통 비우기", description = "사용자가 최종 승인한 범위의 휴지통 비우기 작업을 생성합니다.")
	@PostMapping("/empty")
	public ResponseEntity<ApiResponse<CleanupJobCreateResponse>> emptyTrash(@AuthenticationPrincipal String userId,
		@Valid @RequestBody StorageTrashEmptyRequest request) {
		CleanupJobCreateResponse response = storageItemService.emptyTrash(Long.valueOf(userId), request);
		return ResponseEntity.status(HttpStatus.CREATED)
			.location(URI.create("/api/cleanup-jobs/" + response.cleanupJobId()))
			.body(ApiResponse.success(response));
	}
}

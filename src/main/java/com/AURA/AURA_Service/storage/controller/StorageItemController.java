package com.AURA.AURA_Service.storage.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.storage.dto.StorageItemDetailResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemLiveDetailResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemPageResponse;
import com.AURA.AURA_Service.storage.service.StorageItemService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage/items")
public class StorageItemController {
	private final StorageItemService storageItemService;

	public StorageItemController(StorageItemService storageItemService) {
		this.storageItemService = storageItemService;
	}

	@Operation(summary = "저장소 메일/드라이브 항목 목록 조회", description = "현재 로그인한 사용자의 스캔 저장소 항목 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<StorageItemPageResponse>> getItems(@AuthenticationPrincipal String userId,
		@RequestParam("item_source") ItemSource itemSource,
		@RequestParam(value = "trashed", required = false) Boolean trashed,
		@RequestParam(value = "sort", required = false) String sort,
		@RequestParam(value = "page", required = false) Integer page,
		@RequestParam(value = "size", required = false) Integer size) {
		return ResponseEntity.ok(ApiResponse.success(storageItemService.getItems(Long.valueOf(userId),
			itemSource, trashed, sort, page, size)));
	}

	@Operation(summary = "저장소 항목 상세 조회", description = "현재 로그인한 사용자의 스캔 저장소 항목 상세 정보를 조회합니다.")
	@GetMapping("/{itemId}")
	public ResponseEntity<ApiResponse<StorageItemDetailResponse>> getItem(@AuthenticationPrincipal String userId,
		@PathVariable("itemId") Long itemId) {
		return ResponseEntity.ok(ApiResponse.success(storageItemService.getItem(Long.valueOf(userId), itemId)));
	}

	@Operation(summary = "외부 항목 ID 기반 저장소 상세 조회", description = "Gmail Message ID 또는 Drive File ID를 기준으로 최신 메타데이터를 조회합니다.")
	@GetMapping("/detail")
	public ResponseEntity<ApiResponse<StorageItemLiveDetailResponse>> getLiveItemDetail(@AuthenticationPrincipal String userId,
		@RequestParam("item_source") ItemSource itemSource,
		@RequestParam("external_item_id") String externalItemId) {
		return ResponseEntity.ok(ApiResponse.success(storageItemService.getLiveItemDetail(Long.valueOf(userId),
			itemSource, externalItemId)));
	}
}

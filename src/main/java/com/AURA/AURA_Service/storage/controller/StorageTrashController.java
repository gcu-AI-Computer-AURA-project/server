package com.AURA.AURA_Service.storage.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.storage.dto.StorageTrashPageResponse;
import com.AURA.AURA_Service.storage.service.StorageItemService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

	@Operation(summary = "휴지통 항목 목록 조회", description = "현재 로그인한 사용자의 휴지통 항목 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<StorageTrashPageResponse>> getTrashItems(@AuthenticationPrincipal String userId,
		@RequestParam(value = "item_source", required = false) ItemSource itemSource,
		@RequestParam(value = "page", required = false) Integer page,
		@RequestParam(value = "size", required = false) Integer size) {
		return ResponseEntity.ok(ApiResponse.success(storageItemService.getTrashItems(Long.valueOf(userId),
			itemSource, page, size)));
	}
}

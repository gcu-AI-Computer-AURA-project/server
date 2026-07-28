package com.AURA.AURA_Service.announcement.controller;

import com.AURA.AURA_Service.announcement.domain.AnnouncementCategory;
import com.AURA.AURA_Service.announcement.dto.AnnouncementPageResponse;
import com.AURA.AURA_Service.announcement.service.AnnouncementService;
import com.AURA.AURA_Service.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
	private final AnnouncementService announcementService;

	public AnnouncementController(AnnouncementService announcementService) {
		this.announcementService = announcementService;
	}

	@Operation(summary = "공지사항 목록 조회", description = "설정 화면에서 제공하는 공지사항 목록을 상단 고정 및 최신순으로 조회합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<AnnouncementPageResponse>> getList(@AuthenticationPrincipal String userId,
		@RequestParam(required = false) AnnouncementCategory category,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.success(announcementService.getList(Long.valueOf(userId), category, page, size)));
	}
}

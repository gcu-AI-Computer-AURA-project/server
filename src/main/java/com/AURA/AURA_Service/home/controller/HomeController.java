package com.AURA.AURA_Service.home.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse;
import com.AURA.AURA_Service.home.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {
	private final HomeService homeService;

	public HomeController(HomeService homeService) {
		this.homeService = homeService;
	}

	@Operation(summary = "홈 화면 요약 정보 조회", description = "최근 스캔 상태, 정리 후보 수, 예상 확보 용량, 최근 정리 결과, Google 권한 상태를 조회합니다.")
	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<HomeSummaryResponse>> getSummary(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(homeService.getSummary(Long.valueOf(userId))));
	}
}

package com.AURA.AURA_Service.statistics.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsSummaryResponse;
import com.AURA.AURA_Service.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
	private final StatisticsService statisticsService;

	public StatisticsController(StatisticsService statisticsService) {
		this.statisticsService = statisticsService;
	}

	@Operation(summary = "통계 화면 요약 조회", description = "사용자의 누적 스캔, 정리, 확보 용량, 탄소 절감 요약을 조회합니다.")
	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<StatisticsSummaryResponse>> getSummary(@AuthenticationPrincipal String userId) {
		return ResponseEntity.ok(ApiResponse.success(statisticsService.getSummary(Long.valueOf(userId))));
	}
}

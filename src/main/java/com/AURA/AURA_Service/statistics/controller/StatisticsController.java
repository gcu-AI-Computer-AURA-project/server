package com.AURA.AURA_Service.statistics.controller;

import com.AURA.AURA_Service.common.ApiResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsCleanupHistoryResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsMonthlyResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsSummaryResponse;
import com.AURA.AURA_Service.statistics.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

	@Operation(summary = "월별 탄소 절감 그래프 조회", description = "사용자의 월별 스캔, 정리, 확보 용량, 탄소 절감 통계를 조회합니다.")
	@GetMapping("/monthly")
	public ResponseEntity<ApiResponse<StatisticsMonthlyResponse>> getMonthly(@AuthenticationPrincipal String userId,
		@RequestParam(required = false) String from,
		@RequestParam(required = false) String to) {
		return ResponseEntity.ok(ApiResponse.success(statisticsService.getMonthly(Long.valueOf(userId), from, to)));
	}

	@Operation(summary = "최근 정리 기록 조회", description = "사용자의 정리 완료 이력을 최신순으로 조회합니다.")
	@GetMapping("/cleanup-histories")
	public ResponseEntity<ApiResponse<StatisticsCleanupHistoryResponse>> getCleanupHistories(
		@AuthenticationPrincipal String userId,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size) {
		return ResponseEntity.ok(ApiResponse.success(statisticsService.getCleanupHistories(Long.valueOf(userId), page,
			size)));
	}
}

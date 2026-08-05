package com.AURA.AURA_Service.statistics.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import com.AURA.AURA_Service.statistics.dto.StatisticsSummaryResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
	private static final BigDecimal ZERO_CARBON_GRAMS = new BigDecimal("0.0000");

	private final UserRepository userRepository;
	private final ScanJobRepository scanJobRepository;
	private final JdbcTemplate jdbcTemplate;

	public StatisticsService(UserRepository userRepository, ScanJobRepository scanJobRepository, JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.scanJobRepository = scanJobRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public StatisticsSummaryResponse getSummary(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		StatisticsCleanupSummary cleanupSummary = findCleanupSummary(user.getUserId());
		return new StatisticsSummaryResponse(
			scanJobRepository.countByUserAndDeletedAtIsNull(user),
			cleanupSummary.totalCleanupCount(),
			cleanupSummary.totalTrashedItemCount(),
			cleanupSummary.totalPermanentlyDeletedItemCount(),
			cleanupSummary.totalReclaimedBytes(),
			cleanupSummary.totalEstimatedCarbonGrams(),
			cleanupSummary.latestCleanupAt()
		);
	}

	private StatisticsCleanupSummary findCleanupSummary(Long userId) {
		try {
			return jdbcTemplate.query("""
				select
					count(*) as total_cleanup_count,
					coalesce(sum(case when action_type = 'MOVE_TO_TRASH' then cleaned_item_count else 0 end), 0) as total_trashed_item_count,
					coalesce(sum(case when action_type in ('PERMANENT_DELETE', 'EMPTY_TRASH') then cleaned_item_count else 0 end), 0) as total_permanently_deleted_item_count,
					coalesce(sum(reclaimed_bytes), 0) as total_reclaimed_bytes,
					max(completed_at) as latest_cleanup_at
				from cleanup_histories
				where user_id = ?
				""", (rs, rowNum) -> new StatisticsCleanupSummary(
					rs.getLong("total_cleanup_count"),
					rs.getLong("total_trashed_item_count"),
					rs.getLong("total_permanently_deleted_item_count"),
					rs.getLong("total_reclaimed_bytes"),
					findTotalEstimatedCarbonGrams(userId),
					toLocalDateTime(rs.getTimestamp("latest_cleanup_at"))
				), userId).stream().findFirst().orElseGet(StatisticsCleanupSummary::empty);
		} catch (DataAccessException exception) {
			return StatisticsCleanupSummary.empty();
		}
	}

	private BigDecimal findTotalEstimatedCarbonGrams(Long userId) {
		try {
			BigDecimal totalEstimatedCarbonGrams = jdbcTemplate.queryForObject("""
				select coalesce(sum(estimated_carbon_grams), 0.0000)
				from carbon_saving_histories
				where user_id = ?
				""", BigDecimal.class, userId);
			return totalEstimatedCarbonGrams == null ? ZERO_CARBON_GRAMS : totalEstimatedCarbonGrams;
		} catch (DataAccessException exception) {
			return ZERO_CARBON_GRAMS;
		}
	}

	private LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private record StatisticsCleanupSummary(
		long totalCleanupCount,
		long totalTrashedItemCount,
		long totalPermanentlyDeletedItemCount,
		long totalReclaimedBytes,
		BigDecimal totalEstimatedCarbonGrams,
		LocalDateTime latestCleanupAt
	) {
		private static StatisticsCleanupSummary empty() {
			return new StatisticsCleanupSummary(0L, 0L, 0L, 0L, ZERO_CARBON_GRAMS, null);
		}
	}
}

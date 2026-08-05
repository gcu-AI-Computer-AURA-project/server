package com.AURA.AURA_Service.statistics.service;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import com.AURA.AURA_Service.statistics.dto.CarbonFormulaResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsCleanupHistoryResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsCleanupHistoryResponse.CleanupHistoryItemResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsMonthlyResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsMonthlyResponse.MonthlyStatisticResponse;
import com.AURA.AURA_Service.statistics.dto.StatisticsSummaryResponse;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
	private static final int MAX_PAGE_SIZE = 100;
	private static final BigDecimal ZERO_CARBON_GRAMS = new BigDecimal("0.0000");
	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
	private static final String CARBON_FORMULA_VERSION = "v1";
	private static final String CARBON_FORMULA_DESCRIPTION = "삭제 또는 휴지통 이동으로 확보한 저장 용량을 내부 기준에 따라 예상 탄소 절감량으로 환산합니다.";
	private static final String CARBON_FORMULA_UNIT = "grams";
	private static final LocalDateTime CARBON_FORMULA_LAST_UPDATED_AT = LocalDateTime.of(2026, 7, 14, 0, 0);

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

	@Transactional(readOnly = true)
	public StatisticsMonthlyResponse getMonthly(Long userId, String from, String to) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		YearMonthRange range = resolveYearMonthRange(from, to);
		Map<String, MonthlyStatisticResponse> months = createEmptyMonthlyStatistics(range);
		findMonthlyStatistics(user.getUserId(), range).forEach(month -> months.put(month.statYearMonth(), month));
		return new StatisticsMonthlyResponse(List.copyOf(months.values()));
	}

	@Transactional(readOnly = true)
	public StatisticsCleanupHistoryResponse getCleanupHistories(Long userId, int page, int size) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		validatePageRequest(page, size);
		try {
			long totalElements = countCleanupHistories(user.getUserId());
			List<CleanupHistoryItemResponse> content = findCleanupHistories(user.getUserId(), page, size);
			return StatisticsCleanupHistoryResponse.from(content, page, size, totalElements);
		} catch (DataAccessException exception) {
			return StatisticsCleanupHistoryResponse.empty(page, size);
		}
	}

	public CarbonFormulaResponse getCarbonFormula() {
		return new CarbonFormulaResponse(
			CARBON_FORMULA_VERSION,
			CARBON_FORMULA_DESCRIPTION,
			CARBON_FORMULA_UNIT,
			CARBON_FORMULA_LAST_UPDATED_AT
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

	private List<MonthlyStatisticResponse> findMonthlyStatistics(Long userId, YearMonthRange range) {
		try {
			return jdbcTemplate.query("""
				select
					stat_year_month,
					scan_count,
					cleanup_count,
					trashed_item_count,
					permanently_deleted_item_count,
					reclaimed_bytes,
					estimated_carbon_grams
				from monthly_user_statistics
				where user_id = ?
					and stat_year_month between ? and ?
				order by stat_year_month asc
				""", (rs, rowNum) -> new MonthlyStatisticResponse(
					rs.getString("stat_year_month"),
					rs.getLong("scan_count"),
					rs.getLong("cleanup_count"),
					rs.getLong("trashed_item_count"),
					rs.getLong("permanently_deleted_item_count"),
					rs.getLong("reclaimed_bytes"),
					defaultCarbon(rs.getBigDecimal("estimated_carbon_grams"))
				), userId, range.fromText(), range.toText());
		} catch (DataAccessException exception) {
			return List.of();
		}
	}

	private long countCleanupHistories(Long userId) {
		Long totalElements = jdbcTemplate.queryForObject("""
			select count(*)
			from cleanup_histories
			where user_id = ?
			""", Long.class, userId);
		return totalElements == null ? 0L : totalElements;
	}

	private List<CleanupHistoryItemResponse> findCleanupHistories(Long userId, int page, int size) {
		return jdbcTemplate.query("""
			select
				ch.history_id,
				ch.cleanup_job_id,
				ch.scan_job_id,
				ch.action_type,
				ch.cleaned_item_count,
				ch.reclaimed_bytes,
				coalesce(csh.estimated_carbon_grams, 0.0000) as estimated_carbon_grams,
				ch.completed_at
			from cleanup_histories ch
			left join carbon_saving_histories csh on csh.history_id = ch.history_id
			where ch.user_id = ?
			order by ch.completed_at desc, ch.history_id desc
			limit ? offset ?
			""", (rs, rowNum) -> new CleanupHistoryItemResponse(
				rs.getLong("history_id"),
				rs.getLong("cleanup_job_id"),
				getNullableLong(rs.getObject("scan_job_id")),
				rs.getString("action_type"),
				rs.getInt("cleaned_item_count"),
				rs.getLong("reclaimed_bytes"),
				defaultCarbon(rs.getBigDecimal("estimated_carbon_grams")),
				toLocalDateTime(rs.getTimestamp("completed_at"))
			), userId, size, (long)page * size);
	}

	private YearMonthRange resolveYearMonthRange(String from, String to) {
		YearMonth toMonth = isBlank(to) ? YearMonth.now() : parseYearMonth(to);
		YearMonth fromMonth = isBlank(from) ? toMonth.minusMonths(5) : parseYearMonth(from);
		if (fromMonth.isAfter(toMonth)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		return new YearMonthRange(fromMonth, toMonth);
	}

	private YearMonth parseYearMonth(String value) {
		try {
			return YearMonth.parse(value, YEAR_MONTH_FORMATTER);
		} catch (DateTimeParseException exception) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
	}

	private Map<String, MonthlyStatisticResponse> createEmptyMonthlyStatistics(YearMonthRange range) {
		Map<String, MonthlyStatisticResponse> months = new LinkedHashMap<>();
		YearMonth current = range.from();
		while (!current.isAfter(range.to())) {
			String statYearMonth = current.format(YEAR_MONTH_FORMATTER);
			months.put(statYearMonth, new MonthlyStatisticResponse(statYearMonth, 0L, 0L, 0L, 0L, 0L, ZERO_CARBON_GRAMS));
			current = current.plusMonths(1);
		}
		return months;
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private void validatePageRequest(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
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

	private BigDecimal defaultCarbon(BigDecimal value) {
		return value == null ? ZERO_CARBON_GRAMS : value;
	}

	private Long getNullableLong(Object value) {
		if (value == null) {
			return null;
		}
		return ((Number)value).longValue();
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

	private record YearMonthRange(
		YearMonth from,
		YearMonth to
	) {
		private String fromText() {
			return from.format(YEAR_MONTH_FORMATTER);
		}

		private String toText() {
			return to.format(YEAR_MONTH_FORMATTER);
		}
	}
}

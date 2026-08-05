package com.AURA.AURA_Service.home.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.LatestCleanupResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.LatestScanResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.PermissionSummaryResponse;
import com.AURA.AURA_Service.home.dto.HomeSummaryResponse.StorageSummaryResponse;
import com.AURA.AURA_Service.scan.domain.ScanJob;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.repository.ScanJobRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {
	private static final List<JobStatus> RUNNING_STATUSES = List.of(JobStatus.PENDING, JobStatus.SCANNING, JobStatus.ANALYZING);
	private static final BigDecimal ZERO_CARBON_GRAMS = new BigDecimal("0.0000");

	private final UserRepository userRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final ScanJobRepository scanJobRepository;
	private final JdbcTemplate jdbcTemplate;

	public HomeService(UserRepository userRepository, GooglePermissionRepository googlePermissionRepository,
		ScanJobRepository scanJobRepository, JdbcTemplate jdbcTemplate) {
		this.userRepository = userRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.scanJobRepository = scanJobRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Transactional(readOnly = true)
	public HomeSummaryResponse getSummary(Long userId) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		ScanJob latestScan = scanJobRepository.findFirstByUserAndDeletedAtIsNullOrderByCreatedAtDesc(user).orElse(null);
		boolean hasRunningScan = scanJobRepository.existsByUserAndJobStatusInAndDeletedAtIsNull(user, RUNNING_STATUSES);
		return new HomeSummaryResponse(
			createStorageSummary(user.getUserId(), latestScan),
			LatestScanResponse.from(latestScan),
			findLatestCleanup(user.getUserId()).orElse(null),
			createPermissionSummary(user),
			hasRunningScan
		);
	}

	private StorageSummaryResponse createStorageSummary(Long userId, ScanJob latestScan) {
		CleanupStorageSummary cleanupSummary = findCleanupStorageSummary(userId);
		return new StorageSummaryResponse(
			latestScan == null ? 0L : latestScan.getEstimatedReclaimBytes(),
			cleanupSummary.latestRemainingDriveBytes(),
			cleanupSummary.totalReclaimedBytes(),
			cleanupSummary.totalEstimatedCarbonGrams()
		);
	}

	private PermissionSummaryResponse createPermissionSummary(User user) {
		List<GooglePermission> permissions = googlePermissionRepository.findByUser(user);
		return new PermissionSummaryResponse(
			findPermissionStatus(permissions, ServiceType.GMAIL),
			findPermissionStatus(permissions, ServiceType.DRIVE)
		);
	}

	private PermissionStatus findPermissionStatus(List<GooglePermission> permissions, ServiceType serviceType) {
		return permissions.stream()
			.filter(permission -> permission.getServiceType() == serviceType)
			.map(GooglePermission::getPermissionStatus)
			.findFirst()
			.orElse(PermissionStatus.DISCONNECTED);
	}

	private CleanupStorageSummary findCleanupStorageSummary(Long userId) {
		try {
			Long totalReclaimedBytes = jdbcTemplate.queryForObject("""
				select coalesce(sum(reclaimed_bytes), 0)
				from cleanup_histories
				where user_id = ?
				""", Long.class, userId);
			Long latestRemainingDriveBytes = jdbcTemplate.query("""
				select remaining_drive_bytes
				from cleanup_histories
				where user_id = ?
					and remaining_drive_bytes is not null
				order by completed_at desc
				limit 1
				""", (rs, rowNum) -> rs.getLong("remaining_drive_bytes"), userId).stream()
				.findFirst()
				.orElse(null);
			BigDecimal totalEstimatedCarbonGrams = jdbcTemplate.queryForObject("""
				select coalesce(sum(estimated_carbon_grams), 0.0000)
				from carbon_saving_histories
				where user_id = ?
				""", BigDecimal.class, userId);
			return new CleanupStorageSummary(
				defaultZero(totalReclaimedBytes),
				latestRemainingDriveBytes,
				defaultCarbon(totalEstimatedCarbonGrams)
			);
		} catch (DataAccessException exception) {
			return CleanupStorageSummary.empty();
		}
	}

	private Optional<LatestCleanupResponse> findLatestCleanup(Long userId) {
		try {
			return jdbcTemplate.query("""
				select
					ch.cleanup_job_id,
					ch.cleaned_item_count,
					ch.reclaimed_bytes,
					coalesce(csh.estimated_carbon_grams, 0.0000) as estimated_carbon_grams,
					ch.completed_at
				from cleanup_histories ch
				left join carbon_saving_histories csh on csh.history_id = ch.history_id
				where ch.user_id = ?
				order by ch.completed_at desc
				limit 1
				""", (rs, rowNum) -> new LatestCleanupResponse(
					rs.getLong("cleanup_job_id"),
					rs.getInt("cleaned_item_count"),
					rs.getLong("reclaimed_bytes"),
					defaultCarbon(rs.getBigDecimal("estimated_carbon_grams")),
					toLocalDateTime(rs.getTimestamp("completed_at"))
				), userId).stream().findFirst();
		} catch (DataAccessException exception) {
			return Optional.empty();
		}
	}

	private Long defaultZero(Long value) {
		return value == null ? 0L : value;
	}

	private BigDecimal defaultCarbon(BigDecimal value) {
		return value == null ? ZERO_CARBON_GRAMS : value;
	}

	private LocalDateTime toLocalDateTime(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private record CleanupStorageSummary(
		Long totalReclaimedBytes,
		Long latestRemainingDriveBytes,
		BigDecimal totalEstimatedCarbonGrams
	) {
		private static CleanupStorageSummary empty() {
			return new CleanupStorageSummary(0L, null, ZERO_CARBON_GRAMS);
		}
	}
}

package com.AURA.AURA_Service.cleanup.domain;

import com.AURA.AURA_Service.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carbon_saving_histories")
public class CarbonSavingHistory {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "carbon_history_id") private Long carbonHistoryId;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
	@OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "history_id", nullable = false, unique = true) private CleanupHistory cleanupHistory;
	@Column(name = "reclaimed_bytes", nullable = false) private Long reclaimedBytes;
	@Column(name = "estimated_carbon_grams", nullable = false, precision = 12, scale = 4) private BigDecimal estimatedCarbonGrams;
	@Column(name = "formula_version", nullable = false, length = 30) private String formulaVersion;
	@Column(name = "calculated_at", nullable = false) private LocalDateTime calculatedAt;

	protected CarbonSavingHistory() { }

	public BigDecimal getEstimatedCarbonGrams() { return estimatedCarbonGrams; }
	public String getFormulaVersion() { return formulaVersion; }
}

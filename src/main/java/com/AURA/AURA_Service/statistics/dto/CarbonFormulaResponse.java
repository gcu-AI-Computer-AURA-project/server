package com.AURA.AURA_Service.statistics.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public record CarbonFormulaResponse(
	@JsonProperty("formula_version")
	String formulaVersion,
	String description,
	String unit,
	@JsonProperty("last_updated_at")
	LocalDateTime lastUpdatedAt
) {
}

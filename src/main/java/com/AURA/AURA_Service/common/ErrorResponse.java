package com.AURA.AURA_Service.common;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
	LocalDateTime timestamp,
	int status,
	String code,
	String message,
	List<FieldErrorDetail> errors,
	String path
) {
	public record FieldErrorDetail(String field, Object value, String reason) {
	}
}

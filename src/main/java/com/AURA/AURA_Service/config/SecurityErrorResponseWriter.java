package com.AURA.AURA_Service.config;

import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorResponseWriter {
	private final ObjectMapper objectMapper;

	public SecurityErrorResponseWriter() {
		this.objectMapper = new ObjectMapper().findAndRegisterModules();
	}

	public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode) throws IOException {
		ErrorResponse errorResponse = new ErrorResponse(
			LocalDateTime.now(),
			errorCode.getHttpStatus().value(),
			errorCode.getCode(),
			errorCode.getMessage(),
			List.of(),
			request.getRequestURI()
		);
		response.setStatus(errorCode.getHttpStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), errorResponse);
	}
}

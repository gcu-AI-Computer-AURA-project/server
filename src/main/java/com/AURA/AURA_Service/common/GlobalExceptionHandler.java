package com.AURA.AURA_Service.common;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception, HttpServletRequest request) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity.status(errorCode.getHttpStatus()).body(createResponse(errorCode, List.of(), request));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
		List<ErrorResponse.FieldErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
			.map(error -> new ErrorResponse.FieldErrorDetail(error.getField(), error.getRejectedValue(), error.getDefaultMessage()))
			.toList();
		return ResponseEntity.badRequest().body(createResponse(ErrorCode.INVALID_INPUT, errors, request));
	}

	@ExceptionHandler({
		HttpMessageNotReadableException.class,
		MethodArgumentTypeMismatchException.class,
		MissingServletRequestParameterException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequestException(Exception exception, HttpServletRequest request) {
		return ResponseEntity.badRequest().body(createResponse(ErrorCode.INVALID_INPUT, List.of(), request));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
		LOGGER.error("처리되지 않은 서버 오류가 발생했습니다. path={}", request.getRequestURI(), exception);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(errorCode.getHttpStatus()).body(createResponse(errorCode, List.of(), request));
	}

	private ErrorResponse createResponse(ErrorCode errorCode, List<ErrorResponse.FieldErrorDetail> errors, HttpServletRequest request) {
		return new ErrorResponse(LocalDateTime.now(), errorCode.getHttpStatus().value(), errorCode.getCode(), errorCode.getMessage(), errors, request.getRequestURI());
	}
}

package com.AURA.AURA_Service.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "입력값이 올바르지 않습니다."),
	GOOGLE_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-001", "Google 인증에 실패했습니다."),
	INVALID_REDIRECT_URI(HttpStatus.BAD_REQUEST, "AUTH-002", "허용되지 않은 Redirect URI입니다."),
	INVALID_SERVER_CONFIGURATION(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH-003", "인증 서버 설정이 올바르지 않습니다."),
	GOOGLE_AUTHORIZATION_CODE_INVALID(HttpStatus.UNAUTHORIZED, "AUTH-004", "Google 인증 코드가 만료되었거나 이미 사용되었습니다."),
	GOOGLE_USER_INFO_FAILED(HttpStatus.UNAUTHORIZED, "AUTH-005", "Google 사용자 정보를 조회할 수 없습니다. 로그인 Scope를 확인해 주세요."),
	GOOGLE_OAUTH_CLIENT_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-006", "현재 OAuth 클라이언트는 Authorization Code 교환이 허용되지 않습니다."),
	GOOGLE_OAUTH_REQUEST_INVALID(HttpStatus.BAD_REQUEST, "AUTH-007", "Google OAuth 토큰 교환 요청 형식이 올바르지 않습니다."),
	GOOGLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-008", "Google 계정 권한 동의가 거부되었습니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "서버 내부 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	public HttpStatus getHttpStatus() { return httpStatus; }
	public String getCode() { return code; }
	public String getMessage() { return message; }
}

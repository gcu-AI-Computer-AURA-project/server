package com.AURA.AURA_Service.auth.dto;

public record LogoutResponse(boolean success, String message) {
	public static LogoutResponse loggedOut() {
		return new LogoutResponse(true, "로그아웃되었습니다.");
	}
}

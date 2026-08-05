package com.AURA.AURA_Service.auth.dto;

public record GoogleConnectionDisconnectResponse(boolean success, String message) {
	public static GoogleConnectionDisconnectResponse disconnected() {
		return new GoogleConnectionDisconnectResponse(true, "Google \uacc4\uc815 \uc5f0\uacb0\uc774 \ud574\uc81c\ub418\uc5c8\uc2b5\ub2c8\ub2e4.");
	}
}

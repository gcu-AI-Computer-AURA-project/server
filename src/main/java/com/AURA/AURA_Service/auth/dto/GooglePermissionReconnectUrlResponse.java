package com.AURA.AURA_Service.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GooglePermissionReconnectUrlResponse(
	@JsonProperty("authorization_url") String authorizationUrl,
	@JsonProperty("expires_in") long expiresIn
) { }

package com.AURA.AURA_Service.auth.dto;

import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GooglePermissionReconnectUrlRequest(
	@NotEmpty @JsonProperty("service_types") List<@NotNull ServiceType> serviceTypes,
	@NotBlank @JsonProperty("redirect_uri") String redirectUri
) { }

package com.AURA.AURA_Service.config;

import com.AURA.AURA_Service.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	private final SecurityErrorResponseWriter securityErrorResponseWriter;

	public JwtAuthenticationEntryPoint(SecurityErrorResponseWriter securityErrorResponseWriter) {
		this.securityErrorResponseWriter = securityErrorResponseWriter;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
		throws IOException {
		securityErrorResponseWriter.write(request, response, ErrorCode.AUTH_TOKEN_MISSING);
	}
}

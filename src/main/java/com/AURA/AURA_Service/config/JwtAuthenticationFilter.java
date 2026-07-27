package com.AURA.AURA_Service.config;

import com.AURA.AURA_Service.auth.service.JwtTokenService;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenService jwtTokenService;
	private final SecurityErrorResponseWriter securityErrorResponseWriter;

	public JwtAuthenticationFilter(JwtTokenService jwtTokenService, SecurityErrorResponseWriter securityErrorResponseWriter) {
		this.jwtTokenService = jwtTokenService;
		this.securityErrorResponseWriter = securityErrorResponseWriter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String header = request.getHeader(AUTHORIZATION_HEADER);
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			try {
				String token = header.substring(BEARER_PREFIX.length());
				Long userId = jwtTokenService.getAccessTokenUserId(token);
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (CustomException exception) {
				SecurityContextHolder.clearContext();
				securityErrorResponseWriter.write(request, response, exception.getErrorCode());
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.equals("/api/auth/google/login")
			|| path.equals("/api/auth/token/refresh")
			|| path.startsWith("/swagger-ui")
			|| path.startsWith("/v3/api-docs")
			|| path.equals("/actuator/health");
	}
}

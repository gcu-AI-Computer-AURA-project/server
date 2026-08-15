package com.AURA.AURA_Service.cleanup.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.AURA.AURA_Service.cleanup.service.CleanupJobService;
import com.AURA.AURA_Service.common.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CleanupJobControllerTest {
	private final CleanupJobService cleanupJobService = mock(CleanupJobService.class);
	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CleanupJobController(cleanupJobService))
		.setControllerAdvice(new GlobalExceptionHandler())
		.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
		.build();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void malformedCandidateIdArrayReturnsBadRequest() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));

		mockMvc.perform(post("/api/cleanup-jobs")
				.principal(new UsernamePasswordAuthenticationToken("1", null))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "scan_job_id": 19,
					  "action_type": "MOVE_TO_TRASH",
					  "candidates": [
					    {
					      "candidate_id": [5011, 4989],
					      "selection_version": 0
					    }
					  ],
					  "approval_confirmed": true
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON-001"))
			.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(cleanupJobService);
	}

	@Test
	void zeroScanJobIdReturnsBadRequest() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));

		mockMvc.perform(post("/api/cleanup-jobs")
				.principal(new UsernamePasswordAuthenticationToken("1", null))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "scan_job_id": 0,
					  "action_type": "MOVE_TO_TRASH",
					  "candidates": [
					    {
					      "candidate_id": 5011,
					      "selection_version": 0
					    }
					  ],
					  "approval_confirmed": true
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("COMMON-001"))
			.andExpect(jsonPath("$.status").value(400));

		verifyNoInteractions(cleanupJobService);
	}
}

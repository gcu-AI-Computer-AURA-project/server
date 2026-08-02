package com.AURA.AURA_Service.scan.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.AURA.AURA_Service.auth.domain.ScanSetting.ScanSource;
import com.AURA.AURA_Service.scan.domain.ScanJob.JobStatus;
import com.AURA.AURA_Service.scan.dto.ScanCreateRequest;
import com.AURA.AURA_Service.scan.dto.ScanCreateResponse;
import com.AURA.AURA_Service.scan.service.ScanJobService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScanJobControllerTest {
	private final ScanJobService scanJobService = mock(ScanJobService.class);
	private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ScanJobController(scanJobService))
		.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
		.build();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void createReturnsCreatedScanJobResponse() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));
		when(scanJobService.create(eq(1L), any(ScanCreateRequest.class))).thenReturn(new ScanCreateResponse(
			15L,
			JobStatus.PENDING,
			ScanSource.DRIVE_FOLDER,
			new BigDecimal("0.00"),
			LocalDateTime.of(2026, 7, 14, 10, 40, 0)
		));

		mockMvc.perform(post("/api/scans")
				.principal(new UsernamePasswordAuthenticationToken("1", null))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "use_saved_settings": false,
					  "settings_override": {
					    "scan_source": "DRIVE_FOLDER",
					    "drive_folder_id": "1aBcDriveFolderId",
					    "include_subfolders": true,
					    "last_opened_before_months": 36,
					    "last_modified_before_months": 24,
					    "created_before_months": 6,
					    "exclude_recent_days": 30,
					    "include_keywords": ["광고", "프로모션"],
					    "exclude_keywords": ["영수증", "계약서"],
					    "file_extensions": ["pdf", "zip"],
					    "include_mail_attachment_size": true,
					    "apply_recent_conditions": true
					  }
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/scans/15"))
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.scan_job_id").value(15))
			.andExpect(jsonPath("$.data.job_status").value("PENDING"))
			.andExpect(jsonPath("$.data.scan_source").value("DRIVE_FOLDER"))
			.andExpect(jsonPath("$.data.progress_percent").value(0.00))
			.andExpect(jsonPath("$.data.created_at").value("2026-07-14T10:40:00"));
	}
}

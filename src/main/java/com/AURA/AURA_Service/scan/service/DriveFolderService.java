package com.AURA.AURA_Service.scan.service;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.PermissionStatus;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.OAuthToken.TokenStatus;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.GooglePermissionRepository;
import com.AURA.AURA_Service.auth.repository.OAuthTokenRepository;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient;
import com.AURA.AURA_Service.auth.service.GoogleOAuthClient.GoogleToken;
import com.AURA.AURA_Service.auth.service.TokenEncryptionService;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.dto.DriveFolderResponse;
import com.AURA.AURA_Service.scan.dto.DriveFolderResponse.DriveFolderItem;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriveFolderService {
	private static final String APPLICATION_NAME = "AURA_Service";
	private static final String DRIVE_ROOT_FOLDER_ID = "root";
	private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";
	private static final String FOLDER_FIELDS = "nextPageToken, files(id, name, parents, modifiedTime, driveId)";
	private static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

	private final UserRepository userRepository;
	private final OAuthTokenRepository oauthTokenRepository;
	private final GooglePermissionRepository googlePermissionRepository;
	private final TokenEncryptionService tokenEncryptionService;
	private final GoogleOAuthClient googleOAuthClient;

	public DriveFolderService(UserRepository userRepository, OAuthTokenRepository oauthTokenRepository,
		GooglePermissionRepository googlePermissionRepository, TokenEncryptionService tokenEncryptionService,
		GoogleOAuthClient googleOAuthClient) {
		this.userRepository = userRepository;
		this.oauthTokenRepository = oauthTokenRepository;
		this.googlePermissionRepository = googlePermissionRepository;
		this.tokenEncryptionService = tokenEncryptionService;
		this.googleOAuthClient = googleOAuthClient;
	}

	@Transactional
	public DriveFolderResponse getDriveFolders(Long userId, String parentId, String pageToken, Integer size) {
		User user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		OAuthToken oauthToken = oauthTokenRepository.findByUser(user)
			.orElseThrow(() -> new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED));
		validateToken(oauthToken);
		validateDrivePermission(user, oauthToken);

		String refreshToken = tokenEncryptionService.decrypt(oauthToken.getEncryptedRefreshToken());
		GoogleToken googleToken = googleOAuthClient.refreshAccessToken(refreshToken);
		oauthToken.update(null, googleToken.expiresIn(), googleToken.scope());

		try {
			FileList fileList = createDrive(googleToken.accessToken()).files().list()
				.setQ(createFolderQuery(parentId))
				.setFields(FOLDER_FIELDS)
				.setPageToken(normalize(pageToken))
				.setPageSize(size)
				.setOrderBy("folder,name")
				.setCorpora("user")
				.setIncludeItemsFromAllDrives(false)
				.setSupportsAllDrives(false)
				.execute();
			return new DriveFolderResponse(toFolderItems(fileList.getFiles()), fileList.getNextPageToken());
		} catch (IOException exception) {
			throw new CustomException(ErrorCode.GOOGLE_DRIVE_FOLDER_LIST_FAILED);
		}
	}

	private void validateToken(OAuthToken oauthToken) {
		if (oauthToken.getTokenStatus() != TokenStatus.VALID || isBlank(oauthToken.getEncryptedRefreshToken())) {
			throw new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED);
		}
	}

	private void validateDrivePermission(User user, OAuthToken oauthToken) {
		GooglePermission permission = googlePermissionRepository.findByUserAndServiceType(user, ServiceType.DRIVE)
			.orElseGet(() -> createDrivePermissionFromToken(user, oauthToken));
		if (permission.getPermissionStatus() != PermissionStatus.CONNECTED) {
			throw new CustomException(ErrorCode.DRIVE_PERMISSION_REQUIRED);
		}
	}

	private GooglePermission createDrivePermissionFromToken(User user, OAuthToken oauthToken) {
		GooglePermission permission = GooglePermission.create(user, ServiceType.DRIVE);
		LocalDateTime checkedAt = LocalDateTime.now();
		if (hasScope(oauthToken.getScopeText(), "drive")) {
			permission.connect(oauthToken.getScopeText(), checkedAt);
		} else {
			permission.requireReconnect(oauthToken.getScopeText(), checkedAt);
		}
		return googlePermissionRepository.save(permission);
	}

	private boolean hasScope(String scopeText, String keyword) {
		return scopeText != null && scopeText.toLowerCase(Locale.ROOT).contains(keyword);
	}

	private Drive createDrive(String accessToken) {
		try {
			NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, Date.from(Instant.now().plusSeconds(3600))));
			return new Drive.Builder(httpTransport, GsonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
				.setApplicationName(APPLICATION_NAME)
				.build();
		} catch (GeneralSecurityException | IOException exception) {
			throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		}
	}

	private String createFolderQuery(String parentId) {
		String normalizedParentId = normalize(parentId);
		String targetParentId = normalizedParentId == null ? DRIVE_ROOT_FOLDER_ID : normalizedParentId;
		return new StringBuilder("mimeType = '")
			.append(FOLDER_MIME_TYPE)
			.append("' and trashed = false and '")
			.append(escapeQueryValue(targetParentId))
			.append("' in parents")
			.toString();
	}

	private List<DriveFolderItem> toFolderItems(List<File> files) {
		if (files == null) return List.of();
		return files.stream()
			.filter(file -> file.getDriveId() == null)
			.map(file -> new DriveFolderItem(file.getId(), file.getName(), extractParentId(file), toLocalDateTime(file.getModifiedTime())))
			.toList();
	}

	private String extractParentId(File file) {
		List<String> parents = file.getParents();
		if (parents == null || parents.isEmpty()) return null;
		return parents.get(0);
	}

	private LocalDateTime toLocalDateTime(DateTime dateTime) {
		if (dateTime == null) return null;
		return LocalDateTime.ofInstant(Instant.ofEpochMilli(dateTime.getValue()), KOREA_ZONE_ID);
	}

	private String normalize(String value) {
		if (isBlank(value)) return null;
		return value.trim();
	}

	private String escapeQueryValue(String value) {
		return value.replace("\\", "\\\\").replace("'", "\\'");
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}

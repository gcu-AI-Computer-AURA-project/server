package com.AURA.AURA_Service.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FirebaseConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseConfig.class);

	@Value("${aura.fcm.credential-path:}")
	private String credentialPath;

	/**
	 * Firebase Admin SDK 초기화 메소드
	 * 환경변수로 주입된 서비스 계정 JSON 키 경로를 읽어 FirebaseApp을 1회 초기화한다.
	 *
	 * @return : 없음
	 * @since : 2026.07.29
	 * @version : 0.0.1
	 * @author : 정효림
	 */
	@PostConstruct
	public void initialize() {
		if (!StringUtils.hasText(credentialPath)) {
			LOGGER.warn("FCM_CREDENTIAL_PATH가 설정되지 않아 Firebase 초기화를 건너뜁니다.");
			return;
		}
		if (!FirebaseApp.getApps().isEmpty()) {
			LOGGER.info("FirebaseApp이 이미 초기화되어 있습니다.");
			return;
		}
		try (InputStream serviceAccount = new FileInputStream(credentialPath)) {
			FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.build();
			FirebaseApp.initializeApp(options);
			LOGGER.info("Firebase Admin SDK 초기화가 완료되었습니다.");
		} catch (IOException exception) {
			throw new IllegalStateException("Firebase 서비스 계정 키 파일을 읽을 수 없습니다.", exception);
		}
	}
}

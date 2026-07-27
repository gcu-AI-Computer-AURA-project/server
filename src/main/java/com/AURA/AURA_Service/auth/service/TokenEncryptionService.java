package com.AURA.AURA_Service.auth.service;

import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenEncryptionService {
	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH = 128;
	private final SecureRandom secureRandom = new SecureRandom();
	private final String encodedKey;

	public TokenEncryptionService(@Value("${aura.token-encryption-key}") String encodedKey) {
		this.encodedKey = encodedKey;
	}

	public String encrypt(String plainText) {
		try {
			byte[] key = Base64.getDecoder().decode(encodedKey);
			if (key.length != 32) throw new IllegalArgumentException();
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		}
	}

	public String decrypt(String encryptedText) {
		try {
			byte[] key = Base64.getDecoder().decode(encodedKey);
			if (key.length != 32) throw new IllegalArgumentException();
			byte[] decoded = Base64.getDecoder().decode(encryptedText);
			ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
			byte[] iv = new byte[IV_LENGTH];
			byteBuffer.get(iv);
			byte[] encrypted = new byte[byteBuffer.remaining()];
			byteBuffer.get(encrypted);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException exception) {
			throw new CustomException(ErrorCode.INVALID_SERVER_CONFIGURATION);
		}
	}
}

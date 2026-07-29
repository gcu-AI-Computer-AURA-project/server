package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.FcmToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
	Optional<FcmToken> findByFcmToken(String fcmToken);
}

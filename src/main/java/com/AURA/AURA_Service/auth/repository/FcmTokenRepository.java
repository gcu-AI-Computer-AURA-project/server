package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.FcmToken;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
	Optional<FcmToken> findByFcmToken(String fcmToken);
	List<FcmToken> findByUserAndIsActiveTrue(User user);
}

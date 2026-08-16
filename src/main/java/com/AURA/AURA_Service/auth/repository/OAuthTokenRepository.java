package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.OAuthToken;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
	Optional<OAuthToken> findByUser(User user);
	Optional<OAuthToken> findByUserUserId(Long userId);
}

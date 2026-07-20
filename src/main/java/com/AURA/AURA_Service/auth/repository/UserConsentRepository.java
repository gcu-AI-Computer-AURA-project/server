package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.domain.UserConsent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
	Optional<UserConsent> findByUser(User user);
}

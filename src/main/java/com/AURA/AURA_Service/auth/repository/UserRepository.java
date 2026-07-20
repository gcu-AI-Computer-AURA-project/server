package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByGoogleProviderId(String googleProviderId);
}

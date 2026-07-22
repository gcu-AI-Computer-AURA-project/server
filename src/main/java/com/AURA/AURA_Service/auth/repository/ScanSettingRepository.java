package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanSettingRepository extends JpaRepository<ScanSetting, Long> {
	boolean existsByUserUserId(Long userId);
	Optional<ScanSetting> findByUser(User user);
}

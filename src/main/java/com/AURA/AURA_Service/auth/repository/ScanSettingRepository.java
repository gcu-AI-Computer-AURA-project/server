package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.ScanSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanSettingRepository extends JpaRepository<ScanSetting, Long> {
	boolean existsByUserUserId(Long userId);
}

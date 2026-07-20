package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
}

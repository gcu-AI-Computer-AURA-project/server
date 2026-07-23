package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.NotificationSetting;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
	Optional<NotificationSetting> findByUser(User user);
}

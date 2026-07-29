package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.Notification;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	Page<Notification> findByUser(User user, Pageable pageable);
	Page<Notification> findByUserAndIsReadFalse(User user, Pageable pageable);
	Optional<Notification> findByNotificationIdAndUser(Long notificationId, User user);
}

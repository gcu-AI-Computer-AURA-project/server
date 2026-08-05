package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScannedItemRepository extends JpaRepository<ScannedItem, Long> {
	long countByUserAndDeletedAtIsNull(User user);
}

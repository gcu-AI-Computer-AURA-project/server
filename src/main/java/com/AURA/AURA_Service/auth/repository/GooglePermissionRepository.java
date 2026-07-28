package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.GooglePermission;
import com.AURA.AURA_Service.auth.domain.GooglePermission.ServiceType;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GooglePermissionRepository extends JpaRepository<GooglePermission, Long> {
	List<GooglePermission> findByUserOrderByServiceTypeAsc(User user);
	List<GooglePermission> findByUser(User user);
	Optional<GooglePermission> findByUserAndServiceType(User user, ServiceType serviceType);
}

package com.AURA.AURA_Service.auth.repository;

import com.AURA.AURA_Service.auth.domain.UserWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {
}

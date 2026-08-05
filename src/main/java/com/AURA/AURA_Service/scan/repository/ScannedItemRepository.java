package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScannedItemRepository extends JpaRepository<ScannedItem, Long>, JpaSpecificationExecutor<ScannedItem> {
	long countByUserAndDeletedAtIsNull(User user);

	@Query("""
		select item
		from ScannedItem item
		join fetch item.scanJob scanJob
		where item.itemId = :itemId
			and item.user.userId = :userId
			and item.deletedAt is null
		""")
	Optional<ScannedItem> findDetailByItemIdAndUserId(@Param("itemId") Long itemId, @Param("userId") Long userId);
}

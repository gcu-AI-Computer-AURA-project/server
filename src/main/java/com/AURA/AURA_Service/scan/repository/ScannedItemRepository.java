package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import java.util.List;
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

	@Query("""
		select item
		from ScannedItem item
		where item.user.userId = :userId
			and item.itemSource = :itemSource
			and item.externalItemId in :externalItemIds
			and item.deletedAt is null
		order by item.createdAt desc
		""")
	List<ScannedItem> findLatestSnapshots(@Param("userId") Long userId, @Param("itemSource") ItemSource itemSource,
		@Param("externalItemIds") List<String> externalItemIds);
}

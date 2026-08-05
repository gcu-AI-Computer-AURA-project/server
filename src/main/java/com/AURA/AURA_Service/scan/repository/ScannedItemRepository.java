package com.AURA.AURA_Service.scan.repository;

import com.AURA.AURA_Service.scan.domain.ScannedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScannedItemRepository extends JpaRepository<ScannedItem, Long>, JpaSpecificationExecutor<ScannedItem> {
}

package com.AURA.AURA_Service.announcement.repository;

import com.AURA.AURA_Service.announcement.domain.Announcement;
import com.AURA.AURA_Service.announcement.domain.AnnouncementCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
	Page<Announcement> findByCategory(AnnouncementCategory category, Pageable pageable);
}

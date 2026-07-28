package com.AURA.AURA_Service.announcement.repository;

import com.AURA.AURA_Service.announcement.domain.Announcement;
import com.AURA.AURA_Service.announcement.domain.AnnouncementRead;
import com.AURA.AURA_Service.auth.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
	Optional<AnnouncementRead> findByUserAndAnnouncement(User user, Announcement announcement);
	List<AnnouncementRead> findByUserAndAnnouncementIn(User user, Collection<Announcement> announcements);
}

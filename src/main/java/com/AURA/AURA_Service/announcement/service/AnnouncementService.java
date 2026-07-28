package com.AURA.AURA_Service.announcement.service;

import com.AURA.AURA_Service.announcement.domain.Announcement;
import com.AURA.AURA_Service.announcement.domain.AnnouncementCategory;
import com.AURA.AURA_Service.announcement.domain.AnnouncementRead;
import com.AURA.AURA_Service.announcement.dto.AnnouncementDetailResponse;
import com.AURA.AURA_Service.announcement.dto.AnnouncementListItemResponse;
import com.AURA.AURA_Service.announcement.dto.AnnouncementPageResponse;
import com.AURA.AURA_Service.announcement.dto.AnnouncementReadResponse;
import com.AURA.AURA_Service.announcement.repository.AnnouncementReadRepository;
import com.AURA.AURA_Service.announcement.repository.AnnouncementRepository;
import com.AURA.AURA_Service.auth.domain.User;
import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnnouncementService {
	private static final int MAX_PAGE_SIZE = 100;

	private final AnnouncementRepository announcementRepository;
	private final AnnouncementReadRepository announcementReadRepository;
	private final UserRepository userRepository;

	public AnnouncementService(AnnouncementRepository announcementRepository,
		AnnouncementReadRepository announcementReadRepository,
		UserRepository userRepository) {
		this.announcementRepository = announcementRepository;
		this.announcementReadRepository = announcementReadRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public AnnouncementPageResponse getList(Long userId, AnnouncementCategory category, int page, int size) {
		User user = findUser(userId);
		Pageable pageable = createPageable(page, size);
		Page<Announcement> announcements = category == null
			? announcementRepository.findAll(pageable)
			: announcementRepository.findByCategory(category, pageable);
		Set<Long> readAnnouncementIds = findReadAnnouncementIds(user, announcements.getContent());
		List<AnnouncementListItemResponse> content = announcements.getContent().stream()
			.map(announcement -> AnnouncementListItemResponse.from(announcement, readAnnouncementIds))
			.toList();
		return AnnouncementPageResponse.from(announcements, content);
	}

	@Transactional(readOnly = true)
	public AnnouncementDetailResponse getDetail(Long userId, Long announcementId) {
		User user = findUser(userId);
		Announcement announcement = findAnnouncement(announcementId);
		LocalDateTime readAt = announcementReadRepository.findByUserAndAnnouncement(user, announcement)
			.map(AnnouncementRead::getReadAt)
			.orElse(null);
		return AnnouncementDetailResponse.from(announcement, readAt);
	}

	@Transactional
	public AnnouncementReadResponse read(Long userId, Long announcementId) {
		User user = findUser(userId);
		Announcement announcement = findAnnouncement(announcementId);
		AnnouncementRead announcementRead = announcementReadRepository.findByUserAndAnnouncement(user, announcement)
			.orElseGet(() -> announcementReadRepository.save(new AnnouncementRead(user, announcement)));
		return new AnnouncementReadResponse(announcement.getAnnouncementId(), announcementRead.getReadAt());
	}

	private Pageable createPageable(int page, int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}
		Sort sort = Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("publishedAt"));
		return PageRequest.of(page, size, sort);
	}

	private Set<Long> findReadAnnouncementIds(User user, List<Announcement> announcements) {
		if (announcements.isEmpty()) {
			return Set.of();
		}
		return announcementReadRepository.findByUserAndAnnouncementIn(user, announcements).stream()
			.map(announcementRead -> announcementRead.getAnnouncement().getAnnouncementId())
			.collect(Collectors.toSet());
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private Announcement findAnnouncement(Long announcementId) {
		return announcementRepository.findById(announcementId)
			.orElseThrow(() -> new CustomException(ErrorCode.ANNOUNCEMENT_NOT_FOUND));
	}
}

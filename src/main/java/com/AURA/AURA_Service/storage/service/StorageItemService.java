package com.AURA.AURA_Service.storage.service;

import com.AURA.AURA_Service.auth.repository.UserRepository;
import com.AURA.AURA_Service.common.CustomException;
import com.AURA.AURA_Service.common.ErrorCode;
import com.AURA.AURA_Service.scan.domain.ScannedItem;
import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.AURA.AURA_Service.scan.repository.ScannedItemRepository;
import com.AURA.AURA_Service.storage.dto.StorageItemListItemResponse;
import com.AURA.AURA_Service.storage.dto.StorageItemPageResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StorageItemService {
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 30;
	private static final int MAX_SIZE = 100;

	private final UserRepository userRepository;
	private final ScannedItemRepository scannedItemRepository;

	public StorageItemService(UserRepository userRepository, ScannedItemRepository scannedItemRepository) {
		this.userRepository = userRepository;
		this.scannedItemRepository = scannedItemRepository;
	}

	@Transactional(readOnly = true)
	public StorageItemPageResponse getItems(Long userId, ItemSource itemSource, Boolean trashed, String sort,
		Integer page, Integer size) {
		if (!userRepository.existsById(userId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(size), createSort(sort));
		Page<ScannedItem> items = scannedItemRepository.findAll(createSpecification(userId, itemSource, trashed), pageable);
		List<StorageItemListItemResponse> content = items.getContent().stream()
			.map(StorageItemListItemResponse::from)
			.toList();
		return StorageItemPageResponse.from(items, content);
	}

	private Specification<ScannedItem> createSpecification(Long userId, ItemSource itemSource, Boolean trashed) {
		return (root, query, criteriaBuilder) -> {
			var predicate = criteriaBuilder.and(
				criteriaBuilder.equal(root.get("user").get("userId"), userId),
				criteriaBuilder.equal(root.get("itemSource"), itemSource),
				criteriaBuilder.isNull(root.get("deletedAt"))
			);
			if (trashed == null) return predicate;
			return criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("isTrashed"), trashed));
		};
	}

	private Sort createSort(String sort) {
		return switch (sort == null ? "" : sort) {
			case "size_desc" -> Sort.by(Sort.Order.desc("sizeBytes"), Sort.Order.desc("itemId"));
			case "modified_desc" -> Sort.by(Sort.Order.desc("modifiedTime"), Sort.Order.desc("itemId"));
			case "created_desc" -> Sort.by(Sort.Order.desc("createdTime"), Sort.Order.desc("itemId"));
			case "oldest" -> Sort.by(Sort.Order.asc("createdTime"), Sort.Order.asc("itemId"));
			default -> Sort.by(Sort.Order.desc("itemId"));
		};
	}

	private int normalizePage(Integer page) {
		return page == null || page < 0 ? DEFAULT_PAGE : page;
	}

	private int normalizeSize(Integer size) {
		if (size == null || size < 1) return DEFAULT_SIZE;
		return Math.min(size, MAX_SIZE);
	}
}

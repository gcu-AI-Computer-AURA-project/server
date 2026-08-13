package com.AURA.AURA_Service.storage.dto;

import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StorageMoveToTrashRequest(
	@Valid @NotEmpty(message = "휴지통으로 이동할 항목이 필요합니다.")
	List<ItemRequest> items,
	@NotNull(message = "휴지통 이동 승인 여부는 필수입니다.")
	@JsonProperty("approval_confirmed") Boolean approvalConfirmed
) {
	public record ItemRequest(
		@JsonProperty("item_id") Long itemId,
		@NotNull(message = "항목 출처는 필수입니다.")
		@JsonProperty("item_source") ItemSource itemSource,
		@NotBlank(message = "외부 항목 ID는 필수입니다.")
		@JsonProperty("external_item_id") String externalItemId,
		@JsonProperty("snapshot_title") String snapshotTitle,
		@JsonProperty("snapshot_size_bytes") Long snapshotSizeBytes
	) {
	}
}

package com.AURA.AURA_Service.storage.dto;

import com.AURA.AURA_Service.scan.domain.ScannedItem.ItemSource;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StoragePermanentDeleteRequest(
	@Valid
	@NotEmpty(message = "영구 삭제할 항목 목록은 필수 입력값입니다.")
	List<ItemRequest> items,

	@NotNull(message = "영구 삭제 승인 여부는 필수 입력값입니다.")
	@JsonProperty("approval_confirmed")
	Boolean approvalConfirmed,

	@NotNull(message = "확인 문구는 필수 입력값입니다.")
	@JsonProperty("confirmation_text")
	String confirmationText
) {
	public record ItemRequest(
		@NotNull(message = "항목 출처는 필수 입력값입니다.")
		@JsonProperty("item_source")
		ItemSource itemSource,

		@NotNull(message = "외부 항목 ID는 필수 입력값입니다.")
		@JsonProperty("external_item_id")
		String externalItemId,

		@JsonProperty("item_id")
		Long itemId,

		@JsonProperty("snapshot_title")
		String snapshotTitle,

		@NotNull(message = "스냅샷 용량은 필수 입력값입니다.")
		@JsonProperty("snapshot_size_bytes")
		Long snapshotSizeBytes
	) {
	}
}

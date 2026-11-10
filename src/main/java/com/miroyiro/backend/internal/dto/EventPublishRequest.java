package com.miroyiro.backend.internal.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * scrap publish_service → POST /internal/pipeline/events.
 * body = processed_item.event_data (+ artist_id, anime_id) + processed_item_id. 키 = main.event 컬럼 이름 (snake_case).
 * 값 검사(@Pattern)는 DB CHECK 와 같은 목록 → 틀리면 409 가 아니라 400 (scrap 이 PublishInvalid 로 처리).
 */
public record EventPublishRequest(
        @NotNull Long processedItemId,
        @NotBlank @Pattern(regexp = "jpop|anime") String category,
        @NotBlank @Pattern(regexp = "concert|broadcast|streaming|popup|collabo_cafe|fair|exhibition|release|etc") String type,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String venue,
        @Size(max = 255) String address,
        @Size(max = 50) String city,
        @NotNull OffsetDateTime startAt,
        OffsetDateTime endAt,
        Boolean isAllDay,
        Boolean isInternational,
        Boolean reservationRequired,
        @Size(max = 500) String posterUrl,
        String description,
        @Size(max = 255) String priceInfo,
        @Size(max = 500) String ticketUrl,
        @NotBlank @Pattern(regexp = "kopis|anissia|animate|aniplus|x|umj|manual") String source,
        @Size(max = 100) String sourceId,
        @Size(max = 500) String sourceUrl,
        Long artistId,
        Long animeId) {
}

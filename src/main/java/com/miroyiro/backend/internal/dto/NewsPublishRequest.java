package com.miroyiro.backend.internal.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** scrap publish_service → POST /internal/pipeline/news (본문 없음, 아웃링크만). */
public record NewsPublishRequest(
        @NotNull Long processedItemId,
        @NotBlank @Pattern(regexp = "jpop|anime") String category,
        @NotBlank @Pattern(regexp = "aniplus|x|umj|manual") String source,
        @NotBlank @Size(max = 500) String sourceUrl,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 500) String summary,
        @Size(max = 500) String thumbnailUrl,
        OffsetDateTime publishedAt,
        Long artistId,
        Long animeId) {
}

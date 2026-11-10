package com.miroyiro.backend.internal.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * scrap catalog_service → POST /internal/catalog/streaming
 * { "anime_id": 7, "links": [ { "platform": "laftel", "source": "laftel", "url": "...", "external_id": "48046", "version": "dub" } ] }
 */
public record StreamingUpsertRequest(@NotNull Long animeId, @NotEmpty @Valid List<Link> links) {

    public record Link(
            @NotNull @Pattern(regexp = "netflix|tving|watcha|wavve|disney_plus|coupang_play|laftel|aniplus_tv|etc") String platform,
            @NotNull @Pattern(regexp = "manual|tmdb|anilist|wikidata|laftel|aniplus") String source,
            @Size(max = 500) String url,
            @Size(max = 30) String externalId,     // laftel 이면 anime.laftel_id 로도 저장
            String version) {                      // dub | sub | uncut (지금은 저장 안 함, 로그용)
    }
}

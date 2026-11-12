package com.miroyiro.backend.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * scrap anilist_resolver / anissia → POST /internal/catalog/anime.
 * anilist_id 또는 anissia_no 로 기존 행을 찾고 없으면 생성. null 인 값은 기존 값을 지우지 않음.
 * (format·episodes·genres·studios 등은 /api/anime 만들 때 엔티티에 추가하면서 같이 반영)
 */
public record AnimeUpsertRequest(
        Integer anilistId,
        Integer anissiaNo,
        Integer tmdbId,
        @NotBlank @Size(max = 200) String titleRomaji,
        @Size(max = 200) String titleKo,
        @Size(max = 200) String titleNative,
        @Size(max = 200) String titleEn,
        @Pattern(regexp = "winter|spring|summer|fall") String season,
        Short seasonYear,
        @Pattern(regexp = "not_yet_released|releasing|finished|cancelled|hiatus") String status,
        @Size(max = 500) String posterUrl) {
}

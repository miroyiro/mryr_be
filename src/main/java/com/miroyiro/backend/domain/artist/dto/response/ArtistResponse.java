package com.miroyiro.backend.domain.artist.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.miroyiro.backend.domain.artist.entity.Artist;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ArtistResponse(
        Long id,
        String name,
        String nameEn,
        String nameJa,
        String category,
        String profileImage,
        String description,
        LocalDate debutDate,
        String officialUrl,
        long followerCount,
        Boolean isFollowed
) {
    public static ArtistResponse from(
            Artist artist,
            long followerCount,
            Boolean isFollowed
    ) {
        return new ArtistResponse(
                artist.getId(),
                artist.getName(),
                artist.getNameEn(),
                artist.getNameJa(),
                artist.getCategory(),
                artist.getProfileImage(),
                artist.getDescription(),
                artist.getDebutDate(),
                artist.getOfficialUrl(),
                followerCount,
                isFollowed
        );
    }
}

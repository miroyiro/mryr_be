package com.miroyiro.backend.domain.artist.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ArtistFollowResponse(
        Long artistId,
        boolean isFollowed
) {
}

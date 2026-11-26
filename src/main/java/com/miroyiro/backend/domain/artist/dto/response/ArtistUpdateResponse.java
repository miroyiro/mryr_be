package com.miroyiro.backend.domain.artist.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ArtistUpdateResponse(
        Long id,
        OffsetDateTime updatedAt
) {
}

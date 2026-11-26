package com.miroyiro.backend.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JpopUpcomingResponse(

        Long id,

        ArtistInfo artist,

        String title,

        String type,

        String venue,

        String city,

        String country,

        OffsetDateTime startAt,

        OffsetDateTime endAt,

        String status,

        @JsonProperty("is_international")
        boolean international,

        String ticketUrl,

        Long calendarId

) {

    public record ArtistInfo(
            Long id,
            String name
    ) {
    }
}

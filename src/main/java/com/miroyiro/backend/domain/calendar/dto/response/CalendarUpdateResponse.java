package com.miroyiro.backend.domain.calendar.dto.response;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CalendarUpdateResponse(

        Long calendarId,
        String status,
        String memo,

        @JsonProperty("is_alert")
        boolean alert,

        OffsetDateTime updatedAt

) {
}

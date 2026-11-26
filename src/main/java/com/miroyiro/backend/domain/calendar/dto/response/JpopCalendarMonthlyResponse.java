package com.miroyiro.backend.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record JpopCalendarMonthlyResponse(

        Long calendarId,

        Long eventId,

        String title,

        String type,

        OffsetDateTime startAt,

        OffsetDateTime endAt,

        @JsonProperty("is_all_day")
        boolean allDay,

        String status

) {
}

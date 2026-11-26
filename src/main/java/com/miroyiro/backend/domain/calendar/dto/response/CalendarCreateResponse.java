package com.miroyiro.backend.domain.calendar.dto.response;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record CalendarCreateResponse(

        Long calendarId,
        Long eventId,
        String status,
        OffsetDateTime createdAt

) {
}

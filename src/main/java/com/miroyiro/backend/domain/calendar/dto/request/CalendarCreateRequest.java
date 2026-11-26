package com.miroyiro.backend.domain.calendar.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CalendarCreateRequest(

        @JsonProperty("event_id")
        @NotNull(message = "event_id is required")
        @Positive(message = "event_id must be positive")
        Long eventId,

        @JsonProperty("is_alert")
        @NotNull(message = "is_alert is required")
        Boolean alert,

        @Size(max = 500, message = "memo must not exceed 500 characters")
        String memo

) {
}

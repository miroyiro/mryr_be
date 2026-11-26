package com.miroyiro.backend.domain.calendar.dto.request;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CalendarUpdateRequest {

    private final Set<String> providedFields = new HashSet<>();

    @Pattern(
            regexp = "planned|done|canceled",
            message = "Invalid calendar status"
    )
    private String status;

    @Size(
            max = 500,
            message = "memo must not exceed 500 characters"
    )
    private String memo;

    private Boolean alert;

    @JsonSetter(value = "status", nulls = Nulls.FAIL)
    public void setStatus(String status) {
        this.status = status;
        providedFields.add("status");
    }

    @JsonSetter(value = "memo", nulls = Nulls.SET)
    public void setMemo(String memo) {
        this.memo = memo;
        providedFields.add("memo");
    }

    @JsonSetter(value = "is_alert", nulls = Nulls.FAIL)
    public void setAlert(Boolean alert) {
        this.alert = alert;
        providedFields.add("is_alert");
    }

    public String status() {
        return status;
    }

    public String memo() {
        return memo;
    }

    public Boolean alert() {
        return alert;
    }

    public boolean hasField(String field) {
        return providedFields.contains(field);
    }

    public boolean isEmpty() {
        return providedFields.isEmpty();
    }
}

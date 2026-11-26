package com.miroyiro.backend.domain.calendar.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "user_calendar",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_calendar_user_event",
                        columnNames = {"user_id", "event_id"}
                )
        }
)
public class UserCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    // planned | done | canceled
    @Column(name = "status", length = 10, nullable = false)
    private String status = "planned";

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "is_alert", nullable = false)
    private boolean alert = true;

    @Column(
            name = "created_at",
            insertable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(
            name = "updated_at",
            insertable = false,
            updatable = false
    )
    private OffsetDateTime updatedAt;

    protected UserCalendar() {
    }

    public UserCalendar(
            Long userId,
            Long eventId,
            boolean alert,
            String memo
    ) {
        this.userId = userId;
        this.eventId = eventId;
        this.status = "planned";
        this.alert = alert;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getEventId() {
        return eventId;
    }

    public String getStatus() {
        return status;
    }

    public String getMemo() {
        return memo;
    }

    public boolean isAlert() {
        return alert;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }

    public void updateAlert(boolean alert) {
        this.alert = alert;
    }
}

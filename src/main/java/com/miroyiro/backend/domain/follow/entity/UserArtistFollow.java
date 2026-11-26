package com.miroyiro.backend.domain.follow.entity;

import java.math.BigDecimal;
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
    name = "user_artist_follow",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_artist_follow",
            columnNames = {"user_id", "artist_id"}
        )
    }
)
public class UserArtistFollow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artist_id")
    private Long artistId;

    @Column(name = "raw_input", length = 100)
    private String rawInput;

    @Column(name = "match_status", length = 10, nullable = false)
    private String matchStatus;

    @Column(name = "match_score", precision = 4, scale = 3)
    private BigDecimal matchScore;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected UserArtistFollow() {
    }

    public UserArtistFollow(Long userId, Long artistId) {
        this.userId = userId;
        this.artistId = artistId;
        this.matchStatus = "matched";
        this.resolvedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getArtistId() {
        return artistId;
    }

    public String getRawInput() {
        return rawInput;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public BigDecimal getMatchScore() {
        return matchScore;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

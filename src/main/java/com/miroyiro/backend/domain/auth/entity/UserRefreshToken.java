package com.miroyiro.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", length = 64, nullable = false)
    private String tokenHash;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    // Refresh Token 생성
    public static UserRefreshToken create(
            User user,
            String tokenHash,
            String userAgent,
            OffsetDateTime expiresAt
    ) {
        UserRefreshToken token = new UserRefreshToken();

        token.user = user;
        token.tokenHash = tokenHash;
        token.userAgent = userAgent;
        token.expiresAt = expiresAt;
        token.revokedAt = null;

        return token;
    }

    // 만료 여부
    public boolean isExpired() {
        return !expiresAt.isAfter(OffsetDateTime.now());
    }

    // 폐기 여부
    public boolean isRevoked() {
        return revokedAt != null;
    }

    // Refresh Token 폐기
    public void revoke() {
        if (this.revokedAt == null) {
            this.revokedAt = OffsetDateTime.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}

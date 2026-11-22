package com.miroyiro.backend.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nickname", length = 30)
    private String nickname;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "oauth_provider", length = 10)
    private String oauthProvider;

    @Column(name = "oauth_id", length = 100)
    private String oauthId;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "role", length = 10)
    private String role;

    @Column(name = "onboarded_at")
    private OffsetDateTime onboardedAt;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 이메일 회원가입
    public static User createEmailUser(
            String email,
            String passwordHash,
            String nickname
    ) {
        User user = new User();

        user.email = email;
        user.passwordHash = passwordHash;
        user.nickname = nickname;
        user.role = "user";
        user.oauthProvider = "local";
        user.oauthId = null;
        user.onboardedAt = null;

        return user;
    }

    // 소셜 회원가입
    public static User createOAuthUser(
            String provider,
            String oauthId,
            String email,
            String nickname,
            String profileImage
    ) {
        User user = new User();

        user.oauthProvider = provider;
        user.oauthId = oauthId;
        user.email = email;
        user.nickname = nickname;
        user.profileImage = profileImage;
        user.passwordHash = null;
        user.role = "user";
        user.onboardedAt = null;

        return user;
    }

    // 마지막 로그인 시간 갱신
    public void updateLastLoginAt() {
        this.lastLoginAt = OffsetDateTime.now();
    }

    // 온보딩 완료 여부
    public boolean isOnboarded() {
        return this.onboardedAt != null;
    }

    // 탈퇴 여부
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // 생성 시각 자동 설정
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        this.updatedAt = now;
    }

    // 수정 시각 자동 설정
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

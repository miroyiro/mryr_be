package com.miroyiro.backend.domain.auth.service.oauth;

public record OAuthProfile(
        String provider,
        String oauthId,
        String email,
        String nickname,
        String profileImage
) {}

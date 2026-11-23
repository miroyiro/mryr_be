package com.miroyiro.backend.domain.auth.dto.response;

import com.miroyiro.backend.domain.auth.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MeResponse(
        Long id,
        String email,
        String nickname,

        @JsonProperty("profile_image")
        String profileImage,

        @JsonProperty("oauth_provider")
        String oauthProvider,

        String role,
        boolean onboarded
) {
    public static MeResponse from(User user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImage(),
                user.getOauthProvider(),
                user.getRole(),
                user.isOnboarded()
        );
    }
}

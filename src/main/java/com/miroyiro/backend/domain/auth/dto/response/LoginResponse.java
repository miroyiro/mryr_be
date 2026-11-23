package com.miroyiro.backend.domain.auth.dto.response;

import com.miroyiro.backend.domain.auth.entity.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("expires_in")
        long expiresIn,

        UserInfo user
) {
    public record UserInfo(
            Long id,
            String email,
            String nickname,
            String role,
            boolean onboarded
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getRole(),
                    user.isOnboarded()
            );
        }
    }

    public static LoginResponse of(
            String accessToken,
            long expiresIn,
            User user
    ) {
        return new LoginResponse(
                accessToken,
                expiresIn,
                UserInfo.from(user)
        );
    }
}

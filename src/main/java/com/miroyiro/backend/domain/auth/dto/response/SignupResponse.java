package com.miroyiro.backend.domain.auth.dto.response;

import com.miroyiro.backend.domain.auth.entity.User;

public record SignupResponse(
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

    public static SignupResponse from(User user) {
        return new SignupResponse(
                UserInfo.from(user)
        );
    }
}

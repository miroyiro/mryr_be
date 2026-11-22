package com.miroyiro.backend.domain.auth.service.oauth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
public class OAuthStateService {

    private static final String COOKIE_PREFIX = "oauth_state_";

    private final SecureRandom secureRandom = new SecureRandom();
    private final OAuthProperties properties;

    public OAuthStateService(OAuthProperties properties) {
        this.properties = properties;
    }

    // OAuth 로그인용 state 생성
    public String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // state 저장 쿠키 생성
    public ResponseCookie createStateCookie(
            OAuthProvider provider,
            String state
    ) {
        return ResponseCookie.from(
                        cookieName(provider),
                        state
                )
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/api/auth/" + provider.getValue() + "/callback")
                .maxAge(Duration.ofMinutes(5))
                .build();
    }

    // 콜백에서 state 비교
    public void validateState(
            String expectedState,
            String actualState
    ) {
        if (expectedState == null || actualState == null
                || expectedState.isBlank()
                || actualState.isBlank()) {
            throw invalidState();
        }

        boolean matches = MessageDigest.isEqual(
                expectedState.getBytes(StandardCharsets.UTF_8),
                actualState.getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            throw invalidState();
        }
    }

    // state 쿠키 삭제
    public ResponseCookie deleteStateCookie(
            OAuthProvider provider
    ) {
        return ResponseCookie.from(cookieName(provider), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/api/auth/" + provider.getValue() + "/callback")
                .maxAge(Duration.ZERO)
                .build();
    }

    public String cookieName(OAuthProvider provider) {
        return COOKIE_PREFIX + provider.getValue();
    }

    private ResponseStatusException invalidState() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid OAuth state"
        );
    }
}

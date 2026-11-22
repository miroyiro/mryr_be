package com.miroyiro.backend.domain.auth.service.oauth;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public enum OAuthProvider {

    GOOGLE("google"),
    KAKAO("kakao"),
    NAVER("naver");

    private final String value;

    OAuthProvider(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static OAuthProvider from(String provider) {
        for (OAuthProvider value : values()) {
            if (value.value.equals(provider)) {
                return value;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported OAuth provider"
        );
    }
}

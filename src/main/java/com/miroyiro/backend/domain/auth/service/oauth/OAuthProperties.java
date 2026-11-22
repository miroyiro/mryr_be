package com.miroyiro.backend.domain.auth.service.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuthProperties {

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${oauth.naver.client-id}")
    private String naverClientId;

    @Value("${oauth.naver.client-secret}")
    private String naverClientSecret;

    @Value("${oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${oauth.frontend-url}")
    private String frontendUrl;

    @Value("${auth.cookie.secure:true}")
    private boolean cookieSecure;

    public record ClientConfig(
            String clientId,
            String clientSecret,
            String redirectUri
    ) {}

    public ClientConfig getClient(OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> new ClientConfig(
                    googleClientId,
                    googleClientSecret,
                    googleRedirectUri
            );
            case KAKAO -> new ClientConfig(
                    kakaoClientId,
                    kakaoClientSecret,
                    kakaoRedirectUri
            );
            case NAVER -> new ClientConfig(
                    naverClientId,
                    naverClientSecret,
                    naverRedirectUri
            );
        };
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }
}

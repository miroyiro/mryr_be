package com.miroyiro.backend.domain.auth.service.oauth;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class OAuthClient {

    private final OAuthProperties properties;
    private final RestClient restClient;

    public OAuthClient(OAuthProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    // 제공자별 로그인 URL 생성
    public String getAuthorizationUrl(
            OAuthProvider provider,
            String state
    ) {
        OAuthProperties.ClientConfig config =
                properties.getClient(provider);

        String baseUrl = switch (provider) {
            case GOOGLE ->
                    "https://accounts.google.com/o/oauth2/v2/auth";
            case KAKAO ->
                    "https://kauth.kakao.com/oauth/authorize";
            case NAVER ->
                    "https://nid.naver.com/oauth2.0/authorize";
        };

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(baseUrl)
                        .queryParam("response_type", "code")
                        .queryParam("client_id", config.clientId())
                        .queryParam("redirect_uri", config.redirectUri())
                        .queryParam("state", state);

        if (provider == OAuthProvider.GOOGLE) {
            builder.queryParam("scope", "openid email profile");
        }

        return builder.build().encode().toUriString();
    }

    // 인가 코드 → 제공자 Access Token
    public String exchangeCode(
            OAuthProvider provider,
            String code,
            String state
    ) {
        OAuthProperties.ClientConfig config =
                properties.getClient(provider);

        String tokenUrl = switch (provider) {
            case GOOGLE -> "https://oauth2.googleapis.com/token";
            case KAKAO -> "https://kauth.kakao.com/oauth/token";
            case NAVER -> "https://nid.naver.com/oauth2.0/token";
        };

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("grant_type", "authorization_code");
        form.add("client_id", config.clientId());
        form.add("code", code);

        if (provider == OAuthProvider.NAVER) {
            form.add("client_secret", config.clientSecret());
            form.add("state", state);
        } else {
            form.add("redirect_uri", config.redirectUri());

            if (config.clientSecret() != null
                    && !config.clientSecret().isBlank()) {
                form.add("client_secret", config.clientSecret());
            }
        }

        try {
            JsonNode response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            String accessToken = text(response, "access_token");

            if (accessToken == null) {
                throw providerError();
            }

            return accessToken;

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OAuth token request failed",
                    ex
            );
        }
    }

    // 제공자 Access Token → 사용자 정보
    public OAuthProfile getProfile(
            OAuthProvider provider,
            String accessToken
    ) {
        String profileUrl = switch (provider) {
            case GOOGLE ->
                    "https://openidconnect.googleapis.com/v1/userinfo";
            case KAKAO ->
                    "https://kapi.kakao.com/v2/user/me";
            case NAVER ->
                    "https://openapi.naver.com/v1/nid/me";
        };

        try {
            JsonNode response = restClient.get()
                    .uri(profileUrl)
                    .headers(headers ->
                            headers.setBearerAuth(accessToken)
                    )
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw providerError();
            }

            return switch (provider) {
                case GOOGLE -> parseGoogle(response);
                case KAKAO -> parseKakao(response);
                case NAVER -> parseNaver(response);
            };

        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "OAuth profile request failed",
                    ex
            );
        }
    }

    private OAuthProfile parseGoogle(JsonNode json) {
        String id = text(json, "sub");

        if (id == null) {
            throw providerError();
        }

        String email = json.path("email_verified")
                .asBoolean(false)
                ? text(json, "email")
                : null;

        return new OAuthProfile(
                "google",
                id,
                email,
                text(json, "name"),
                text(json, "picture")
        );
    }

    private OAuthProfile parseKakao(JsonNode json) {
        String id = text(json, "id");

        if (id == null) {
            throw providerError();
        }

        JsonNode account = json.path("kakao_account");
        JsonNode profile = account.path("profile");

        String email = account.path("is_email_verified")
                .asBoolean(false)
                ? text(account, "email")
                : null;

        String nickname = text(profile, "nickname");

        if (nickname == null) {
            nickname = text(
                    json.path("properties"),
                    "nickname"
            );
        }

        String image = text(profile, "profile_image_url");

        if (image == null) {
            image = text(
                    json.path("properties"),
                    "profile_image"
            );
        }

        return new OAuthProfile(
                "kakao",
                id,
                email,
                nickname,
                image
        );
    }

    private OAuthProfile parseNaver(JsonNode json) {
        if (!"00".equals(text(json, "resultcode"))) {
            throw providerError();
        }

        JsonNode profile = json.path("response");
        String id = text(profile, "id");

        if (id == null) {
            throw providerError();
        }

        return new OAuthProfile(
                "naver",
                id,
                text(profile, "email"),
                text(profile, "nickname"),
                text(profile, "profile_image")
        );
    }

    private String text(JsonNode json, String field) {
        if (json == null || json.isMissingNode()) {
            return null;
        }

        JsonNode value = json.path(field);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        String result = value.asText();

        return result.isBlank() ? null : result;
    }

    private ResponseStatusException providerError() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Invalid OAuth provider response"
        );
    }
}

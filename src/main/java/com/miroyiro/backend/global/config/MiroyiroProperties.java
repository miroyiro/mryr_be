package com.miroyiro.backend.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 miroyiro.* (값은 환경변수). */
@ConfigurationProperties(prefix = "miroyiro")
public record MiroyiroProperties(Internal internal, Scrap scrap, Jwt jwt, Cors cors) {

    /** scrap → backend /internal/** 에서 받는 키 (X-Internal-Key). */
    public record Internal(String key) {}

    /** backend → scrap 호출 (관리자 파이프라인 프록시, /internal/anilist/* 등). */
    public record Scrap(String baseUrl, String internalKey) {}

    public record Jwt(String secret) {}

    public record Cors(List<String> allowedOrigins) {}
}

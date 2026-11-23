package com.miroyiro.backend.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.time.Instant;
import java.util.Date;

@Component
public class JwtProvider {

    private static final long ACCESS_TOKEN_EXPIRES_IN = 1800L;

    private final SecretKey secretKey;

    public JwtProvider(
            @Value("${jwt.secret}") String secret
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 bytes"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // Access Token 생성
    public String createAccessToken(
            Long userId,
            String role
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("token_type", "access")
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(
                                now.plusSeconds(ACCESS_TOKEN_EXPIRES_IN)
                        )
                )
                .signWith(secretKey)
                .compact();
    }

    // Access Token 검증
    public Claims validateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(
                "token_type",
                String.class
        );

        if (!"access".equals(tokenType)) {
            throw new JwtException(
                    "Invalid token type"
            );
        }

        String subject = claims.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new JwtException(
                    "Missing user ID"
            );
        }

        try {
            Long userId = Long.valueOf(subject);

            if (userId <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            throw new JwtException(
                    "Invalid user ID",
                    e
            );
        }

        return claims;
    }

    // Access Token에서 사용자 ID 추출
    public Long getUserId(String token) {
        Claims claims = validateAccessToken(token);

        return Long.valueOf(claims.getSubject());
    }

    // Access Token 유효기간 반환 (초)
    public long getAccessTokenExpiresIn() {
        return ACCESS_TOKEN_EXPIRES_IN;
    }
}

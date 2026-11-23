package com.miroyiro.backend.domain.auth.service;

import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.entity.UserRefreshToken;
import com.miroyiro.backend.domain.auth.repository.RefreshTokenRepository;
import com.miroyiro.backend.global.security.jwt.JwtProvider;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final long REFRESH_TOKEN_DAYS = 14L;

    private static final int REFRESH_TOKEN_BYTES = 32;

    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProvider jwtProvider
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProvider = jwtProvider;
    }

    // Access Token + Refresh Token 발급 결과
    public record TokenPair(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {}

    // 최초 로그인 시 토큰 발급
    @Transactional
    public TokenPair issueTokens(
            User user,
            String userAgent
    ) {
        String accessToken = jwtProvider.createAccessToken(
                user.getId(),
                user.getRole()
        );

        String refreshToken = generateRefreshToken();

        String tokenHash = hashRefreshToken(refreshToken);

        UserRefreshToken tokenEntity = UserRefreshToken.create(
                user,
                tokenHash,
                normalizeUserAgent(userAgent),
                OffsetDateTime.now().plusDays(REFRESH_TOKEN_DAYS)
        );

        refreshTokenRepository.save(tokenEntity);

        return new TokenPair(
                accessToken,
                refreshToken,
                jwtProvider.getAccessTokenExpiresIn()
        );
    }

    // Refresh Token으로 Access Token 재발급
    @Transactional
    public TokenPair refresh(
            String refreshToken,
            String userAgent
    ) {
        UserRefreshToken existingToken =
                findRefreshTokenForUpdate(refreshToken);

        // 이미 폐기된 토큰
        if (existingToken.isRevoked()) {
            throw unauthorized("Refresh token has been revoked");
        }

        // 만료된 토큰
        if (existingToken.isExpired()) {
            throw unauthorized("Refresh token has expired");
        }

        User user = existingToken.getUser();

        // 탈퇴한 사용자
        if (user.isDeleted()) {
            throw unauthorized("User has been deleted");
        }

        // 기존 Refresh Token 폐기
        existingToken.revoke();

        // 새 Access Token + Refresh Token 발급
        return issueTokens(user, userAgent);
    }

    // 현재 기기 로그아웃
    @Transactional
    public void revoke(
            String refreshToken,
            Long userId
    ) {
        UserRefreshToken existingToken =
                findRefreshTokenForUpdate(refreshToken);

        // 다른 사용자의 토큰으로 로그아웃하는 것 방지
        if (!existingToken.getUser().getId().equals(userId)) {
            throw unauthorized("Refresh token does not belong to user");
        }

        // 이미 만료되었거나 폐기된 토큰
        if (existingToken.isExpired() || existingToken.isRevoked()) {
            throw unauthorized("Invalid refresh token");
        }

        // 현재 기기의 Refresh Token만 폐기
        existingToken.revoke();
    }

    // Refresh Token 조회 + DB 행 잠금
    private UserRefreshToken findRefreshTokenForUpdate(
            String refreshToken
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw unauthorized("Refresh token is required");
        }

        String tokenHash = hashRefreshToken(refreshToken);

        return refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() ->
                        unauthorized("Invalid refresh token")
                );
    }

    // 안전한 랜덤 Refresh Token 생성
    private String generateRefreshToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];

        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    // Refresh Token SHA-256 해시
    private String hashRefreshToken(String refreshToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    refreshToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    e
            );
        }
    }

    // User-Agent 길이 제한
    private String normalizeUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }

        if (userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }

        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }

    // 인증 실패 예외
    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                message
        );
    }
}

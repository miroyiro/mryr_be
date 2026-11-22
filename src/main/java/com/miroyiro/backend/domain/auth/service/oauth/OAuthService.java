package com.miroyiro.backend.domain.auth.service.oauth;

import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.repository.AuthRepository;
import com.miroyiro.backend.domain.auth.service.TokenService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class OAuthService {

    private final OAuthClient oauthClient;
    private final OAuthStateService stateService;
    private final AuthRepository authRepository;
    private final TokenService tokenService;

    public OAuthService(
            OAuthClient oauthClient,
            OAuthStateService stateService,
            AuthRepository authRepository,
            TokenService tokenService
    ) {
        this.oauthClient = oauthClient;
        this.stateService = stateService;
        this.authRepository = authRepository;
        this.tokenService = tokenService;
    }

    // 로그인 시작 결과
    public record OAuthStartResult(
            String authorizationUrl,
            String state
    ) {}

    // 로그인 완료 결과
    public record OAuthLoginResult(
            Long userId,
            String refreshToken
    ) {}

    // OAuth 로그인 시작
    public OAuthStartResult start(String providerName) {
        OAuthProvider provider =
                OAuthProvider.from(providerName);

        String state = stateService.generateState();

        String authorizationUrl =
                oauthClient.getAuthorizationUrl(
                        provider,
                        state
                );

        return new OAuthStartResult(
                authorizationUrl,
                state
        );
    }

    // OAuth 콜백 처리
    @Transactional
    public OAuthLoginResult callback(
            String providerName,
            String code,
            String state,
            String expectedState,
            String userAgent
    ) {
        OAuthProvider provider =
                OAuthProvider.from(providerName);

        // CSRF state 검증
        stateService.validateState(
                expectedState,
                state
        );

        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Authorization code is required"
            );
        }

        // 인가 코드 → 제공자 Access Token
        String providerAccessToken =
                oauthClient.exchangeCode(
                        provider,
                        code,
                        state
                );

        // 제공자 사용자 정보 조회
        OAuthProfile profile =
                oauthClient.getProfile(
                        provider,
                        providerAccessToken
                );

        // 사용자 조회 또는 신규 생성
        User user = findOrCreateUser(profile);

        // 마지막 로그인 시간 갱신
        user.updateLastLoginAt();

        // 우리 서비스의 Access / Refresh Token 발급
        TokenService.TokenPair tokens =
                tokenService.issueTokens(
                        user,
                        userAgent
                );

        return new OAuthLoginResult(
                user.getId(),
                tokens.refreshToken()
        );
    }

    // 기존 사용자 조회 또는 신규 사용자 생성
    private User findOrCreateUser(OAuthProfile profile) {

        var existing = authRepository
                .findByOauthProviderAndOauthIdAndDeletedAtIsNull(
                        profile.provider(),
                        profile.oauthId()
                );

        if (existing.isPresent()) {
            return existing.get();
        }

        String email = normalizeEmail(profile.email());

        // 동일 이메일이 이미 있으면 자동 계정 연결하지 않음
        if (email != null && authRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already belongs to another account"
            );
        }

        // 소셜 신규 회원은 충돌 가능성이 낮은 임시 닉네임 사용
        String nickname = generateNickname(profile);

        if (authRepository.existsByNickname(nickname)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Generated nickname already exists"
            );
        }

        User user = User.createOAuthUser(
                profile.provider(),
                profile.oauthId(),
                email,
                nickname,
                profile.profileImage()
        );

        return authRepository.saveAndFlush(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        String normalized =
                email.trim().toLowerCase(Locale.ROOT);

        if (normalized.length() > 100) {
            return null;
        }

        return normalized;
    }

    // 최대 30자의 임시 닉네임
    private String generateNickname(OAuthProfile profile) {
        String prefix = switch (profile.provider()) {
            case "google" -> "g_";
            case "kakao" -> "k_";
            case "naver" -> "n_";
            default -> "u_";
        };

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    (profile.provider() + ":" + profile.oauthId())
                            .getBytes(StandardCharsets.UTF_8)
            );

            return prefix
                    + HexFormat.of()
                            .formatHex(hash)
                            .substring(0, 24);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    e
            );
        }
    }
}

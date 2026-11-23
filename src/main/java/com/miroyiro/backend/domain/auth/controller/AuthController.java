package com.miroyiro.backend.domain.auth.controller;

import com.miroyiro.backend.domain.auth.dto.request.LoginRequest;
import com.miroyiro.backend.domain.auth.dto.request.SignupRequest;

import com.miroyiro.backend.domain.auth.dto.response.AuthResponse;
import com.miroyiro.backend.domain.auth.dto.response.LoginResponse;
import com.miroyiro.backend.domain.auth.dto.response.LogoutResponse;
import com.miroyiro.backend.domain.auth.dto.response.MeResponse;
import com.miroyiro.backend.domain.auth.dto.response.SignupResponse;
import com.miroyiro.backend.domain.auth.dto.response.TokenResponse;

import com.miroyiro.backend.domain.auth.service.AuthService;
import com.miroyiro.backend.domain.auth.service.TokenService;

import com.miroyiro.backend.domain.auth.service.oauth.OAuthProperties;
import com.miroyiro.backend.domain.auth.service.oauth.OAuthProvider;
import com.miroyiro.backend.domain.auth.service.oauth.OAuthService;
import com.miroyiro.backend.domain.auth.service.oauth.OAuthStateService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final long REFRESH_TOKEN_DAYS = 14L;

    private final AuthService authService;
    private final TokenService tokenService;
    private final OAuthService oauthService;
    private final OAuthStateService oauthStateService;
    private final OAuthProperties oauthProperties;

    public AuthController(
            AuthService authService,
            TokenService tokenService,
            OAuthService oauthService,
            OAuthStateService oauthStateService,
            OAuthProperties oauthProperties
    ) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.oauthService = oauthService;
        this.oauthStateService = oauthStateService;
        this.oauthProperties = oauthProperties;
    }

    // =====================================================
    // 1. 이메일 회원가입
    // POST /api/auth/signup
    // =====================================================

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse result = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new AuthResponse<>(result));
    }

    // =====================================================
    // 2. 이메일 로그인
    // POST /api/auth/login
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthService.LoginResult result = authService.login(
                request,
                userAgent
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshCookie(
                                result.refreshToken()
                        ).toString()
                )
                .body(
                        new AuthResponse<>(
                                result.response()
                        )
                );
    }

    // =====================================================
    // 3. 로그인 사용자 정보 조회
    // GET /api/auth/me
    // =====================================================

    @GetMapping("/me")
    public ResponseEntity<AuthResponse<MeResponse>> me(
            Authentication authentication
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        MeResponse result = authService.me(userId);

        return ResponseEntity.ok(
                new AuthResponse<>(result)
        );
    }

    // =====================================================
    // 4. Access Token 재발급
    // POST /api/auth/refresh
    // =====================================================

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse<TokenResponse>> refresh(
            @CookieValue(
                    value = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken,
            HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");

        TokenService.TokenPair tokens = tokenService.refresh(
                refreshToken,
                userAgent
        );

        TokenResponse response = new TokenResponse(
                tokens.accessToken(),
                tokens.expiresIn()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        createRefreshCookie(
                                tokens.refreshToken()
                        ).toString()
                )
                .body(
                        new AuthResponse<>(response)
                );
    }

    // =====================================================
    // 5. 로그아웃
    // POST /api/auth/logout
    // =====================================================

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse<LogoutResponse>> logout(
            Authentication authentication,
            @CookieValue(
                    value = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {
        Long userId = getAuthenticatedUserId(authentication);

        tokenService.revoke(
                refreshToken,
                userId
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteRefreshCookie().toString()
                )
                .body(
                        new AuthResponse<>(
                                new LogoutResponse(true)
                        )
                );
    }

    // =====================================================
    // 6. OAuth 로그인 시작
    // GET /api/auth/{provider}
    // =====================================================

    @GetMapping("/{provider:google|kakao|naver}")
    public ResponseEntity<Void> oauthStart(
            @PathVariable String provider
    ) {
        OAuthProvider oauthProvider =
                OAuthProvider.from(provider);

        OAuthService.OAuthStartResult result =
                oauthService.start(provider);

        ResponseCookie stateCookie =
                oauthStateService.createStateCookie(
                        oauthProvider,
                        result.state()
                );

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.SET_COOKIE,
                        stateCookie.toString()
                )
                .location(
                        URI.create(result.authorizationUrl())
                )
                .build();
    }

    // =====================================================
    // 7. OAuth 로그인 콜백
    // GET /api/auth/{provider}/callback
    // =====================================================

    @GetMapping("/{provider:google|kakao|naver}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,

            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,

            @RequestParam(
                    name = "error_description",
                    required = false
            ) String errorDescription,

            HttpServletRequest httpRequest
    ) {
        OAuthProvider oauthProvider =
                OAuthProvider.from(provider);

        String expectedState = readCookie(
                httpRequest,
                oauthStateService.cookieName(oauthProvider)
        );

        ResponseCookie deleteStateCookie =
                oauthStateService.deleteStateCookie(oauthProvider);

        try {
            // 제공자가 오류를 반환한 경우에도 state 검증
            oauthStateService.validateState(
                    expectedState,
                    state
            );

            if (error != null && !error.isBlank()) {
                return oauthFailure(
                        "oauth_denied",
                        deleteStateCookie
                );
            }

            OAuthService.OAuthLoginResult result =
                    oauthService.callback(
                            provider,
                            code,
                            state,
                            expectedState,
                            httpRequest.getHeader("User-Agent")
                    );

            ResponseCookie refreshCookie =
                    createRefreshCookie(
                            result.refreshToken()
                    );

            URI redirectUri = frontendRedirect("/auth/done", null);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            deleteStateCookie.toString()
                    )
                    .header(
                            HttpHeaders.SET_COOKIE,
                            refreshCookie.toString()
                    )
                    .location(redirectUri)
                    .build();

        } catch (ResponseStatusException ex) {
            String errorCode = switch (ex.getStatusCode().value()) {
                case 400 -> "invalid_oauth_request";
                case 409 -> "account_conflict";
                case 502, 503 -> "provider_unavailable";
                default -> "oauth_failed";
            };

            return oauthFailure(
                    errorCode,
                    deleteStateCookie
            );

        } catch (Exception ex) {
            return oauthFailure(
                    "oauth_failed",
                    deleteStateCookie
            );
        }
    }

    // =====================================================
    // 공통 메서드
    // =====================================================

    // 인증된 사용자 ID 추출
    private Long getAuthenticatedUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        try {
            return Long.valueOf(authentication.getName());

        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authenticated user"
            );
        }
    }

    // Refresh Token 쿠키 생성
    private ResponseCookie createRefreshCookie(String token) {
        return ResponseCookie
                .from(REFRESH_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(oauthProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ofDays(REFRESH_TOKEN_DAYS))
                .build();
    }

    // Refresh Token 쿠키 삭제
    private ResponseCookie deleteRefreshCookie() {
        return ResponseCookie
                .from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(oauthProperties.isCookieSecure())
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }

    // 요청 쿠키 조회
    private String readCookie(
            HttpServletRequest request,
            String cookieName
    ) {
        if (request.getCookies() == null) {
            return null;
        }

        for (var cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    // 프론트엔드 리다이렉트 주소 생성
    private URI frontendRedirect(
            String path,
            String errorCode
    ) {
        String frontendUrl =
                oauthProperties.getFrontendUrl();

        UriComponentsBuilder builder =
                UriComponentsBuilder
                        .fromUriString(frontendUrl)
                        .path(path);

        if (errorCode != null) {
            builder.queryParam("error", errorCode);
        }

        return builder.build().encode().toUri();
    }

    // OAuth 실패 리다이렉트
    private ResponseEntity<Void> oauthFailure(
            String errorCode,
            ResponseCookie deleteStateCookie
    ) {
        URI redirectUri = frontendRedirect(
                "/login",
                errorCode
        );

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteStateCookie.toString()
                )
                .location(redirectUri)
                .build();
    }
}

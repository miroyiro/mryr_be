package com.miroyiro.backend.domain.auth.service.oauth;

import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.repository.AuthRepository;
import com.miroyiro.backend.domain.auth.service.TokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private OAuthClient oauthClient;

    @Mock
    private OAuthStateService stateService;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenService tokenService;

    private OAuthService oauthService;

    @BeforeEach
    void setUp() {
        oauthService = new OAuthService(
                oauthClient,
                stateService,
                authRepository,
                tokenService
        );
    }

    @Test
    void start_google_success() {
        when(stateService.generateState())
                .thenReturn("random-state");

        when(oauthClient.getAuthorizationUrl(
                OAuthProvider.GOOGLE,
                "random-state"
        )).thenReturn(
                "https://accounts.google.com/o/oauth2/v2/auth"
        );

        var result = oauthService.start("google");

        assertEquals("random-state", result.state());

        assertTrue(
                result.authorizationUrl()
                        .startsWith("https://accounts.google.com")
        );
    }

    @Test
    void start_unsupportedProvider_returns400() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> oauthService.start("apple")
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );
    }

    @Test
    void callback_existingUser_success() throws Exception {
        User user = User.createOAuthUser(
                "google",
                "google-123",
                "test@example.com",
                "tester",
                null
        );

        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);

        OAuthProfile profile = new OAuthProfile(
                "google",
                "google-123",
                "test@example.com",
                "tester",
                null
        );

        when(oauthClient.exchangeCode(
                OAuthProvider.GOOGLE,
                "auth-code",
                "valid-state"
        )).thenReturn("provider-access-token");

        when(oauthClient.getProfile(
                OAuthProvider.GOOGLE,
                "provider-access-token"
        )).thenReturn(profile);

        when(authRepository
                .findByOauthProviderAndOauthIdAndDeletedAtIsNull(
                        "google",
                        "google-123"
                )).thenReturn(Optional.of(user));

        when(tokenService.issueTokens(
                eq(user),
                anyString()
        )).thenReturn(
                new TokenService.TokenPair(
                        "access-token",
                        "refresh-token",
                        1800L
                )
        );

        var result = oauthService.callback(
                "google",
                "auth-code",
                "valid-state",
                "valid-state",
                "JUnit-Test"
        );

        assertEquals(1L, result.userId());
        assertEquals("refresh-token", result.refreshToken());

        verify(authRepository, never())
                .saveAndFlush(any(User.class));
    }

    @Test
    void callback_newUser_success() throws Exception {
        OAuthProfile profile = new OAuthProfile(
                "kakao",
                "kakao-123",
                "new@example.com",
                "kakao-user",
                null
        );

        when(oauthClient.exchangeCode(
                OAuthProvider.KAKAO,
                "auth-code",
                "valid-state"
        )).thenReturn("provider-access-token");

        when(oauthClient.getProfile(
                OAuthProvider.KAKAO,
                "provider-access-token"
        )).thenReturn(profile);

        when(authRepository
                .findByOauthProviderAndOauthIdAndDeletedAtIsNull(
                        "kakao",
                        "kakao-123"
                )).thenReturn(Optional.empty());

        when(authRepository.existsByEmail("new@example.com"))
                .thenReturn(false);

        when(authRepository.existsByNickname(anyString()))
                .thenReturn(false);

        when(authRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> {
                    User savedUser = invocation.getArgument(0);

                    Field idField =
                            User.class.getDeclaredField("id");

                    idField.setAccessible(true);
                    idField.set(savedUser, 2L);

                    return savedUser;
                });

        when(tokenService.issueTokens(
                any(User.class),
                anyString()
        )).thenReturn(
                new TokenService.TokenPair(
                        "access-token",
                        "refresh-token",
                        1800L
                )
        );

        var result = oauthService.callback(
                "kakao",
                "auth-code",
                "valid-state",
                "valid-state",
                "JUnit-Test"
        );

        assertEquals(2L, result.userId());
        assertEquals("refresh-token", result.refreshToken());

        verify(authRepository)
                .saveAndFlush(any(User.class));
    }

    @Test
    void callback_emailConflict_returns409() {
        OAuthProfile profile = new OAuthProfile(
                "google",
                "google-456",
                "existing@example.com",
                "tester",
                null
        );

        when(oauthClient.exchangeCode(
                OAuthProvider.GOOGLE,
                "auth-code",
                "valid-state"
        )).thenReturn("provider-access-token");

        when(oauthClient.getProfile(
                OAuthProvider.GOOGLE,
                "provider-access-token"
        )).thenReturn(profile);

        when(authRepository
                .findByOauthProviderAndOauthIdAndDeletedAtIsNull(
                        "google",
                        "google-456"
                )).thenReturn(Optional.empty());

        when(authRepository.existsByEmail(
                "existing@example.com"
        )).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> oauthService.callback(
                        "google",
                        "auth-code",
                        "valid-state",
                        "valid-state",
                        "JUnit-Test"
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verifyNoInteractions(tokenService);
    }

    @Test
    void callback_missingCode_returns400() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> oauthService.callback(
                        "google",
                        null,
                        "valid-state",
                        "valid-state",
                        "JUnit-Test"
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        verifyNoInteractions(oauthClient);
    }
}

package com.miroyiro.backend.domain.auth.service;

import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.entity.UserRefreshToken;
import com.miroyiro.backend.domain.auth.repository.RefreshTokenRepository;
import com.miroyiro.backend.global.security.jwt.JwtProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtProvider jwtProvider;

    private TokenService tokenService;

    private User user;

    @BeforeEach
    void setUp() throws Exception {
        tokenService = new TokenService(
                refreshTokenRepository,
                jwtProvider
        );

        user = User.createEmailUser(
                "test@example.com",
                "encoded-password",
                "tester"
        );

        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, 1L);
    }

    @Test
    void issueTokens_success() {
        when(jwtProvider.createAccessToken(1L, "user"))
                .thenReturn("access-token");

        when(jwtProvider.getAccessTokenExpiresIn())
                .thenReturn(1800L);

        var result = tokenService.issueTokens(
                user,
                "JUnit-Test"
        );

        assertEquals("access-token", result.accessToken());
        assertNotNull(result.refreshToken());
        assertFalse(result.refreshToken().isBlank());
        assertEquals(1800L, result.expiresIn());

        verify(refreshTokenRepository)
                .save(any(UserRefreshToken.class));
    }

    @Test
    void issueTokens_storesHashNotRawToken() {
        when(jwtProvider.createAccessToken(1L, "user"))
                .thenReturn("access-token");

        var result = tokenService.issueTokens(
                user,
                "JUnit-Test"
        );

        var captor = org.mockito.ArgumentCaptor.forClass(
                UserRefreshToken.class
        );

        verify(refreshTokenRepository).save(captor.capture());

        UserRefreshToken savedToken = captor.getValue();

        assertEquals(64, savedToken.getTokenHash().length());

        assertNotEquals(
                result.refreshToken(),
                savedToken.getTokenHash()
        );

        assertEquals("JUnit-Test", savedToken.getUserAgent());
        assertFalse(savedToken.isExpired());
    }

    @Test
    void refresh_success_rotatesToken() {
        UserRefreshToken existingToken =
                UserRefreshToken.create(
                        user,
                        "existing-hash",
                        "JUnit-Test",
                        OffsetDateTime.now().plusDays(1)
                );

        when(refreshTokenRepository.findByTokenHashForUpdate(
                anyString()
        )).thenReturn(Optional.of(existingToken));

        when(jwtProvider.createAccessToken(1L, "user"))
                .thenReturn("new-access-token");

        when(jwtProvider.getAccessTokenExpiresIn())
                .thenReturn(1800L);

        var result = tokenService.refresh(
                "old-refresh-token",
                "JUnit-Test"
        );

        assertTrue(existingToken.isRevoked());

        assertEquals(
                "new-access-token",
                result.accessToken()
        );

        assertNotEquals(
                "old-refresh-token",
                result.refreshToken()
        );

        verify(refreshTokenRepository)
                .save(any(UserRefreshToken.class));
    }

    @Test
    void refresh_revokedToken_returns401() {
        UserRefreshToken existingToken =
                UserRefreshToken.create(
                        user,
                        "existing-hash",
                        "JUnit-Test",
                        OffsetDateTime.now().plusDays(1)
                );

        existingToken.revoke();

        when(refreshTokenRepository.findByTokenHashForUpdate(
                anyString()
        )).thenReturn(Optional.of(existingToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tokenService.refresh(
                        "old-refresh-token",
                        "JUnit-Test"
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verify(refreshTokenRepository, never())
                .save(any(UserRefreshToken.class));
    }

    @Test
    void refresh_expiredToken_returns401() {
        UserRefreshToken expiredToken =
                UserRefreshToken.create(
                        user,
                        "expired-hash",
                        "JUnit-Test",
                        OffsetDateTime.now().minusDays(1)
                );

        when(refreshTokenRepository.findByTokenHashForUpdate(
                anyString()
        )).thenReturn(Optional.of(expiredToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tokenService.refresh(
                        "expired-refresh-token",
                        "JUnit-Test"
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    @Test
    void refresh_unknownToken_returns401() {
        when(refreshTokenRepository.findByTokenHashForUpdate(
                anyString()
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tokenService.refresh(
                        "unknown-token",
                        "JUnit-Test"
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    @Test
    void revoke_success() {
        UserRefreshToken existingToken =
                UserRefreshToken.create(
                        user,
                        "existing-hash",
                        "JUnit-Test",
                        OffsetDateTime.now().plusDays(1)
                );

        when(refreshTokenRepository.findByTokenHashForUpdate(
                anyString()
        )).thenReturn(Optional.of(existingToken));

        tokenService.revoke(
                "refresh-token",
                1L
        );

        assertTrue(existingToken.isRevoked());
        assertNotNull(existingToken.getRevokedAt());
    }

    @Test
    void revoke_otherUser_returns401() {
        UserRefreshToken existingToken =
                UserRefreshToken.create(
                        user,
                        "existing-hash",
                        "JUnit-Test",
                        OffsetDateTime.now().plusDays(1)
                );

        when(refreshTokenRepository.findByTokenHashForUpdate(
                anyString()
        )).thenReturn(Optional.of(existingToken));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> tokenService.revoke(
                        "refresh-token",
                        999L
                )
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        assertFalse(existingToken.isRevoked());
    }
}

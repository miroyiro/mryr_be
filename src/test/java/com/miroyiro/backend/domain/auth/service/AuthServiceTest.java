package com.miroyiro.backend.domain.auth.service;

import com.miroyiro.backend.domain.auth.dto.request.LoginRequest;
import com.miroyiro.backend.domain.auth.dto.request.SignupRequest;
import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.repository.AuthRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository authRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.createEmailUser(
                "test@example.com",
                "encoded-password",
                "tester"
        );
    }

    @Test
    void signup_success() {
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "password123",
                "tester"
        );

        when(authRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(authRepository.existsByNickname(anyString()))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(authRepository.saveAndFlush(any(User.class)))
                .thenReturn(user);

        var result = authService.signup(request);

        assertEquals("test@example.com", result.user().email());
        assertEquals("tester", result.user().nickname());
        assertEquals("user", result.user().role());
        assertFalse(result.user().onboarded());

        verify(passwordEncoder).encode("password123");
        verify(authRepository).saveAndFlush(any(User.class));
    }

    @Test
    void signup_duplicateEmail_returns409() {
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "password123",
                "tester"
        );

        when(authRepository.existsByEmail("test@example.com"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.signup(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        verify(authRepository, never()).saveAndFlush(any());
    }

    @Test
    void signup_duplicateNickname_returns409() {
        SignupRequest request = new SignupRequest(
                "test@example.com",
                "password123",
                "tester"
        );

        when(authRepository.existsByEmail(anyString()))
                .thenReturn(false);

        when(authRepository.existsByNickname("tester"))
                .thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.signup(request)
        );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
    }

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        when(authRepository.findByEmailAndDeletedAtIsNull(
                "test@example.com"
        )).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

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

        var result = authService.login(
                request,
                "JUnit-Test"
        );

        assertEquals(
                "access-token",
                result.response().accessToken()
        );

        assertEquals(
                "refresh-token",
                result.refreshToken()
        );

        assertEquals(
                1800L,
                result.response().expiresIn()
        );

        assertNotNull(user.getLastLoginAt());
    }

    @Test
    void login_wrongPassword_returns401() {
        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password"
        );

        when(authRepository.findByEmailAndDeletedAtIsNull(
                "test@example.com"
        )).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request, "JUnit-Test")
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        verifyNoInteractions(tokenService);
    }

    @Test
    void login_unknownEmail_returns401() {
        LoginRequest request = new LoginRequest(
                "unknown@example.com",
                "password123"
        );

        when(authRepository.findByEmailAndDeletedAtIsNull(
                "unknown@example.com"
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.login(request, "JUnit-Test")
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    @Test
    void me_success() {
        when(authRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(user));

        var result = authService.me(1L);

        assertEquals("test@example.com", result.email());
        assertEquals("tester", result.nickname());
        assertFalse(result.onboarded());
    }

    @Test
    void me_unknownUser_returns404() {
        when(authRepository.findByIdAndDeletedAtIsNull(999L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.me(999L)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );
    }
}

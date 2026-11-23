package com.miroyiro.backend.domain.auth.service;

import com.miroyiro.backend.domain.auth.dto.request.LoginRequest;
import com.miroyiro.backend.domain.auth.dto.request.SignupRequest;
import com.miroyiro.backend.domain.auth.dto.response.LoginResponse;
import com.miroyiro.backend.domain.auth.dto.response.MeResponse;
import com.miroyiro.backend.domain.auth.dto.response.SignupResponse;
import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.repository.AuthRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthRepository authRepository,
            TokenService tokenService,
            PasswordEncoder passwordEncoder
    ) {
        this.authRepository = authRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    // 로그인 결과: JSON 응답 + 쿠키에 저장할 Refresh Token
    public record LoginResult(
            LoginResponse response,
            String refreshToken
    ) {}

    // 이메일 회원가입
    @Transactional
    public SignupResponse signup(SignupRequest request) {

        String email = normalizeEmail(request.email());
        String nickname = request.nickname().trim();

        if (nickname.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nickname is required"
            );
        }

        // BCrypt는 최대 72바이트까지만 처리
        int passwordBytes = request.password()
                .getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 bytes"
            );
        }

        // 이메일 중복 검사
        if (authRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        // 닉네임 중복 검사
        if (authRepository.existsByNickname(nickname)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Nickname already exists"
            );
        }

        // 비밀번호 해시
        String passwordHash = passwordEncoder.encode(
                request.password()
        );

        // 이메일 사용자 생성
        User user = User.createEmailUser(
                email,
                passwordHash,
                nickname
        );

        // DB 저장
        try {
            User savedUser = authRepository.saveAndFlush(user);

            return SignupResponse.from(savedUser);

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email or nickname already exists",
                    e
            );
        }
    }

    // 이메일 로그인
    @Transactional
    public LoginResult login(
            LoginRequest request,
            String userAgent
    ) {
        String email = normalizeEmail(request.email());

        // 이메일로 사용자 조회
        User user = authRepository
                .findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(this::invalidCredentials);

        // 소셜 계정이거나 비밀번호가 일치하지 않는 경우
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                )) {
            throw invalidCredentials();
        }

        // 마지막 로그인 시간 갱신
        user.updateLastLoginAt();

        // Access Token + Refresh Token 발급
        TokenService.TokenPair tokens = tokenService.issueTokens(
                user,
                userAgent
        );

        // 로그인 응답 DTO 생성
        LoginResponse response = LoginResponse.of(
                tokens.accessToken(),
                tokens.expiresIn(),
                user
        );

        return new LoginResult(
                response,
                tokens.refreshToken()
        );
    }

    // 로그인 사용자 정보 조회
    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {

        User user = authRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        return MeResponse.from(user);
    }

    // 이메일 정규화
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 로그인 실패 예외
    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
    }
}

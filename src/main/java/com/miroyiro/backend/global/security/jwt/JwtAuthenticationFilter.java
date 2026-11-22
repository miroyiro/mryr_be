package com.miroyiro.backend.global.security.jwt;

import com.miroyiro.backend.domain.auth.entity.User;
import com.miroyiro.backend.domain.auth.repository.AuthRepository;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AuthRepository authRepository;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            AuthRepository authRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.authRepository = authRepository;
    }

    // JWT 검사가 필요 없는 경로
    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getServletPath();

        return path.equals("/internal")
                || path.startsWith("/internal/")
                || path.equals("/api/health")
                || path.startsWith("/actuator/health")
                || path.equals("/api/auth/signup")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/kakao")
                || path.startsWith("/api/auth/kakao/")
                || path.equals("/api/auth/naver")
                || path.startsWith("/api/auth/naver/")
                || path.equals("/api/auth/google")
                || path.startsWith("/api/auth/google/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader("Authorization");

        // Authorization 헤더가 없는 경우
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7);

        try {
            // JWT 서명 및 만료 검증
            Long userId = jwtProvider.getUserId(token);

            // DB에서 탈퇴하지 않은 사용자 조회
            User user = authRepository
                    .findByIdAndDeletedAtIsNull(userId)
                    .orElse(null);

            if (user == null) {
                SecurityContextHolder.clearContext();

                response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "User not found"
                );
                return;
            }

            // DB에 저장된 역할을 Spring Security 권한으로 변환
            String authority =
                    "ROLE_" + user.getRole().toUpperCase();

            var authorities = List.of(
                    new SimpleGrantedAuthority(authority)
            );

            // 인증 객체 생성
            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getId().toString(),
                            null,
                            authorities
                    );

            // SecurityContext에 인증 정보 저장
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException e) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired access token"
            );
            return;
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }
}

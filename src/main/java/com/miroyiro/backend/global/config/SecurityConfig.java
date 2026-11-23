package com.miroyiro.backend.global.config;

import com.miroyiro.backend.global.security.internal.InternalKeyFilter;
import com.miroyiro.backend.global.security.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Configuration
public class SecurityConfig {

    private static final Set<String> ALLOWED_OAUTH_PROVIDERS =
            Set.of("google", "kakao", "naver");

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalKeyFilter internalKeyFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
            // 기존 설정 유지: Stateless JWT 인증
            .csrf(AbstractHttpConfigurer::disable)

            // 기존 CORS 설정 사용
            .cors(cors -> {})

            // 기본 인증 및 로그인 폼 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)

            // 서버 세션을 사용하지 않음
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // 인증되지 않은 요청은 401 반환
            .exceptionHandling(exception ->
                exception.authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                        HttpStatus.UNAUTHORIZED
                    )
                )
            )

            // 기존 내부 API 인증 필터 유지
            .addFilterBefore(
                internalKeyFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            // JWT 인증 필터 추가
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            .authorizeHttpRequests(auth -> auth

                // 1. 내부 API: InternalKeyFilter에서 검증
                .requestMatchers("/internal/**").permitAll()

                // 2. Health Check 공개
                .requestMatchers(
                    "/api/health",
                    "/actuator/health/**"
                ).permitAll()

                // 3. 이메일 회원가입 및 로그인 공개
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/signup",
                    "/api/auth/login"
                ).permitAll()

                // 4. Refresh Token 재발급: 쿠키로 인증
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/refresh"
                ).permitAll()

                // 5. OAuth 로그인 시작 및 콜백 공개
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/{provider}",
                    "/api/auth/{provider}/callback"
                ).permitAll()

                // 6. 로그인 필수 API
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/me"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/logout"
                ).authenticated()

                // 7. 나머지 인증 API는 허용하지 않음
                .requestMatchers("/api/auth/**").denyAll()

                // 8. 관리자 API
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // 9. 기존 팀 정책 유지
                .requestMatchers("/api/**").permitAll()

                // 10. 나머지 경로 차단
                .anyRequest().denyAll()
            );

        return http.build();
    }

    // 비밀번호 해시 및 검증
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // JWT 필터의 서블릿 자동 등록 방지
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);

        registration.setEnabled(false);

        return registration;
    }

    // 기존 CORS 설정 유지
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            MiroyiroProperties props
    ) {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                props.cors().allowedOrigins()
        );

        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");

        // Refresh Token 쿠키 전송 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/api/**",
                configuration
        );

        return source;
    }
}

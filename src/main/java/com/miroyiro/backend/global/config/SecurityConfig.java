package com.miroyiro.backend.global.config;

import com.miroyiro.backend.global.security.internal.InternalKeyFilter;
import com.miroyiro.backend.global.security.jwt.JwtAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

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

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalKeyFilter internalKeyFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
            // Stateless JWT 인증
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정
            .cors(cors -> {})

            // 기본 인증 및 로그인 폼 비활성화
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)

            // 서버 세션 사용하지 않음
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // 인증 및 권한 예외 처리
            .exceptionHandling(exception -> exception

                // 인증되지 않은 사용자: 401
                .authenticationEntryPoint(
                    new HttpStatusEntryPoint(
                        HttpStatus.UNAUTHORIZED
                    )
                )

                // 인증됐지만 권한이 없는 사용자: 403
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(
                        HttpServletResponse.SC_FORBIDDEN
                    );

                    response.setContentType(
                        "application/json"
                    );

                    response.setCharacterEncoding("UTF-8");

                    response.getWriter().write(
                        """
                        {
                            "error": {
                                "code": "FORBIDDEN",
                                "message": "접근 권한이 없습니다."
                            }
                        }
                        """
                    );
                })
            )

            // 내부 API 인증 필터
            .addFilterBefore(
                internalKeyFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            // JWT 인증 필터
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            .authorizeHttpRequests(auth -> auth

                // 오류 처리 과정의 재인증 방지
                .dispatcherTypeMatchers(
                    DispatcherType.ERROR
                ).permitAll()

                // 1. 내부 API
                .requestMatchers("/internal/**").permitAll()

                // 2. Health Check 공개
                .requestMatchers(
                    "/api/health",
                    "/actuator/health/**"
                ).permitAll()

                // 3. 회원가입 및 로그인 공개
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/signup",
                    "/api/auth/login"
                ).permitAll()

                // 4. Refresh Token 재발급
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/refresh"
                ).permitAll()

                // 5. 로그인 필수 API
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/me"
                ).authenticated()

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/logout"
                ).authenticated()

                // 6. OAuth 로그인 및 콜백 공개
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/auth/google",
                    "/api/auth/google/callback",
                    "/api/auth/kakao",
                    "/api/auth/kakao/callback",
                    "/api/auth/naver",
                    "/api/auth/naver/callback"
                ).permitAll()

                // 7. 나머지 인증 API 차단
                .requestMatchers("/api/auth/**").denyAll()

                // 8. 관리자 API
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")

                // 9. 아티스트 상세 조회 공개
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/artists/{id}"
                ).permitAll()

                // 10. 아티스트 팔로우
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/artists/{id}/follow"
                ).authenticated()

                // 11. 아티스트 언팔로우
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/artists/{id}/follow"
                ).authenticated()

                // 12. 기존 팀 정책 유지
                .requestMatchers("/api/**").permitAll()

                // 13. 나머지 경로 차단
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

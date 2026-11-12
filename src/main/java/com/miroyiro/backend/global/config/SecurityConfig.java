package com.miroyiro.backend.global.config;

import com.miroyiro.backend.global.security.internal.InternalKeyFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 경로별 보안 경계 (03 문서 '보안 경계').
 *  /internal/**   : InternalKeyFilter (X-Internal-Key) — 세션·JWT 안 씀
 *  /api/admin/**  : JWT + ROLE_ADMIN  (JwtAuthenticationFilter 는 auth 담당이 추가)
 *  /api/**        : 지금은 permitAll. 로그인 필요한 API 가 생기면 그 경로만 authenticated() 로
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, InternalKeyFilter internalKeyFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)            // 쿠키 세션 안 씀 (JWT + 내부키)
            .cors(cors -> {})
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/internal/**").permitAll()   // 키 검사는 InternalKeyFilter 가 먼저 함
                .requestMatchers("/api/health", "/actuator/health/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").permitAll()
                .anyRequest().denyAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(MiroyiroProperties props) {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(props.cors().allowedOrigins());
        c.addAllowedMethod("*");
        c.addAllowedHeader("*");
        c.setAllowCredentials(true);                      // refresh 토큰 쿠키
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", c);   // /internal 은 브라우저가 부르지 않음 → CORS 없음
        return source;
    }
}

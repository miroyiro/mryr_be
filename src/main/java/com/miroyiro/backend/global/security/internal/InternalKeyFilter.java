package com.miroyiro.backend.global.security.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.miroyiro.backend.global.config.MiroyiroProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * /internal/** 전용: X-Internal-Key 가 설정값과 같아야 통과 (scrap → backend).
 * 키가 비어 있으면(설정 누락) 전부 거부 — '키 없음 = 열림' 이 되지 않게.
 * 네트워크 단에서도 Tailscale 대역만 허용 (infra SG) → 이중 방어.
 */
@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";

    private final byte[] expected;

    public InternalKeyFilter(MiroyiroProperties props) {
        String key = props.internal() == null ? null : props.internal().key();
        this.expected = key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String given = request.getHeader(HEADER);
        if (expected.length == 0 || given == null
                || !MessageDigest.isEqual(expected, given.getBytes(StandardCharsets.UTF_8))) {   // 타이밍 공격 방지
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":{\"code\":\"INVALID_INTERNAL_KEY\",\"message\":\"invalid internal key\"}}");
            return;
        }
        chain.doFilter(request, response);
    }
}

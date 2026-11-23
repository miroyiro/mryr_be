package com.miroyiro.backend.domain.auth.dto.response;

public record AuthResponse<T>(
        T data
) {}

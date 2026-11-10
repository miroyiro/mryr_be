package com.miroyiro.backend.global.error;

import org.springframework.http.HttpStatus;

/** 06 '상태 코드' 표 기준. 도메인별 코드는 여기 추가. */
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다"),
    CONFLICT(HttpStatus.CONFLICT, "이미 존재합니다"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "외부 서버 응답 이상"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "외부 서비스 제한 또는 점검 중"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}

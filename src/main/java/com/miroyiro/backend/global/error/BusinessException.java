package com.miroyiro.backend.global.error;

public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    public BusinessException(ErrorCode code) {
        this(code, code.defaultMessage());
    }

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }

    public static BusinessException notFound(String what) {
        return new BusinessException(ErrorCode.NOT_FOUND, what + " not found");
    }
}

package com.miroyiro.backend.global.error;

/** { "error": { "code": "NOT_FOUND", "message": "..." } } */
public record ErrorResponse(Body error) {

    public record Body(String code, String message) {}

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(new Body(code.name(), message));
    }
}

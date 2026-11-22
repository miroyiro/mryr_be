package com.miroyiro.backend.global.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e) {
        return respond(e.code(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse(ErrorCode.INVALID_REQUEST.defaultMessage());
        return respond(ErrorCode.INVALID_REQUEST, msg);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception e) {
        return respond(ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.defaultMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> noRoute(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, "no route");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> conflict(DataIntegrityViolationException e) {
        log.warn("constraint violation: {}", e.getMostSpecificCause().getMessage());
        return respond(ErrorCode.CONFLICT, "constraint violation");
    }

   @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException e) {
        log.warn("HTTP error: {} - {}", e.getStatusCode(), e.getReason());

        ErrorCode code;

        switch (e.getStatusCode().value()) {
            case 400 -> code = ErrorCode.INVALID_REQUEST;
            case 401 -> code = ErrorCode.UNAUTHORIZED;
            case 403 -> code = ErrorCode.FORBIDDEN;
            case 404 -> code = ErrorCode.NOT_FOUND;
            case 409 -> code = ErrorCode.CONFLICT;
            case 502 -> code = ErrorCode.BAD_GATEWAY;
            case 503 -> code = ErrorCode.SERVICE_UNAVAILABLE;
            default -> {
                return ResponseEntity
                        .status(e.getStatusCode())
                        .body(ErrorResponse.of(
                                ErrorCode.INTERNAL_ERROR,
                                "요청 처리 중 오류가 발생했습니다"
                        ));
            }
        }

        return respond(
                code,
                e.getReason() != null
                        ? e.getReason()
                        : code.defaultMessage()
        );
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unknown(Exception e) {
        log.error("unhandled", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, message));
    }
}

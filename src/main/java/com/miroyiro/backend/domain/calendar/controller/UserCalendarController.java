package com.miroyiro.backend.domain.calendar.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.miroyiro.backend.domain.calendar.dto.request.CalendarCreateRequest;
import com.miroyiro.backend.domain.calendar.dto.request.CalendarUpdateRequest;
import com.miroyiro.backend.domain.calendar.dto.response.CalendarCreateResponse;
import com.miroyiro.backend.domain.calendar.dto.response.CalendarUpdateResponse;
import com.miroyiro.backend.domain.calendar.dto.response.CalendarDeleteResponse;
import com.miroyiro.backend.domain.calendar.service.UserCalendarService;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/calendar")
public class UserCalendarController {

    private final UserCalendarService calendarService;

    public UserCalendarController(
            UserCalendarService calendarService
    ) {
        this.calendarService = calendarService;
    }

    /**
     * 내 캘린더 일정 등록.
     *
     * POST /api/calendar
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            Authentication authentication,
            @Valid @RequestBody CalendarCreateRequest request
    ) {
        Long userId = getUserId(authentication);

        CalendarCreateResponse response =
                calendarService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("data", response));
    }

    /**
     * 내 캘린더 일정 수정.
     *
     * PATCH /api/calendar/{calendarId}
     */
    @PatchMapping("/{calendarId}")
    public ResponseEntity<Map<String, Object>> update(
            Authentication authentication,
            @PathVariable("calendarId") Long calendarId,
            @Valid @RequestBody CalendarUpdateRequest request
    ) {
        Long userId = getUserId(authentication);

        CalendarUpdateResponse response =
                calendarService.update(
                        userId,
                        calendarId,
                        request
                );

        return ResponseEntity.ok(
                Map.of("data", response)
        );
    }

    /**
     * 내 캘린더 일정 삭제.
     *
     * DELETE /api/calendar/{calendarId}
     */
    @DeleteMapping("/{calendarId}")
    public ResponseEntity<Map<String, Object>> delete(
            Authentication authentication,
            @PathVariable("calendarId") Long calendarId
    ) {
        Long userId = getUserId(authentication);

        CalendarDeleteResponse response =
                calendarService.delete(
                        userId,
                        calendarId
                );

        return ResponseEntity.ok(
                Map.of("data", response)
        );
    }

    /**
     * JWT 인증 정보에서 사용자 ID 추출.
     */
    private Long getUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }

        try {
            return Long.valueOf(
                    authentication.getName()
            );
        } catch (NumberFormatException e) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }
    }
}

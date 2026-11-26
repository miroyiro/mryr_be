package com.miroyiro.backend.domain.calendar.controller;

import com.miroyiro.backend.domain.calendar.dto.response.JpopCalendarMonthlyResponse;
import com.miroyiro.backend.domain.calendar.dto.response.JpopCalendarDailyResponse;
import com.miroyiro.backend.domain.calendar.dto.response.JpopUpcomingResponse;
import com.miroyiro.backend.domain.calendar.service.JpopCalendarService;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar/jpop")
public class JpopCalendarController {

    private final JpopCalendarService calendarService;

    public JpopCalendarController(
            JpopCalendarService calendarService
    ) {
        this.calendarService = calendarService;
    }

    // 1. JPOP 월별 캘린더 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMonthly(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        List<JpopCalendarMonthlyResponse> data =
                calendarService.getMonthly(
                        userId,
                        year,
                        month
                );

        return ResponseEntity.ok(
                Map.of("data", data)
        );
    }

    // 2. JPOP 일자별 상세 조회
    @GetMapping("/{date}")
    public ResponseEntity<Map<String, Object>> getDaily(
            @PathVariable("date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        List<JpopCalendarDailyResponse> data =
                calendarService.getDaily(
                        userId,
                        date
                );

        return ResponseEntity.ok(
                Map.of(
                        "date", date.toString(),
                        "data", data
                )
        );
    }

    // 3. 팔로우한 아티스트의 다가오는 일정 조회
    @GetMapping("/upcoming")
    public ResponseEntity<Map<String, Object>> getUpcoming(
            @RequestParam(
                    name = "limit",
                    defaultValue = "10"
            ) int limit,

            @RequestParam(
                    name = "artistId",
                    required = false
            ) Long artistId,

            @RequestParam(
                    name = "type",
                    required = false
            ) String type,

            @RequestParam(
                    name = "country",
                    required = false
            ) String country,

            @RequestParam(
                    name = "city",
                    required = false
            ) String city,

            Authentication authentication
    ) {
        Long userId = getUserId(authentication);

        List<JpopUpcomingResponse> data =
                calendarService.getUpcoming(
                        userId,
                        limit,
                        artistId,
                        type,
                        country,
                        city
                );

        return ResponseEntity.ok(
                Map.of(
                        "data", data,
                        "meta", Map.of(
                                "count", data.size()
                        )
                )
        );
    }

    // JWT 인증 정보에서 사용자 ID 추출
    private Long getUserId(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication
                    instanceof AnonymousAuthenticationToken) {

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

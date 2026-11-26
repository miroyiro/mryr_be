package com.miroyiro.backend.domain.calendar.service;

import com.miroyiro.backend.domain.calendar.dto.response.JpopCalendarMonthlyResponse;
import com.miroyiro.backend.domain.calendar.dto.response.JpopCalendarDailyResponse;
import com.miroyiro.backend.domain.calendar.dto.response.JpopUpcomingResponse;
import com.miroyiro.backend.domain.calendar.repository.JpopCalendarRepository;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class JpopCalendarService {

    private static final ZoneId KST =
            ZoneId.of("Asia/Seoul");

    private static final int MAX_LIMIT = 100;

    private static final Set<String> ALLOWED_TYPES =
            Set.of(
                    "concert",
                    "release",
                    "exhibition",
                    "popup",
                    "etc"
            );

    private final JpopCalendarRepository calendarRepository;

    public JpopCalendarService(
            JpopCalendarRepository calendarRepository
    ) {
        this.calendarRepository = calendarRepository;
    }

    // 1. JPOP 월별 캘린더 조회
    public List<JpopCalendarMonthlyResponse> getMonthly(
            Long userId,
            int year,
            int month
    ) {
        validateUser(userId);

        if (year < 1 || year > 9999) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "year must be between 1 and 9999"
            );
        }

        if (month < 1 || month > 12) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "month must be between 1 and 12"
            );
        }

        YearMonth yearMonth =
                YearMonth.of(year, month);

        OffsetDateTime startAt =
                yearMonth.atDay(1)
                        .atStartOfDay(KST)
                        .toOffsetDateTime();

        OffsetDateTime endAt =
                yearMonth.plusMonths(1)
                        .atDay(1)
                        .atStartOfDay(KST)
                        .toOffsetDateTime();

        return calendarRepository.findMonthly(
                        userId,
                        startAt,
                        endAt
                )
                .stream()
                .map(item ->
                        new JpopCalendarMonthlyResponse(
                                item.getCalendarId(),
                                item.getEventId(),
                                item.getTitle(),
                                item.getType(),
                                toKst(item.getStartAt()),
                                toKst(item.getEndAt()),
                                Boolean.TRUE.equals(
                                        item.getAllDay()
                                ),
                                item.getStatus()
                        )
                )
                .toList();
    }

    // 2. JPOP 일자별 캘린더 조회
    public List<JpopCalendarDailyResponse> getDaily(
            Long userId,
            LocalDate date
    ) {
        validateUser(userId);

        if (date == null
                || date.getYear() < 1
                || date.getYear() > 9999) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid date"
            );
        }

        OffsetDateTime startAt =
                date.atStartOfDay(KST)
                        .toOffsetDateTime();

        OffsetDateTime endAt =
                date.plusDays(1)
                        .atStartOfDay(KST)
                        .toOffsetDateTime();

        return calendarRepository.findDaily(
                        userId,
                        startAt,
                        endAt
                )
                .stream()
                .map(item ->
                        new JpopCalendarDailyResponse(
                                item.getCalendarId(),
                                item.getEventId(),
                                item.getTitle(),
                                item.getType(),
                                toKst(item.getStartAt()),
                                toKst(item.getEndAt()),
                                item.getVenue(),
                                item.getMemo(),
                                item.getStatus()
                        )
                )
                .toList();
    }

    // 3. 팔로우한 아티스트의 다가오는 일정 조회
    public List<JpopUpcomingResponse> getUpcoming(
            Long userId,
            int limit,
            Long artistId,
            String type,
            String country,
            String city
    ) {
        validateUser(userId);

        // 조회 개수 검증
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "limit must be between 1 and 100"
            );
        }

        // 아티스트 ID 검증
        if (artistId != null && artistId <= 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "artistId must be positive"
            );
        }

        // 선택적 검색 조건 정리
        type = normalize(type);
        country = normalize(country);
        city = normalize(city);

        // 이벤트 유형 검증
        if (type != null
                && !ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Invalid event type"
            );
        }

        // 국가 코드 검증
        if (country != null) {

            country = country.toUpperCase(Locale.ROOT);

            if (!country.matches("[A-Z]{2}")) {
                throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid country code"
                );
            }
        }

        // 도시명 길이 검증
        if (city != null && city.length() > 50) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "city must not exceed 50 characters"
            );
        }

        // 현재 시각부터 앞으로 14일
        OffsetDateTime startAt =
                OffsetDateTime.now(KST);

        OffsetDateTime endAt =
                startAt.plusDays(14);

        return calendarRepository.findUpcoming(
                        userId,
                        startAt,
                        endAt,
                        artistId,
                        type,
                        country,
                        city,
                        limit
                )
                .stream()
                .map(item ->
                        new JpopUpcomingResponse(
                                item.getId(),

                                new JpopUpcomingResponse.ArtistInfo(
                                        item.getArtistId(),
                                        item.getArtistName()
                                ),

                                item.getTitle(),
                                item.getType(),
                                item.getVenue(),
                                item.getCity(),
                                item.getCountry(),
                                toKst(item.getStartAt()),
                                toKst(item.getEndAt()),
                                item.getStatus(),

                                Boolean.TRUE.equals(
                                        item.getInternational()
                                ),

                                item.getTicketUrl(),
                                item.getCalendarId()
                        )
                )
                .toList();
    }

    // 로그인 사용자 검증
    private void validateUser(Long userId) {

        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }
    }

    // 선택적 검색 조건 정리
    private String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // 응답 시간대를 한국 시간으로 통일
    private OffsetDateTime toKst(Instant value) {
    return value == null
            ? null
            : value.atZone(KST).toOffsetDateTime();
}
}

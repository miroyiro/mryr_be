package com.miroyiro.backend.domain.calendar.repository;

import com.miroyiro.backend.domain.calendar.entity.UserCalendar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.time.Instant;

public interface JpopCalendarRepository
        extends JpaRepository<UserCalendar, Long> {

    // 월별 캘린더 조회 결과
    interface MonthlyProjection {

        Long getCalendarId();

        Long getEventId();

        String getTitle();

        String getType();

        Instant getStartAt();

        Instant getEndAt();

        Boolean getAllDay();

        String getStatus();
    }

    // 일자별 캘린더 조회 결과
    interface DailyProjection {

        Long getCalendarId();

        Long getEventId();

        String getTitle();

        String getType();

        Instant getStartAt();

        Instant getEndAt();

        String getVenue();

        String getMemo();

        String getStatus();
    }

    // 다가오는 일정 조회 결과
    interface UpcomingProjection {

        Long getId();

        Long getArtistId();

        String getArtistName();

        String getTitle();

        String getType();

        String getVenue();

        String getCity();

        String getCountry();

        Instant getStartAt();

        Instant getEndAt();

        String getStatus();

        Boolean getInternational();

        String getTicketUrl();

        Long getCalendarId();
    }

    // 1. 내 JPOP 캘린더 월별 조회
    @Query(value = """
            SELECT
                uc.id AS "calendarId",
                e.id AS "eventId",
                e.title AS title,
                e.type AS type,
                e.start_at AS "startAt",
                e.end_at AS "endAt",
                e.is_all_day AS "allDay",
                uc.status AS status
            FROM miroyiro_main.user_calendar uc
            JOIN miroyiro_main.event e
                ON e.id = uc.event_id
            WHERE uc.user_id = :userId
              AND e.category = 'jpop'
              AND e.start_at >= :startAt
              AND e.start_at < :endAt
            ORDER BY e.start_at ASC, e.id ASC
            """, nativeQuery = true)
    List<MonthlyProjection> findMonthly(
            @Param("userId") Long userId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    // 2. 내 JPOP 캘린더 일자별 상세 조회
    @Query(value = """
            SELECT
                uc.id AS "calendarId",
                e.id AS "eventId",
                e.title AS title,
                e.type AS type,
                e.start_at AS "startAt",
                e.end_at AS "endAt",
                e.venue AS venue,
                uc.memo AS memo,
                uc.status AS status
            FROM miroyiro_main.user_calendar uc
            JOIN miroyiro_main.event e
                ON e.id = uc.event_id
            WHERE uc.user_id = :userId
              AND e.category = 'jpop'
              AND e.start_at >= :startAt
              AND e.start_at < :endAt
            ORDER BY e.start_at ASC, e.id ASC
            """, nativeQuery = true)
    List<DailyProjection> findDaily(
            @Param("userId") Long userId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    // 3. 팔로우한 아티스트의 다가오는 일정 조회
    @Query(value = """
            SELECT
                e.id AS id,
                a.id AS "artistId",
                a.name AS "artistName",
                e.title AS title,
                e.type AS type,
                e.venue AS venue,
                e.city AS city,
                e.country AS country,
                e.start_at AS "startAt",
                e.end_at AS "endAt",
                e.status AS status,
                e.is_international AS "international",
                e.ticket_url AS "ticketUrl",
                uc.id AS "calendarId"
            FROM miroyiro_main.event e
            JOIN miroyiro_main.artist a
                ON a.id = e.artist_id
            JOIN miroyiro_main.user_artist_follow uaf
                ON uaf.artist_id = a.id
               AND uaf.user_id = :userId
               AND uaf.match_status = 'matched'
            LEFT JOIN miroyiro_main.user_calendar uc
                ON uc.event_id = e.id
               AND uc.user_id = :userId
            WHERE e.category = 'jpop'
              AND e.start_at >= :startAt
              AND e.start_at < :endAt
              AND e.status = 'upcoming'
              AND (
                  CAST(:artistId AS BIGINT) IS NULL
                  OR e.artist_id = :artistId
              )
              AND (
                  CAST(:type AS VARCHAR) IS NULL
                  OR e.type = :type
              )
              AND (
                  CAST(:country AS VARCHAR) IS NULL
                  OR e.country = :country
              )
              AND (
                  CAST(:city AS VARCHAR) IS NULL
                  OR e.city = :city
              )
            ORDER BY e.start_at ASC, e.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<UpcomingProjection> findUpcoming(
            @Param("userId") Long userId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("artistId") Long artistId,
            @Param("type") String type,
            @Param("country") String country,
            @Param("city") String city,
            @Param("limit") int limit
    );
}

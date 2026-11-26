package com.miroyiro.backend.domain.calendar.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.miroyiro.backend.domain.calendar.entity.UserCalendar;

public interface CalendarActivityRepository
        extends Repository<UserCalendar, Long> {

    /**
     * 일정 등록 활동 기록 저장.
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO miroyiro_main.user_activity_log (
                        user_id,
                        type,
                        ref_id
                    )
                    VALUES (
                        :userId,
                        'calendar_register',
                        :eventId
                    )
                    """,
            nativeQuery = true
    )
    int insertCalendarRegister(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId
    );
}

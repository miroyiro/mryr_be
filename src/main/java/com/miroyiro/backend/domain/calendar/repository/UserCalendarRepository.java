package com.miroyiro.backend.domain.calendar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miroyiro.backend.domain.calendar.entity.UserCalendar;

public interface UserCalendarRepository
        extends JpaRepository<UserCalendar, Long> {

    /**
     * 사용자의 이벤트 등록 여부 확인.
     */
    boolean existsByUserIdAndEventId(
            Long userId,
            Long eventId
    );

    /**
     * 사용자가 등록한 특정 이벤트 일정 조회.
     */
    Optional<UserCalendar> findByUserIdAndEventId(
            Long userId,
            Long eventId
    );
}

package com.miroyiro.backend.domain.calendar.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miroyiro.backend.domain.calendar.dto.request.CalendarCreateRequest;
import com.miroyiro.backend.domain.calendar.dto.request.CalendarUpdateRequest;
import com.miroyiro.backend.domain.calendar.dto.response.CalendarCreateResponse;
import com.miroyiro.backend.domain.calendar.dto.response.CalendarUpdateResponse;
import com.miroyiro.backend.domain.calendar.dto.response.CalendarDeleteResponse;
import com.miroyiro.backend.domain.calendar.entity.UserCalendar;
import com.miroyiro.backend.domain.calendar.repository.UserCalendarRepository;
import com.miroyiro.backend.domain.calendar.repository.CalendarActivityRepository;
import com.miroyiro.backend.domain.event.repository.EventRepository;
import com.miroyiro.backend.global.error.BusinessException;
import com.miroyiro.backend.global.error.ErrorCode;

import jakarta.persistence.EntityManager;

@Service
@Transactional(readOnly = true)
public class UserCalendarService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserCalendarRepository calendarRepository;
    private final CalendarActivityRepository activityRepository;
    private final EventRepository eventRepository;
    private final EntityManager entityManager;

    public UserCalendarService(
            UserCalendarRepository calendarRepository,
            CalendarActivityRepository activityRepository,
            EventRepository eventRepository,
            EntityManager entityManager
    ) {
        this.calendarRepository = calendarRepository;
        this.activityRepository = activityRepository;
        this.eventRepository = eventRepository;
        this.entityManager = entityManager;
    }

    /**
     * 내 캘린더에 일정 등록.
     */
    @Transactional
    public CalendarCreateResponse create(
            Long userId,
            CalendarCreateRequest request
    ) {
        validateUser(userId);

        Long eventId = request.eventId();

        // 이벤트 존재 여부 확인
        if (!eventRepository.existsById(eventId)) {
            throw new BusinessException(
                    ErrorCode.NOT_FOUND,
                    "Event not found"
            );
        }

        // 중복 일정 등록 방지
        if (calendarRepository.existsByUserIdAndEventId(
                userId,
                eventId
        )) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "Event already registered in calendar"
            );
        }

        // 내 캘린더 일정 생성
        UserCalendar calendar = new UserCalendar(
                userId,
                eventId,
                request.alert(),
                request.memo()
        );

        // DB 저장 및 UNIQUE 제약조건 확인
        UserCalendar saved =
                calendarRepository.saveAndFlush(calendar);

        // DB에서 생성한 시간 조회
        entityManager.refresh(saved);

        // 일정 등록 활동 기록 저장
        activityRepository.insertCalendarRegister(
                userId,
                eventId
        );

        return new CalendarCreateResponse(
                saved.getId(),
                saved.getEventId(),
                saved.getStatus(),
                toKst(saved.getCreatedAt())
        );
    }

    /**
     * 내 캘린더 일정 수정.
     */
    @Transactional
    public CalendarUpdateResponse update(
            Long userId,
            Long calendarId,
            CalendarUpdateRequest request
    ) {
        validateUser(userId);

        UserCalendar calendar =
                findOwnedCalendar(userId, calendarId);

        // 수정할 필드가 없는 경우
        if (request.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "At least one field must be provided"
            );
        }

        // 상태 변경
        if (request.hasField("status")) {
            calendar.updateStatus(request.status());
        }

        // 메모 변경
        // 명시적인 null 요청도 반영
        if (request.hasField("memo")) {
            calendar.updateMemo(request.memo());
        }

        // 알림 설정 변경
        if (request.hasField("is_alert")) {
            calendar.updateAlert(request.alert());
        }

        // 변경사항 DB 반영
        calendarRepository.flush();

        // DB에서 갱신한 updated_at 조회
        entityManager.refresh(calendar);

        return new CalendarUpdateResponse(
                calendar.getId(),
                calendar.getStatus(),
                calendar.getMemo(),
                calendar.isAlert(),
                toKst(calendar.getUpdatedAt())
        );
    }

    /**
     * 내 캘린더 일정 삭제.
     */
    @Transactional
    public CalendarDeleteResponse delete(
            Long userId,
            Long calendarId
    ) {
        validateUser(userId);

        UserCalendar calendar =
                findOwnedCalendar(userId, calendarId);

        // notification_log는 DB CASCADE로 자동 삭제
        calendarRepository.delete(calendar);
        calendarRepository.flush();

        return new CalendarDeleteResponse(true);
    }

    /**
     * 일정 존재 여부 및 소유권 확인.
     */
    private UserCalendar findOwnedCalendar(
            Long userId,
            Long calendarId
    ) {
        UserCalendar calendar =
                calendarRepository.findById(calendarId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.NOT_FOUND,
                                        "Calendar not found"
                                )
                        );

        if (!userId.equals(calendar.getUserId())) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "You do not own this calendar"
            );
        }

        return calendar;
    }

    /**
     * 로그인 사용자 확인.
     */
    private void validateUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED
            );
        }
    }

    /**
     * DB 시간을 한국 표준시로 변환.
     */
    private OffsetDateTime toKst(OffsetDateTime value) {
        if (value == null) {
            return null;
        }

        return value
                .atZoneSameInstant(KST)
                .toOffsetDateTime();
    }
}

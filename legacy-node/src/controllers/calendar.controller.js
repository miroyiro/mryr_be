import {
  createCalendar,
  updateCalendar,
  deleteCalendar,
  getMonthlyAnimeCalendar,
  getDailyAnimeCalendar,
  getUpcomingAnimeCalendar,
  getMonthlyJpopCalendar,
  getDailyJpopCalendar,
  getUpcomingJpopCalendar,
} from "../services/calendar.service.js";

export async function createCalendarController(req, res, next) {
  try {
    const { event_id, is_alert, memo } = req.body;

    // event_id 검증
    if (!Number.isSafeInteger(event_id) || event_id <= 0) {
      const error = new Error("event_id는 양의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    // is_alert 검증
    if (typeof is_alert !== "boolean") {
      const error = new Error("is_alert는 true 또는 false여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    // memo 검증: 문자열 또는 null
    if (memo !== null && (typeof memo !== "string" || memo.length > 1000)) {
      const error = new Error(
        "memo는 1000자 이하의 문자열 또는 null이어야 합니다.",
      );
      error.statusCode = 400;
      throw error;
    }

    const data = await createCalendar({
      userId: req.user.id,
      eventId: event_id,
      isAlert: is_alert,
      memo,
    });

    return res.status(201).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function updateCalendarController(req, res, next) {
  try {
    const calendarId = Number(req.params.calendarId);

    if (!Number.isSafeInteger(calendarId) || calendarId <= 0) {
      const error = new Error("calendarId는 양의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const { status, memo, is_alert } = req.body;

    // 실제로 전달된 수정 필드만 검사합니다.
    const hasStatus = Object.hasOwn(req.body, "status");
    const hasMemo = Object.hasOwn(req.body, "memo");
    const hasAlert = Object.hasOwn(req.body, "is_alert");

    if (!hasStatus && !hasMemo && !hasAlert) {
      const error = new Error("수정할 항목을 하나 이상 보내야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (hasStatus && !["planned", "done", "canceled"].includes(status)) {
      const error = new Error(
        "status는 planned, done, canceled 중 하나여야 합니다.",
      );
      error.statusCode = 400;
      throw error;
    }

    if (
      hasMemo &&
      memo !== null &&
      (typeof memo !== "string" || memo.length > 1000)
    ) {
      const error = new Error(
        "memo는 1000자 이하의 문자열 또는 null이어야 합니다.",
      );
      error.statusCode = 400;
      throw error;
    }

    if (hasAlert && typeof is_alert !== "boolean") {
      const error = new Error("is_alert는 true 또는 false여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await updateCalendar({
      userId: req.user.id,
      calendarId,
      updates: {
        ...(hasStatus && { status }),
        ...(hasMemo && { memo }),
        ...(hasAlert && { is_alert }),
      },
    });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function deleteCalendarController(req, res, next) {
  try {
    const calendarId = Number(req.params.calendarId);

    if (!Number.isSafeInteger(calendarId) || calendarId <= 0) {
      const error = new Error("calendarId는 양의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await deleteCalendar({
      userId: req.user.id,
      calendarId,
    });

    return res.status(200).json({
      message: "캘린더 등록이 삭제되었습니다.",
      data,
    });
  } catch (error) {
    next(error);
  }
}

export async function getMonthlyAnimeCalendarController(req, res, next) {
  try {
    const year = Number(req.query.year);
    const month = Number(req.query.month);

    if (!Number.isInteger(year) || year < 2000 || year > 2100) {
      const error = new Error("year는 2000~2100 사이의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (!Number.isInteger(month) || month < 1 || month > 12) {
      const error = new Error("month는 1~12 사이의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await getMonthlyAnimeCalendar({ year, month });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getDailyAnimeCalendarController(req, res, next) {
  try {
    const { date } = req.params;

    // YYYY-MM-DD 형식 확인
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      const error = new Error("date는 YYYY-MM-DD 형식이어야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    // 실제 존재하는 날짜인지 확인
    const parsedDate = new Date(`${date}T00:00:00.000Z`);

    if (
      Number.isNaN(parsedDate.getTime()) ||
      parsedDate.toISOString().slice(0, 10) !== date
    ) {
      const error = new Error("유효하지 않은 날짜입니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await getDailyAnimeCalendar({ date });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getUpcomingAnimeCalendarController(req, res, next) {
  try {
    const limit = req.query.limit === undefined ? 10 : Number(req.query.limit);

    const animeId =
      req.query.animeId === undefined ? undefined : Number(req.query.animeId);

    const { type, city } = req.query;

    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      const error = new Error("limit은 1~50 사이의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (
      animeId !== undefined &&
      (!Number.isSafeInteger(animeId) || animeId <= 0)
    ) {
      const error = new Error("animeId는 양의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (
      type !== undefined &&
      (typeof type !== "string" || type.trim().length === 0)
    ) {
      const error = new Error("type은 비어 있지 않은 문자열이어야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (
      city !== undefined &&
      (typeof city !== "string" ||
        city.trim().length === 0 ||
        city.length > 100)
    ) {
      const error = new Error("city는 1~100자 문자열이어야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await getUpcomingAnimeCalendar({
      userId: req.user.id,
      limit,
      animeId,
      type: type?.trim(),
      city: city?.trim(),
    });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

//J-POP

export async function getMonthlyJpopCalendarController(req, res, next) {
  try {
    const year = Number(req.query.year);
    const month = Number(req.query.month);

    if (!Number.isInteger(year) || year < 2000 || year > 2100) {
      const error = new Error("year는 2000~2100 사이의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (!Number.isInteger(month) || month < 1 || month > 12) {
      const error = new Error("month는 1~12 사이의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await getMonthlyJpopCalendar({ year, month });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getDailyJpopCalendarController(req, res, next) {
  try {
    const { date } = req.params;

    // YYYY-MM-DD 형식 확인
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
      const error = new Error("date는 YYYY-MM-DD 형식이어야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    // 실제 존재하는 날짜인지 확인
    const parsedDate = new Date(`${date}T00:00:00.000Z`);

    if (
      Number.isNaN(parsedDate.getTime()) ||
      parsedDate.toISOString().slice(0, 10) !== date
    ) {
      const error = new Error("유효하지 않은 날짜입니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await getDailyJpopCalendar({ date });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getUpcomingJpopCalendarController(req, res, next) {
  try {
    const limit = req.query.limit === undefined ? 10 : Number(req.query.limit);

    const { artistId, type, country, city } = req.query;

    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      return res.status(400).json({
        message: "limit은 1~50 사이의 정수여야 합니다.",
      });
    }

    let parsedArtistId;

    if (artistId !== undefined) {
      parsedArtistId = Number(artistId);

      if (!Number.isInteger(parsedArtistId) || parsedArtistId < 1) {
        return res.status(400).json({
          message: "artistId는 양의 정수여야 합니다.",
        });
      }
    }

    if (type !== undefined && (typeof type !== "string" || !type.trim())) {
      return res.status(400).json({
        message: "type은 비어 있지 않은 문자열이어야 합니다.",
      });
    }

    if (
      country !== undefined &&
      (typeof country !== "string" || !/^[A-Za-z]{2,3}$/.test(country))
    ) {
      return res.status(400).json({
        message: "country는 2~3자리 국가 코드여야 합니다.",
      });
    }

    if (
      city !== undefined &&
      (typeof city !== "string" || !city.trim() || city.trim().length > 100)
    ) {
      return res.status(400).json({
        message: "city는 1~100자 문자열이어야 합니다.",
      });
    }

    const data = await getUpcomingJpopCalendar({
      userId: req.user.id,
      limit,
      artistId: parsedArtistId,
      type: type?.trim(),
      country: country?.toUpperCase(),
      city: city?.trim(),
    });

    return res.status(200).json({
      data,
      meta: { limit },
    });
  } catch (error) {
    next(error);
  }
}

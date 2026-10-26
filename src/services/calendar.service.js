import {
  createCalendarEntry,
  updateCalendarEntry,
  deleteCalendarEntry,
  findMonthlyAnimeCalendar,
  findDailyAnimeCalendar,
  findUpcomingAnimeCalendar,
  findMonthlyJpopCalendar,
  findDailyJpopCalendar,
  findUpcomingJpopCalendar,
} from "../repositories/userCalendar.repository.js";

export async function createCalendar({ userId, eventId, isAlert, memo }) {
  try {
    return await createCalendarEntry({
      userId,
      eventId,
      isAlert,
      memo,
    });
  } catch (error) {
    // 중복 등록
    if (error.code === "23505") {
      const conflictError = new Error("이미 내 캘린더에 등록된 이벤트입니다.");
      conflictError.statusCode = 409;
      throw conflictError;
    }

    throw error;
  }
}

export async function updateCalendar({ userId, calendarId, updates }) {
  return updateCalendarEntry({
    userId,
    calendarId,
    updates,
  });
}

export async function deleteCalendar({ userId, calendarId }) {
  return deleteCalendarEntry({ userId, calendarId });
}

export async function getMonthlyAnimeCalendar({ year, month }) {
  return findMonthlyAnimeCalendar({ year, month });
}

export async function getDailyAnimeCalendar({ date }) {
  return findDailyAnimeCalendar({ date });
}

export async function getUpcomingAnimeCalendar({
  userId,
  limit,
  animeId,
  type,
  city,
}) {
  return findUpcomingAnimeCalendar({
    userId,
    limit,
    animeId,
    type,
    city,
  });
}

//J-POP

export async function getMonthlyJpopCalendar({ year, month }) {
  return findMonthlyJpopCalendar({ year, month });
}

export async function getDailyJpopCalendar({ date }) {
  return findDailyJpopCalendar({ date });
}

export async function getUpcomingJpopCalendar({
  userId,
  limit,
  artistId,
  type,
  country,
  city,
}) {
  return findUpcomingJpopCalendar({
    userId,
    limit,
    artistId,
    type,
    country,
    city,
  });
}

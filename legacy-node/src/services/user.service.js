import { findMyStreakData } from "../repositories/streak.repository.js";

export async function getMyStreak({ userId, year, month }) {
  const result = await findMyStreakData({ userId, year, month });

  const scores = new Map(
    result.history.map((day) => [day.date, Number(day.score)]),
  );

  // 오늘 점수는 실시간 계산 결과를 사용
  scores.set(result.today, result.todayScore);

  // 화면에 반환할 날짜 목록
  const days = result.displayDates.map((date) => ({
    date,
    score: scores.get(date) ?? 0,
  }));

  // 오늘부터 거꾸로 연속 활동 날짜 계산
  let currentStreak = 0;
  let date = result.today;

  while (scores.has(date) && scores.get(date) > 0) {
    currentStreak += 1;
    date = shiftDate(date, -1);
  }

  return {
    days,
    current_streak: currentStreak,
    today_score: result.todayScore,
  };
}

function shiftDate(dateString, offset) {
  const [year, month, day] = dateString.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + offset));

  return [
    date.getUTCFullYear(),
    String(date.getUTCMonth() + 1).padStart(2, "0"),
    String(date.getUTCDate()).padStart(2, "0"),
  ].join("-");
}

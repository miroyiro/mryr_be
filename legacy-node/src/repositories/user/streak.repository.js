import db from "../../config/database.js";

export async function findMyStreakData({ userId, year, month }) {
  // 한국 시간 기준 오늘 날짜
  const todayResult = await db.query(`
    SELECT to_char(
      CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul',
      'YYYY-MM-DD'
    ) AS today
  `);

  const today = todayResult.rows[0].today;

  // 조회 화면의 시작일과 종료일
  let startDate;
  let endDate;

  if (year !== undefined && month !== undefined) {
    const rangeResult = await db.query(
      `SELECT
         to_char(make_date($1, $2, 1), 'YYYY-MM-DD') AS start_date,
         to_char(
           (make_date($1, $2, 1) + INTERVAL '1 month - 1 day')::date,
           'YYYY-MM-DD'
         ) AS end_date`,
      [year, month],
    );

    startDate = rangeResult.rows[0].start_date;
    endDate = rangeResult.rows[0].end_date;
  } else {
    const rangeResult = await db.query(`
      SELECT
        to_char(
          (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date - 83,
          'YYYY-MM-DD'
        ) AS start_date,
        to_char(
          CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul',
          'YYYY-MM-DD'
        ) AS end_date
    `);

    startDate = rangeResult.rows[0].start_date;
    endDate = rangeResult.rows[0].end_date;
  }

  // 조회 구간에 해당하는 날짜 목록 생성
  const datesResult = await db.query(
    `SELECT to_char(d::date, 'YYYY-MM-DD') AS date
     FROM generate_series(
       $1::date,
       $2::date,
       INTERVAL '1 day'
     ) AS d
     WHERE d::date <= $3::date
     ORDER BY d`,
    [startDate, endDate, today],
  );

  // 배치로 확정된 과거 점수 조회
  // 최근 12주 점수는 현재 연속 활동 일수 계산에도 사용
  const historyResult = await db.query(
    `SELECT
       to_char(activity_date, 'YYYY-MM-DD') AS date,
       score
     FROM streak
     WHERE user_id = $1
       AND activity_date >= LEAST($2::date, $3::date) - 83
       AND activity_date <= $3::date
       AND activity_date < $4::date
     ORDER BY activity_date`,
    [userId, startDate, today, today],
  );

  // 오늘의 활동 로그 집계
  const activityResult = await db.query(
    `SELECT
       activity_type,
       COUNT(*)::int AS activity_count
     FROM user_activity_log
     WHERE user_id = $1
       AND created_at >= ($2::date::timestamp AT TIME ZONE 'Asia/Seoul')
       AND created_at < (($2::date + 1)::timestamp AT TIME ZONE 'Asia/Seoul')
     GROUP BY activity_type`,
    [userId, today],
  );

  const counts = Object.fromEntries(
    activityResult.rows.map((row) => [
      row.activity_type,
      Number(row.activity_count),
    ]),
  );

  // 임시 점수 규칙: 일반 활동 1점, 뉴스 열람은 1·3·6회 달성 시 각 1점.
  // 실제 확정 배점과 activity_type 값에 맞춰 조정해야 함.
  const newsViews = counts.news_view ?? 0;
  const newsViewScore =
    Number(newsViews >= 1) + Number(newsViews >= 3) + Number(newsViews >= 6);

  const rawTodayScore =
    (counts.login ?? 0) +
    (counts.like ?? 0) +
    (counts.follow ?? 0) +
    (counts.schedule_registration ?? 0) +
    newsViewScore;

  const todayScore = Math.min(rawTodayScore, 5);

  return {
    today,
    todayScore,
    displayDates: datesResult.rows.map((row) => row.date),
    history: historyResult.rows,
  };
}

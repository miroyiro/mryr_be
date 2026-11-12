import db from "../config/database.js";

export async function createCalendarEntry({ userId, eventId, isAlert, memo }) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // 1. 이벤트 존재 여부 확인
    const eventResult = await client.query(
      `SELECT id
       FROM events
       WHERE id = $1`,
      [eventId],
    );

    if (eventResult.rowCount === 0) {
      const error = new Error("등록할 이벤트를 찾을 수 없습니다.");
      error.statusCode = 404;
      throw error;
    }

    // 2. 내 캘린더에 일정 등록
    const calendarResult = await client.query(
      `INSERT INTO user_calendar (
         user_id,
         event_id,
         status,
         is_alert,
         memo
       )
       VALUES ($1, $2, 'planned', $3, $4)
       RETURNING
         calendar_id,
         event_id,
         status,
         created_at`,
      [userId, eventId, isAlert, memo],
    );

    const calendar = calendarResult.rows[0];

    // 3. 스트릭용 활동 기록 저장
    await client.query(
      `INSERT INTO user_activity_log (
         user_id,
         activity_type,
         target_type,
         target_id
       )
       VALUES ($1, $2, $3, $4)`,
      [userId, "calendar_register", "event", eventId],
    );

    await client.query("COMMIT");

    return {
      calendar_id: calendar.calendar_id,
      event_id: calendar.event_id,
      status: calendar.status,
      created_at: calendar.created_at,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

export async function updateCalendarEntry({ userId, calendarId, updates }) {
  const columnMap = {
    status: "status",
    memo: "memo",
    is_alert: "is_alert",
  };

  const fields = Object.keys(updates).filter((key) =>
    Object.hasOwn(columnMap, key),
  );

  const values = [];
  const setClauses = [];

  for (const field of fields) {
    values.push(updates[field]);

    // SQL 컬럼명은 허용 목록에서 가져오고,
    // 실제 값은 $1, $2 같은 파라미터로 전달합니다.
    setClauses.push(`${columnMap[field]} = $${values.length}`);
  }

  if (setClauses.length === 0) {
    const error = new Error("수정할 항목이 없습니다.");
    error.statusCode = 400;
    throw error;
  }

  // updated_at 컬럼이 있다는 전제입니다.
  setClauses.push("updated_at = NOW()");

  values.push(calendarId);
  const calendarIdParam = `$${values.length}`;

  values.push(userId);
  const userIdParam = `$${values.length}`;

  const result = await db.query(
    `
      UPDATE user_calendar
      SET ${setClauses.join(", ")}
      WHERE calendar_id = ${calendarIdParam}
        AND user_id = ${userIdParam}
      RETURNING
        calendar_id,
        event_id,
        status,
        memo,
        is_alert,
        updated_at
    `,
    values,
  );

  if (result.rowCount === 0) {
    const error = new Error(
      "캘린더 항목을 찾을 수 없거나 수정 권한이 없습니다.",
    );
    error.statusCode = 403;
    throw error;
  }

  const calendar = result.rows[0];

  return {
    calendar_id: calendar.calendar_id,
    event_id: calendar.event_id,
    status: calendar.status,
    memo: calendar.memo,
    is_alert: calendar.is_alert,
    updated_at: calendar.updated_at,
  };
}

export async function deleteCalendarEntry({ userId, calendarId }) {
  const result = await db.query(
    `
      DELETE FROM user_calendar
      WHERE calendar_id = $1
        AND user_id = $2
      RETURNING calendar_id
    `,
    [calendarId, userId],
  );

  if (result.rowCount === 0) {
    const error = new Error(
      "캘린더 항목을 찾을 수 없거나 삭제 권한이 없습니다.",
    );
    error.statusCode = 403;
    throw error;
  }

  return {
    calendar_id: result.rows[0].calendar_id,
  };
}

export async function findMonthlyAnimeCalendar({ year, month }) {
  const result = await db.query(
    `
      SELECT
        e.id AS event_id,
        e.anime_id,
        COALESCE(
          a.title_ko,
          a.title_romaji,
          a.title_native,
          a.title_en
        ) AS anime_title,
        e.title,
        e.type AS event_type,
        e.start_date,
        e.end_date,
        e.status,
        e.poster_url AS thumbnail_url
      FROM events e
      INNER JOIN anime a
        ON a.id = e.anime_id
      WHERE
        e.start_date <= (
          make_date($1, $2, 1)
          + INTERVAL '1 month'
          - INTERVAL '1 day'
        )::date
        AND (
          e.end_date IS NULL
          OR e.end_date >= make_date($1, $2, 1)
        )
      ORDER BY
        e.start_date ASC,
        e.id ASC
    `,
    [year, month],
  );

  return result.rows.map((row) => ({
    event_id: row.event_id,
    anime_id: row.anime_id,
    anime_title: row.anime_title,
    title: row.title,
    event_type: row.event_type,
    start_date: row.start_date,
    end_date: row.end_date,
    status: row.status,
    thumbnail_url: row.thumbnail_url,
  }));
}

export async function findDailyAnimeCalendar({ date }) {
  const result = await db.query(
    `
      SELECT
        e.id AS event_id,
        e.anime_id,
        COALESCE(
          a.title_ko,
          a.title_romaji,
          a.title_native,
          a.title_en
        ) AS anime_title,
        e.title,
        e.type AS event_type,
        e.start_date,
        e.end_date,
        e.status,
        e.poster_url AS thumbnail_url
      FROM events e
      INNER JOIN anime a
        ON a.id = e.anime_id
      WHERE
        e.start_date <= $1::date
        AND (
          e.end_date IS NULL
          OR e.end_date >= $1::date
        )
      ORDER BY
        e.start_date ASC,
        e.id ASC
    `,
    [date],
  );

  return result.rows.map((row) => ({
    event_id: row.event_id,
    anime_id: row.anime_id,
    anime_title: row.anime_title,
    title: row.title,
    event_type: row.event_type,
    start_date: row.start_date,
    end_date: row.end_date,
    status: row.status,
    thumbnail_url: row.thumbnail_url,
  }));
}

export async function findUpcomingAnimeCalendar({
  userId,
  limit,
  animeId,
  type,
  city,
}) {
  const values = [userId];
  const conditions = [
    `
      EXISTS (
        SELECT 1
        FROM user_anime_follow uaf
        WHERE uaf.user_id = $1
          AND uaf.anime_id = e.anime_id
      )
    `,
    `e.start_date <= CURRENT_DATE + 14`,
    `
      (
        e.end_date IS NULL
        OR e.end_date >= CURRENT_DATE
      )
    `,
  ];

  if (animeId !== undefined) {
    values.push(animeId);
    conditions.push(`e.anime_id = $${values.length}`);
  }

  if (type !== undefined) {
    values.push(type);
    conditions.push(`e.type = $${values.length}`);
  }

  if (city !== undefined) {
    values.push(city);
    conditions.push(`e.city = $${values.length}`);
  }

  values.push(limit);

  const result = await db.query(
    `
      SELECT
        e.id AS event_id,
        e.anime_id,
        COALESCE(
          a.title_ko,
          a.title_romaji,
          a.title_native,
          a.title_en
        ) AS anime_title,
        e.title,
        e.type AS event_type,
        e.start_date,
        e.end_date,
        e.city,
        e.poster_url AS thumbnail_url
      FROM events e
      INNER JOIN anime a
        ON a.id = e.anime_id
      WHERE ${conditions.join("\n AND ")}
      ORDER BY
        e.start_date ASC,
        e.id ASC
      LIMIT $${values.length}
    `,
    values,
  );

  return result.rows.map((row) => ({
    event_id: row.event_id,
    anime_id: row.anime_id,
    anime_title: row.anime_title,
    title: row.title,
    event_type: row.event_type,
    start_date: row.start_date,
    end_date: row.end_date,
    city: row.city,
    thumbnail_url: row.thumbnail_url,
  }));
}

// J-POP

export async function findMonthlyJpopCalendar({ year, month }) {
  const result = await db.query(
    `
      SELECT
        e.id AS event_id,
        e.artist_id,
        COALESCE(
          a.name,
          a.name_ja,
          a.name_en
        ) AS artist_name,
        e.title,
        e.type AS event_type,
        e.start_date,
        e.end_date,
        e.country,
        e.city,
        e.venue,
        e.poster_url AS thumbnail_url
      FROM events e
      INNER JOIN artists a
        ON a.id = e.artist_id
      WHERE
        e.artist_id IS NOT NULL
        AND e.start_date <= (
          make_date($1, $2, 1)
          + INTERVAL '1 month'
          - INTERVAL '1 day'
        )::date
        AND (
          e.end_date IS NULL
          OR e.end_date >= make_date($1, $2, 1)
        )
      ORDER BY
        e.start_date ASC,
        e.id ASC
    `,
    [year, month],
  );

  return result.rows.map((row) => ({
    event_id: row.event_id,
    artist_id: row.artist_id,
    artist_name: row.artist_name,
    title: row.title,
    event_type: row.event_type,
    start_date: row.start_date,
    end_date: row.end_date,
    country: row.country,
    city: row.city,
    venue: row.venue,
    thumbnail_url: row.thumbnail_url,
  }));
}

export async function findDailyJpopCalendar({ date }) {
  const result = await db.query(
    `
      SELECT
        e.id AS event_id,
        e.artist_id,
        COALESCE(
          a.name,
          a.name_ja,
          a.name_en
        ) AS artist_name,
        e.title,
        e.type AS event_type,
        e.start_date,
        e.end_date,
        e.country,
        e.city,
        e.venue,
        e.poster_url AS thumbnail_url
      FROM events e
      INNER JOIN artists a
        ON a.id = e.artist_id
      WHERE
        e.artist_id IS NOT NULL
        AND e.start_date <= $1::date
        AND (
          e.end_date IS NULL
          OR e.end_date >= $1::date
        )
      ORDER BY
        e.start_date ASC,
        e.id ASC
    `,
    [date],
  );

  return result.rows.map((row) => ({
    event_id: row.event_id,
    artist_id: row.artist_id,
    artist_name: row.artist_name,
    title: row.title,
    event_type: row.event_type,
    start_date: row.start_date,
    end_date: row.end_date,
    country: row.country,
    city: row.city,
    venue: row.venue,
    thumbnail_url: row.thumbnail_url,
  }));
}

export async function findUpcomingJpopCalendar({
  userId,
  limit,
  artistId,
  type,
  country,
  city,
}) {
  const values = [userId];
  const conditions = [
    `EXISTS (
      SELECT 1
      FROM user_artist_follow uaf
      WHERE uaf.user_id = $1
        AND uaf.artist_id = e.artist_id
    )`,
    `e.start_date <= CURRENT_DATE + 14`,
    `(e.end_date IS NULL OR e.end_date >= CURRENT_DATE)`,
    `e.artist_id IS NOT NULL`,
  ];

  if (artistId !== undefined) {
    values.push(artistId);
    conditions.push(`e.artist_id = $${values.length}`);
  }

  if (type !== undefined) {
    values.push(type);
    conditions.push(`e.type = $${values.length}`);
  }

  if (country !== undefined) {
    values.push(country);
    conditions.push(`UPPER(e.country) = $${values.length}`);
  }

  if (city !== undefined) {
    values.push(city);
    conditions.push(`e.city ILIKE $${values.length}`);
  }

  values.push(limit);

  const sql = `
    SELECT
      e.id AS event_id,
      e.artist_id,
      COALESCE(a.name, a.name_ja, a.name_en) AS artist_name,
      e.title,
      e.type AS event_type,
      e.start_date,
      e.end_date,
      e.country,
      e.city,
      e.venue,
      e.poster_url AS thumbnail_url
    FROM events e
    INNER JOIN artists a
      ON a.id = e.artist_id
    WHERE ${conditions.join("\n      AND ")}
    ORDER BY e.start_date ASC, e.id ASC
    LIMIT $${values.length}
  `;

  const result = await db.query(sql, values);

  return result.rows;
}

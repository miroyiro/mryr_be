import db from "../../config/database.js";

export async function findEvents({
  category,
  type,
  status,
  animeId,
  artistId,
  city,
  from,
  to,
  page,
  limit,
}) {
  const values = [];
  const conditions = [];

  function addCondition(sql, value) {
    values.push(value);
    conditions.push(sql.replace("?", `$${values.length}`));
  }

  if (category !== undefined) {
    addCondition("e.category = ?", category);
  }

  if (type !== undefined) {
    addCondition("e.type = ?", type);
  }

  if (status !== undefined) {
    addCondition("e.status = ?", status);
  }

  if (animeId !== undefined) {
    addCondition("e.anime_id = ?", animeId);
  }

  if (artistId !== undefined) {
    addCondition("e.artist_id = ?", artistId);
  }

  if (city !== undefined) {
    addCondition("e.city ILIKE ?", `%${city}%`);
  }

  // 기간과 겹치는 이벤트를 조회
  if (from !== undefined) {
    addCondition("(e.end_date IS NULL OR e.end_date >= ?::date)", from);
  }

  if (to !== undefined) {
    addCondition("e.start_date <= ?::date", to);
  }

  const whereSql = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";

  // 전체 결과 개수 조회
  const countResult = await db.query(
    `
      SELECT COUNT(*)::int AS total
      FROM events e
      ${whereSql}
    `,
    values,
  );

  const total = countResult.rows[0].total;

  // 페이지네이션
  const offset = (page - 1) * limit;
  const listValues = [...values, limit, offset];

  const result = await db.query(
    `
      SELECT
        e.id AS event_id,
        e.category,
        e.anime_id,
        e.artist_id,
        e.title,
        e.type,
        e.status,
        e.start_date,
        e.end_date,
        e.country,
        e.city,
        e.venue,
        e.poster_url AS thumbnail_url
      FROM events e
      ${whereSql}
      ORDER BY e.start_date ASC NULLS LAST, e.id ASC
      LIMIT $${values.length + 1}
      OFFSET $${values.length + 2}
    `,
    listValues,
  );

  return {
    data: result.rows,
    total,
  };
}

export async function findEventDetail({ eventId, userId }) {
  const result = await db.query(
    `
      WITH viewed_event AS (
        UPDATE events
        SET view_count = COALESCE(view_count, 0) + 1
        WHERE id = $1
        RETURNING *
      )
      SELECT
        e.id AS event_id,
        e.category,
        e.anime_id,
        e.artist_id,
        COALESCE(a.name, a.name_ja, a.name_en) AS artist_name,
        e.title,
        e.type,
        e.status,
        e.start_date,
        e.end_date,
        e.country,
        e.city,
        e.venue,
        e.poster_url AS thumbnail_url,
        e.view_count,
        uc.calendar_id,
        (uc.calendar_id IS NOT NULL) AS is_registered
      FROM viewed_event e
      LEFT JOIN artists a
        ON a.id = e.artist_id
      LEFT JOIN user_calendar uc
        ON uc.event_id = e.id
       AND uc.user_id = $2
      LIMIT 1
    `,
    [eventId, userId],
  );

  return result.rows[0] ?? null;
}

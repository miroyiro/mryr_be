import db from "../config/database.js";

export async function findSearchResults({ q, type, limit }) {
  // 검색어에 포함된 %, _를 와일드카드가 아닌 문자로 취급
  const escapedQuery = q.replace(/[\\%_]/g, "\\$&");
  const keyword = `%${escapedQuery}%`;

  const queries = [];

  // 1. 아티스트 검색
  if (!type || type === "artist") {
    queries.push(`
      SELECT
        'artist'::text AS type,
        id,
        name AS title,
        name_en AS subtitle,
        thumbnail_url
      FROM artists
      WHERE name ILIKE $1 ESCAPE E'\\\\'
         OR name_en ILIKE $1 ESCAPE E'\\\\'
         OR name_ja ILIKE $1 ESCAPE E'\\\\'
    `);
  }

  // 2. 애니메이션 검색
  if (!type || type === "anime") {
    queries.push(`
      SELECT
        'anime'::text AS type,
        id,
        COALESCE(title_ko, title_romaji, title_native, title_en) AS title,
        COALESCE(title_romaji, title_native, title_en) AS subtitle,
        thumbnail_url
      FROM anime
      WHERE title_ko ILIKE $1 ESCAPE E'\\\\'
         OR title_romaji ILIKE $1 ESCAPE E'\\\\'
         OR title_native ILIKE $1 ESCAPE E'\\\\'
         OR title_en ILIKE $1 ESCAPE E'\\\\'
    `);
  }

  // 3. 공연 및 이벤트 검색
  if (!type || type === "event") {
    queries.push(`
      SELECT
        'event'::text AS type,
        id,
        title,
        venue AS subtitle,
        poster_url AS thumbnail_url
      FROM events
      WHERE title ILIKE $1 ESCAPE E'\\\\'
         OR venue ILIKE $1 ESCAPE E'\\\\'
    `);
  }

  // 선택한 검색 대상의 결과를 합친 뒤 전체 결과 개수 제한
  const sql = `
    SELECT type, id, title, subtitle, thumbnail_url
    FROM (
      ${queries.join("\nUNION ALL\n")}
    ) AS search_results
    ORDER BY title ASC
    LIMIT $2
  `;

  const result = await db.query(sql, [keyword, limit]);

  return result.rows.map((row) => ({
    type: row.type,
    id: row.id,
    title: row.title,
    subtitle: row.subtitle ?? null,
    thumbnail_url: row.thumbnail_url ?? null,
  }));
}

// 최근 24시간 검색 기록을 바탕으로 시간별 순위 저장
export async function savePopularKeywordSnapshot() {
  const sql = `
    WITH current_hour AS (
      SELECT date_trunc('hour', NOW()) AS snapshot_hour
    ),
    ranked_keywords AS (
      SELECT
        LOWER(TRIM(keyword)) AS keyword,
        COUNT(*)::int AS search_count,
        ROW_NUMBER() OVER (
          ORDER BY COUNT(*) DESC, LOWER(TRIM(keyword)) ASC
        )::int AS rank
      FROM user_search_history
      WHERE created_at >= NOW() - INTERVAL '24 hours'
        AND TRIM(keyword) <> ''
      GROUP BY LOWER(TRIM(keyword))
    )
    INSERT INTO popular_keyword_snapshot (
      snapshot_hour,
      keyword,
      search_count,
      rank
    )
    SELECT
      current_hour.snapshot_hour,
      ranked_keywords.keyword,
      ranked_keywords.search_count,
      ranked_keywords.rank
    FROM ranked_keywords
    CROSS JOIN current_hour
    ON CONFLICT (snapshot_hour, keyword)
    DO UPDATE SET
      search_count = EXCLUDED.search_count,
      rank = EXCLUDED.rank
  `;

  await db.query(sql);
}

// 최신 스냅샷과 이전 스냅샷으로 추세 계산
export async function findPopularKeywords(limit) {
  const sql = `
    WITH snapshot_hours AS (
      SELECT DISTINCT snapshot_hour
      FROM popular_keyword_snapshot
      ORDER BY snapshot_hour DESC
      LIMIT 2
    ),
    latest_hour AS (
      SELECT MAX(snapshot_hour) AS snapshot_hour
      FROM snapshot_hours
    ),
    previous_hour AS (
      SELECT MIN(snapshot_hour) AS snapshot_hour
      FROM snapshot_hours
    ),
    latest AS (
      SELECT p.keyword, p.rank
      FROM popular_keyword_snapshot p
      JOIN latest_hour h
        ON p.snapshot_hour = h.snapshot_hour
    ),
    previous AS (
      SELECT p.keyword, p.rank
      FROM popular_keyword_snapshot p
      JOIN previous_hour h
        ON p.snapshot_hour = h.snapshot_hour
    )
    SELECT
      latest.rank,
      latest.keyword,
      CASE
        WHEN previous.keyword IS NULL THEN 'new'
        WHEN latest.rank < previous.rank THEN 'up'
        WHEN latest.rank > previous.rank THEN 'down'
        ELSE 'same'
      END AS trend,
      (SELECT snapshot_hour FROM latest_hour) AS updated_at
    FROM latest
    LEFT JOIN previous ON previous.keyword = latest.keyword
    ORDER BY latest.rank ASC
    LIMIT $1
  `;

  const result = await db.query(sql, [limit]);

  return {
    items: result.rows.map((row) => ({
      rank: row.rank,
      keyword: row.keyword,
      trend: row.trend,
    })),
    updated_at: result.rows[0]?.updated_at ?? null,
  };
}

// 로그인한 사용자의 최근 검색 기록 조회
export async function findRecentSearches(userId, limit) {
  const sql = `
    SELECT
      id,
      keyword,
      created_at AS searched_at
    FROM user_search_history
    WHERE user_id = $1
      AND hidden_at IS NULL
    ORDER BY created_at DESC, id DESC
    LIMIT $2
  `;

  const result = await db.query(sql, [userId, limit]);

  return result.rows.map((row) => ({
    id: row.id,
    keyword: row.keyword,
    searched_at: row.searched_at,
  }));
}

// 최근 검색어 소프트 삭제
export async function hideRecentSearch(userId, searchHistoryId) {
  const sql = `
    UPDATE user_search_history
    SET hidden_at = NOW()
    WHERE id = $1
      AND user_id = $2
      AND hidden_at IS NULL
    RETURNING id
  `;

  const result = await db.query(sql, [searchHistoryId, userId]);

  if (result.rowCount === 0) {
    return null;
  }

  return result.rows[0];
}

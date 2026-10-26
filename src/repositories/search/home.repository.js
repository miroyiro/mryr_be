import db from "../config/database.js";

export async function findHomeFeedByUserId(userId, limit = 5) {
  const result = await db.query(
    `
    SELECT
      nf.id AS news_id,
      nf.target_type,
      nf.target_id,

      CASE
        WHEN nf.target_type = 'artist' THEN a.name
        WHEN nf.target_type = 'anime' THEN an.title
      END AS target_name,

      nf.title,
      nf.summary,
      nf.source,
      nf.url,
      nf.thumbnail_url,
      nf.published_at,

      (
        SELECT COUNT(*)::int
        FROM news_like nl
        WHERE nl.news_id = nf.id
      ) AS like_count,

      EXISTS (
        SELECT 1
        FROM news_like nl
        WHERE nl.news_id = nf.id
          AND nl.user_id = $1
      ) AS is_liked

    FROM news_feed nf

    LEFT JOIN artists a
      ON nf.target_type = 'artist'
      AND a.id = nf.target_id

    LEFT JOIN anime an
      ON nf.target_type = 'anime'
      AND an.id = nf.target_id

    WHERE
      (
        nf.target_type = 'artist'
        AND EXISTS (
          SELECT 1
          FROM user_artist_follow uaf
          WHERE uaf.user_id = $1
            AND uaf.artist_id = nf.target_id
        )
      )
      OR
      (
        nf.target_type = 'anime'
        AND EXISTS (
          SELECT 1
          FROM user_anime_follow uanf
          WHERE uanf.user_id = $1
            AND uanf.anime_id = nf.target_id
        )
      )

    ORDER BY
      EXISTS (
        SELECT 1
        FROM user_activity_log ual
        WHERE ual.user_id = $1
          AND ual.activity_type = 'news_view'
          AND ual.target_type = 'news'
          AND ual.target_id = nf.id
      ) ASC,
      nf.published_at DESC

    LIMIT $2
    `,
    [userId, limit],
  );

  return result.rows;
}

export async function findGuestHomeFeed() {
  const result = await db.query(`
    SELECT
      id AS event_id,
      category,
      type,
      title,
      poster_url AS thumbnail_url,
      start_at
    FROM events
    WHERE start_at > NOW()
      AND status <> '취소'
      AND category IN ('jpop', 'anime')
    ORDER BY RANDOM()
    LIMIT 5
  `);

  return result.rows;
}

export async function findPopularHomeFeed({ category = null, limit = 5 }) {
  const result = await db.query(
    `
    SELECT
      id AS event_id,
      category,
      type,
      title,
      poster_url AS thumbnail_url,
      start_at,
      COALESCE(view_count, 0)::int AS view_count
    FROM events
    WHERE
      (
        start_at > NOW()
        OR (
          start_at <= NOW()
          AND end_at >= NOW()
        )
      )
      AND status <> '취소'
      AND category IN ('jpop', 'anime')
      AND ($1::text IS NULL OR category = $1)
    ORDER BY
      COALESCE(view_count, 0) DESC,
      start_at ASC
    LIMIT $2
    `,
    [category, limit],
  );

  return result.rows;
}

export async function saveOnboardingFollows(userId, follows) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    const artistFollowIds = [];
    const animeFollowIds = [];
    let pendingCount = 0;

    for (const item of follows) {
      const { type, id, name } = item;

      // 1. 기존 DB에 등록된 항목
      if (id !== null) {
        const table = type === "artist" ? "artists" : "anime";

        const catalogResult = await client.query(
          `SELECT id FROM ${table} WHERE id = $1 LIMIT 1`,
          [id],
        );

        if (catalogResult.rows.length === 0) {
          const error = new Error(`존재하지 않는 ${type} 항목입니다.`);
          error.statusCode = 400;
          throw error;
        }

        if (type === "artist") {
          await client.query(
            `
            INSERT INTO user_artist_follow (user_id, artist_id)
            VALUES ($1, $2)
            ON CONFLICT (user_id, artist_id) DO NOTHING
            `,
            [userId, id],
          );

          artistFollowIds.push(id);
        } else {
          await client.query(
            `
            INSERT INTO user_anime_follow (user_id, anime_id)
            VALUES ($1, $2)
            ON CONFLICT (user_id, anime_id) DO NOTHING
            `,
            [userId, id],
          );

          animeFollowIds.push(id);
        }

        continue;
      }

      // 2. 아직 DB 항목과 연결되지 않은 관심사
      await client.query(
        `
        INSERT INTO user_follow_request (
          user_id,
          target_type,
          raw_input,
          status
        )
        VALUES ($1, $2, $3, 'pending')
        `,
        [userId, type, name],
      );

      pendingCount++;
    }

    // 3. 온보딩 완료 표시
    await client.query(
      `
      UPDATE users
      SET onboarded_at = COALESCE(onboarded_at, NOW())
      WHERE id = $1
      `,
      [userId],
    );

    await client.query("COMMIT");

    return {
      artist_follow_ids: artistFollowIds,
      anime_follow_ids: animeFollowIds,
      pending_count: pendingCount,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

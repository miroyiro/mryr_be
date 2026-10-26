import db from "../../config/database.js";

export async function findAnimeList({
  season,
  seasonYear,
  genre,
  status,
  weekday,
  sort,
  page,
  limit,
}) {
  const values = [];
  const conditions = [];

  function addCondition(sql, value) {
    values.push(value);
    conditions.push(sql.replace("?", `$${values.length}`));
  }

  if (season !== undefined) {
    addCondition("a.season = ?", season);
  }

  if (seasonYear !== undefined) {
    addCondition("a.season_year = ?", seasonYear);
  }

  if (genre !== undefined) {
    addCondition(
      `EXISTS (
        SELECT 1
        FROM unnest(COALESCE(a.genres, ARRAY[]::text[])) AS g(name)
        WHERE LOWER(g.name) = LOWER(?)
      )`,
      genre,
    );
  }

  if (status !== undefined) {
    addCondition("a.status = ?", status);
  }

  if (weekday !== undefined) {
    addCondition("a.broadcast_weekday = ?", weekday);
  }

  const whereSql = conditions.length ? `WHERE ${conditions.join(" AND ")}` : "";

  const countResult = await db.query(
    `
      SELECT COUNT(*)::int AS total
      FROM anime a
      ${whereSql}
    `,
    values,
  );

  const total = countResult.rows[0].total;

  // sort 값은 Controller에서 허용 목록으로 검증함.
  const sortSql = {
    popular: "COALESCE(a.popularity_score, 0) DESC, a.id ASC",
    latest: "a.created_at DESC NULLS LAST, a.id DESC",
    title: "a.title_romaji ASC, a.id ASC",
  }[sort];

  const offset = (page - 1) * limit;
  const listValues = [...values, limit, offset];

  const result = await db.query(
    `
      SELECT
        a.id,
        a.title_ko,
        a.title_romaji,
        a.poster_url,
        a.season,
        a.season_year,
        a.status,
        COALESCE(a.genres, ARRAY[]::text[]) AS genres,
        CASE
          WHEN a.broadcast_weekday IS NOT NULL
            AND a.broadcast_time IS NOT NULL
          THEN json_build_object(
            'weekday', a.broadcast_weekday,
            'time', TO_CHAR(a.broadcast_time, 'HH24:MI')
          )
          ELSE NULL
        END AS broadcast
      FROM anime a
      ${whereSql}
      ORDER BY ${sortSql}
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

export async function findAnimeDetail({ animeId, userId }) {
  const result = await db.query(
    `
      SELECT
        a.id,
        a.anilist_id,
        a.tmdb_id,
        a.title_ko,
        a.title_romaji,
        a.title_native,
        a.title_en,
        a.format,
        a.status,
        a.season,
        a.season_year,
        a.episodes,
        a.age_rating,

        COALESCE(a.genres, ARRAY[]::text[]) AS genres,
        COALESCE(a.studios, ARRAY[]::text[]) AS studios,

        COALESCE(
          (
            SELECT json_agg(
              json_build_object(
                'name', s.name,
                'role', s.role
              )
              ORDER BY s.sort_order NULLS LAST, s.name
            )
            FROM anime_staff s
            WHERE s.anime_id = a.id
          ),
          '[]'::json
        ) AS staff,

        COALESCE(
          (
            SELECT json_agg(
              json_build_object(
                'name', c.name,
                'image_url', c.image_url,
                'role', c.role,
                'voice_actor_name', c.voice_actor_name
              )
              ORDER BY c.sort_order NULLS LAST, c.id
            )
            FROM (
              SELECT
                ac.id,
                ac.name,
                ac.image_url,
                ac.role,
                ac.voice_actor_name,
                ac.sort_order
              FROM anime_character ac
              WHERE ac.anime_id = a.id
              ORDER BY ac.sort_order NULLS LAST, ac.id
              LIMIT 20
            ) c
          ),
          '[]'::json
        ) AS characters,

        CASE
          WHEN a.broadcast_weekday IS NOT NULL
            AND a.broadcast_time IS NOT NULL
          THEN json_build_object(
            'weekday', a.broadcast_weekday,
            'time', TO_CHAR(a.broadcast_time, 'HH24:MI')
          )
          ELSE NULL
        END AS broadcast,

        a.poster_url,
        a.synopsis_ko,
        a.synopsis_en,
        a.official_url,

        (
          SELECT ROUND(AVG(r.rating)::numeric, 1)
          FROM anime_review r
          WHERE r.anime_id = a.id
        ) AS avg_rating,

        (
          SELECT COUNT(*)::int
          FROM anime_review r
          WHERE r.anime_id = a.id
        ) AS review_count,

        CASE
          WHEN $2::bigint IS NULL THEN NULL
          ELSE EXISTS (
            SELECT 1
            FROM user_anime_follow uaf
            WHERE uaf.anime_id = a.id
              AND uaf.user_id = $2
          )
        END AS is_followed

      FROM anime a
      WHERE a.id = $1
      LIMIT 1
    `,
    [animeId, userId],
  );

  return result.rows[0] ?? null;
}

export async function findAnimeEpisodes({ animeId, order }) {
  // 먼저 작품이 존재하는지 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 정렬 방향은 검증된 값만 사용
  const sortDirection = order === "desc" ? "DESC" : "ASC";

  const result = await db.query(
    `
      SELECT
        episode_no,
        overall_no,
        title,
        air_date,
        duration_min
      FROM anime_episode
      WHERE anime_id = $1
      ORDER BY episode_no ${sortDirection}, overall_no ${sortDirection}
    `,
    [animeId],
  );

  return result.rows;
}

export async function createAnimeFollow({ animeId, userId }) {
  // 1. 애니 작품이 존재하는지 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 팔로우 등록 시도
  // 이미 등록되어 있다면 중복 INSERT를 하지 않음
  const insertResult = await db.query(
    `
      INSERT INTO user_anime_follow (
        user_id,
        anime_id,
        match_status
      )
      VALUES ($1, $2, 'matched')
      ON CONFLICT (user_id, anime_id) DO NOTHING
      RETURNING
        follow_id,
        anime_id,
        true AS is_followed
    `,
    [userId, animeId],
  );

  // 3. 새로 등록된 경우
  if (insertResult.rows.length > 0) {
    return {
      data: insertResult.rows[0],
      created: true,
    };
  }

  // 4. 이미 팔로우 중이라면 기존 데이터 조회
  const existingResult = await db.query(
    `
      SELECT
        follow_id,
        anime_id,
        true AS is_followed
      FROM user_anime_follow
      WHERE user_id = $1
        AND anime_id = $2
      LIMIT 1
    `,
    [userId, animeId],
  );

  if (existingResult.rows.length === 0) {
    throw new Error("팔로우 등록 결과를 확인할 수 없습니다.");
  }

  return {
    data: existingResult.rows[0],
    created: false,
  };
}

export async function deleteAnimeFollow({ animeId, userId }) {
  // 1. 애니 작품이 존재하는지 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 현재 로그인한 사용자의 팔로우만 삭제
  await db.query(
    `
      DELETE FROM user_anime_follow
      WHERE user_id = $1
        AND anime_id = $2
    `,
    [userId, animeId],
  );

  // 3. 해제 결과 반환
  return {
    anime_id: animeId,
    is_followed: false,
  };
}

export async function insertAnimeReview({ animeId, userId, rating, content }) {
  // 1. 작품 존재 여부 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 리뷰 등록
  // 같은 사용자가 같은 작품에 이미 리뷰를 작성했다면 중복 등록 방지
  const result = await db.query(
    `
      INSERT INTO anime_review (
        user_id,
        anime_id,
        rating,
        content
      )
      VALUES ($1, $2, $3, $4)
      ON CONFLICT (user_id, anime_id) DO NOTHING
      RETURNING
        review_id,
        anime_id,
        rating,
        content,
        created_at
    `,
    [userId, animeId, rating, content],
  );

  // 3. 신규 등록 성공
  if (result.rows.length > 0) {
    return {
      data: result.rows[0],
      conflict: false,
    };
  }

  // 4. 이미 리뷰가 존재함
  return {
    data: null,
    conflict: true,
  };
}

export async function findAnimeReviews({ animeId, page, limit }) {
  // 1. 작품 존재 여부 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 전체 리뷰 개수 조회
  const countResult = await db.query(
    `
      SELECT COUNT(*)::int AS total
      FROM anime_review
      WHERE anime_id = $1
    `,
    [animeId],
  );

  const total = countResult.rows[0].total;

  // 3. 리뷰 목록 조회
  const offset = (page - 1) * limit;

  const reviewsResult = await db.query(
    `
      SELECT
        r.review_id,
        r.user_id,
        u.nickname,
        r.rating,
        r.content,
        r.created_at
      FROM anime_review r
      JOIN users u
        ON u.id = r.user_id
      WHERE r.anime_id = $1
      ORDER BY r.created_at DESC, r.review_id DESC
      LIMIT $2
      OFFSET $3
    `,
    [animeId, limit, offset],
  );

  return {
    reviews: reviewsResult.rows,
    total,
  };
}

export async function updateAnimeReviewById({
  animeId,
  reviewId,
  userId,
  rating,
  content,
}) {
  // 1. 작품이 존재하는지 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 해당 작품의 리뷰가 존재하는지 확인
  const reviewResult = await db.query(
    `
      SELECT review_id, user_id
      FROM anime_review
      WHERE review_id = $1
        AND anime_id = $2
      LIMIT 1
    `,
    [reviewId, animeId],
  );

  if (reviewResult.rows.length === 0) {
    return null;
  }

  // 3. 리뷰 작성자 권한 확인
  if (String(reviewResult.rows[0].user_id) !== String(userId)) {
    return { forbidden: true };
  }

  // 4. 전달된 필드만 수정하고 나머지는 유지
  const result = await db.query(
    `
      UPDATE anime_review
      SET
        rating = COALESCE($3, rating),
        content = COALESCE($4, content),
        updated_at = NOW()
      WHERE review_id = $1
        AND anime_id = $2
        AND user_id = $5
      RETURNING
        review_id,
        anime_id,
        rating,
        content,
        created_at,
        updated_at
    `,
    [reviewId, animeId, rating ?? null, content ?? null, userId],
  );

  return result.rows[0] ?? null;
}

export async function deleteAnimeReviewById({ animeId, reviewId, userId }) {
  // 1. 작품이 존재하는지 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 해당 작품의 리뷰가 존재하는지 확인
  const reviewResult = await db.query(
    `
      SELECT review_id, user_id
      FROM anime_review
      WHERE review_id = $1
        AND anime_id = $2
      LIMIT 1
    `,
    [reviewId, animeId],
  );

  if (reviewResult.rows.length === 0) {
    return null;
  }

  // 3. 작성자 확인
  if (String(reviewResult.rows[0].user_id) !== String(userId)) {
    return { forbidden: true };
  }

  // 4. 본인의 리뷰 삭제
  const deleteResult = await db.query(
    `
      DELETE FROM anime_review
      WHERE review_id = $1
        AND anime_id = $2
        AND user_id = $3
      RETURNING review_id
    `,
    [reviewId, animeId, userId],
  );

  if (deleteResult.rows.length === 0) {
    return null;
  }

  return {
    review_id: deleteResult.rows[0].review_id,
    anime_id: animeId,
    deleted: true,
  };
}

export async function findAnimeStreaming({ animeId }) {
  // 1. 작품 존재 여부 확인
  const animeResult = await db.query(
    `
      SELECT id
      FROM anime
      WHERE id = $1
      LIMIT 1
    `,
    [animeId],
  );

  if (animeResult.rows.length === 0) {
    return null;
  }

  // 2. 스트리밍 플랫폼 목록 조회
  const result = await db.query(
    `
      SELECT
        platform_name AS platform,
        watch_url,
        availability_type,
        region
      FROM anime_streaming
      WHERE anime_id = $1
        AND is_available = TRUE
      ORDER BY platform_name ASC, region ASC
    `,
    [animeId],
  );

  return result.rows;
}

import db from "../../config/database.js";

export async function replaceManualAnimeStreaming({ animeId, platforms }) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // 1. 애니메이션 존재 여부 확인
    const animeResult = await client.query(
      `SELECT id
       FROM anime
       WHERE id = $1
       LIMIT 1`,
      [animeId],
    );

    if (animeResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return null;
    }

    // 2. 기존 수동 등록 링크만 삭제
    await client.query(
      `DELETE FROM anime_streaming
       WHERE anime_id = $1
         AND source = 'manual'`,
      [animeId],
    );

    // 3. URL이 있는 플랫폼만 다시 등록
    const platformsToInsert = platforms.filter((item) => item.url !== null);

    for (const item of platformsToInsert) {
      await client.query(
        `INSERT INTO anime_streaming (
           anime_id,
           platform_name,
           watch_url,
           source,
           is_available
         )
         VALUES ($1, $2, $3, 'manual', TRUE)`,
        [animeId, item.platform, item.url],
      );
    }

    await client.query("COMMIT");

    return {
      updated: true,
      platforms_count: platformsToInsert.length,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

export async function createArtist({
  name,
  name_en,
  name_ja,
  category,
  profile_image,
  description,
  debut_date,
  official_url,
}) {
  const result = await db.query(
    `INSERT INTO artist (
       name_ko,
       name_en,
       name_ja,
       category,
       profile_image_url,
       description,
       debut_date,
       official_url
     )
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
     RETURNING id, created_at`,
    [
      name,
      name_en,
      name_ja,
      category,
      profile_image,
      description,
      debut_date,
      official_url,
    ],
  );

  return result.rows[0];
}

const ARTIST_UPDATE_COLUMNS = {
  name: "name_ko",
  name_en: "name_en",
  name_ja: "name_ja",
  profile_image: "profile_image_url",
  description: "description",
  debut_date: "debut_date",
  official_url: "official_url",
};

export async function updateArtist({ artistId, fields }) {
  const entries = Object.entries(fields).filter(([key]) =>
    Object.hasOwn(ARTIST_UPDATE_COLUMNS, key),
  );

  if (entries.length === 0) {
    return { error: "NO_FIELDS" };
  }

  const setClauses = entries.map(
    ([key], index) => `${ARTIST_UPDATE_COLUMNS[key]} = $${index + 1}`,
  );

  const values = entries.map(([, value]) => value);
}

export async function createArtistAlbum({
  artistId,
  title,
  album_type,
  release_date,
  cover_image,
  external_url,
}) {
  // 아티스트 존재 여부 확인
  const artistResult = await db.query(
    `SELECT id
     FROM artist
     WHERE id = $1
     LIMIT 1`,
    [artistId],
  );

  if (artistResult.rows.length === 0) {
    return null;
  }

  const result = await db.query(
    `INSERT INTO album (
       artist_id,
       title,
       album_type,
       release_date,
       cover_image_url,
       external_url
     )
     VALUES ($1, $2, $3, $4, $5, $6)
     RETURNING id`,
    [artistId, title, album_type, release_date, cover_image, external_url],
  );

  return result.rows[0];
}

// 아티스트 곡 등록
export async function createArtistSong({
  artistId,
  title,
  album_id,
  track_no,
  release_date,
  is_representative,
  external_url,
}) {
  // 1. 아티스트 존재 여부 확인
  const artistResult = await db.query(
    `SELECT id
     FROM artist
     WHERE id = $1`,
    [artistId],
  );

  if (artistResult.rows.length === 0) {
    return { error: "ARTIST_NOT_FOUND" };
  }

  // 2. 앨범 ID가 있다면 해당 아티스트의 앨범인지 확인
  if (album_id !== null) {
    const albumResult = await db.query(
      `SELECT id
       FROM album
       WHERE id = $1
         AND artist_id = $2`,
      [album_id, artistId],
    );

    if (albumResult.rows.length === 0) {
      return { error: "INVALID_ALBUM" };
    }
  }

  // 3. 곡 등록
  const result = await db.query(
    `INSERT INTO song (
       artist_id,
       album_id,
       title,
       track_no,
       release_date,
       is_representative,
       external_url
     )
     VALUES ($1, $2, $3, $4, $5, $6, $7)
     RETURNING id`,
    [
      artistId,
      album_id,
      title,
      track_no,
      release_date,
      is_representative,
      external_url,
    ],
  );

  return result.rows[0];
}

export async function createAdminEvent({ eventData, adminId }) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // 후보 ID가 전달되었다면 존재 여부 확인
    if (eventData.processed_item_id !== null) {
      const candidateResult = await client.query(
        `SELECT id
         FROM processed_item
         WHERE id = $1
         FOR UPDATE`,
        [eventData.processed_item_id],
      );

      if (candidateResult.rows.length === 0) {
        await client.query("ROLLBACK");
        return { error: "CANDIDATE_NOT_FOUND" };
      }
    }

    // 이벤트 등록
    const result = await client.query(
      `INSERT INTO event (
         category,
         type,
         title,
         artist_id,
         anime_id,
         venue,
         address,
         city,
         lat,
         lng,
         start_at,
         end_at,
         is_all_day,
         reservation_required,
         poster_url,
         ticket_url,
         source_url,
         source,
         created_by
       )
       VALUES (
         $1, $2, $3, $4, $5, $6, $7, $8, $9,
         $10, $11, $12, $13, $14, $15, $16, $17,
         'manual', $18
       )
       RETURNING id, created_at`,
      [
        eventData.category,
        eventData.type,
        eventData.title,
        eventData.artist_id,
        eventData.anime_id,
        eventData.venue,
        eventData.address,
        eventData.city,
        eventData.lat,
        eventData.lng,
        eventData.start_at,
        eventData.end_at,
        eventData.is_all_day,
        eventData.reservation_required,
        eventData.poster_url,
        eventData.ticket_url,
        eventData.source_url,
        adminId,
      ],
    );

    // 후보에서 등록한 경우 게시 완료 처리
    if (eventData.processed_item_id !== null) {
      await client.query(
        `UPDATE processed_item
         SET status = 'published'
         WHERE id = $1`,
        [eventData.processed_item_id],
      );
    }

    await client.query("COMMIT");

    return result.rows[0];
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

// 관리자 이벤트 삭제
export async function deleteAdminEvent(eventId) {
  const result = await db.query(
    `DELETE FROM event
     WHERE id = $1
     RETURNING id`,
    [eventId],
  );

  if (result.rowCount === 0) {
    return null;
  }

  return {
    deleted: true,
  };
}

const EVENT_UPDATE_COLUMNS = {
  title: "title",
  venue: "venue",
  address: "address",
  city: "city",
  lat: "lat",
  lng: "lng",
  start_at: "start_at",
  end_at: "end_at",
  status: "status",
  source_url: "source_url",
};

// 관리자 이벤트 수정
export async function updateAdminEvent({ eventId, fields }) {
  const entries = Object.entries(fields).filter(([key]) =>
    Object.hasOwn(EVENT_UPDATE_COLUMNS, key),
  );

  if (entries.length === 0) {
    return { error: "NO_FIELDS" };
  }

  const setClauses = entries.map(
    ([key], index) => `${EVENT_UPDATE_COLUMNS[key]} = $${index + 1}`,
  );

  const values = entries.map(([, value]) => value);
  values.push(eventId);

  const result = await db.query(
    `UPDATE event
     SET ${setClauses.join(", ")},
         updated_at = NOW()
     WHERE id = $${values.length}
     RETURNING id, updated_at`,
    values,
  );

  if (result.rows.length === 0) {
    return null;
  }

  return result.rows[0];
}

// 관리자 미매칭 팔로우 요청 조회
export async function findAdminFollowRequests({
  followType,
  matchStatus,
  page,
  limit,
}) {
  const conditions = ["match_status IN ('pending', 'unmatched')"];
  const values = [];

  if (followType) {
    values.push(followType);
    conditions.push(`follow_type = $${values.length}`);
  }

  if (matchStatus) {
    values.push(matchStatus);
    conditions.push(`match_status = $${values.length}`);
  }

  const whereClause = conditions.join(" AND ");

  // 같은 유형의 검색어를 그룹화
  const groupedQuery = `
    SELECT
      follow_type,
      raw_input,
      MIN(id) AS follow_id,
      CASE
        WHEN BOOL_OR(match_status = 'pending') THEN 'pending'
        ELSE 'unmatched'
      END AS match_status,
      MAX(match_score) AS match_score,
      COUNT(*)::int AS request_count,
      MIN(created_at) AS created_at
    FROM follow_request
    WHERE ${whereClause}
    GROUP BY follow_type, raw_input
  `;

  // 전체 그룹 개수 조회
  const countResult = await db.query(
    `SELECT COUNT(*)::int AS total
     FROM (${groupedQuery}) AS grouped`,
    values,
  );

  // 페이지 데이터 조회
  const offset = (page - 1) * limit;
  const dataValues = [...values, limit, offset];

  const result = await db.query(
    `SELECT *
     FROM (${groupedQuery}) AS grouped
     ORDER BY request_count DESC, created_at ASC
     LIMIT $${values.length + 1}
     OFFSET $${values.length + 2}`,
    dataValues,
  );

  return {
    data: result.rows,
    total: countResult.rows[0].total,
  };
}

const FOLLOW_TARGET_TABLES = {
  artist: {
    table: "artist",
    column: "artist_id",
  },
  anime: {
    table: "anime",
    column: "anime_id",
  },
};

// 관리자 미매칭 팔로우 수동 매칭
export async function matchAdminFollow({
  followType,
  followId,
  targetId,
  matchStatus,
  applySameInput,
}) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    const target = FOLLOW_TARGET_TABLES[followType];

    // 1. 대표 요청 조회
    const requestResult = await client.query(
      `SELECT id, raw_input, follow_type, match_status
       FROM follow_request
       WHERE id = $1
         AND follow_type = $2
       FOR UPDATE`,
      [followId, followType],
    );

    if (requestResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return { error: "FOLLOW_NOT_FOUND" };
    }

    const request = requestResult.rows[0];

    // 2. 이미 처리된 요청인지 확인
    if (!["pending", "unmatched"].includes(request.match_status)) {
      await client.query("ROLLBACK");
      return { error: "FOLLOW_ALREADY_PROCESSED" };
    }

    // 3. 같은 검색어의 요청을 처리할지 결정
    const scope = applySameInput
      ? {
          sql: "raw_input = $2",
          values: [followId, request.raw_input],
        }
      : {
          sql: "id = $1",
          values: [followId],
        };

    // 4. 대상 항목 존재 여부 확인
    if (matchStatus === "matched") {
      const targetResult = await client.query(
        `SELECT id
         FROM ${target.table}
         WHERE id = $1`,
        [targetId],
      );

      if (targetResult.rows.length === 0) {
        await client.query("ROLLBACK");
        return { error: "TARGET_NOT_FOUND" };
      }
    }

    // 5. 선택된 요청 업데이트
    const targetColumn = target.column;
    const values = applySameInput
      ? [matchStatus, targetId, followId, request.raw_input]
      : [matchStatus, targetId, followId];

    const whereSql = applySameInput
      ? `follow_type = '${followType}'
         AND raw_input = $4
         AND match_status IN ('pending', 'unmatched')`
      : `id = $3
         AND follow_type = '${followType}'
         AND match_status IN ('pending', 'unmatched')`;

    const result = await client.query(
      `UPDATE follow_request
       SET match_status = $1,
           artist_id = CASE
             WHEN '${followType}' = 'artist' THEN $2
             ELSE NULL
           END,
           anime_id = CASE
             WHEN '${followType}' = 'anime' THEN $2
             ELSE NULL
           END
       WHERE ${whereSql}
       RETURNING id`,
      values,
    );

    await client.query("COMMIT");

    return {
      updated_count: result.rowCount,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

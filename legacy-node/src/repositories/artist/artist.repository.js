import db from "../../config/database.js";

export async function findArtistDetail({ artistId }) {
  const result = await db.query(
    `
      SELECT
        a.id AS artist_id,
        a.name_ko,
        a.name_ja,
        a.name_en,
        a.profile_image_url,
        a.description,
        a.debut_date,
        a.official_url,
        a.social_links
      FROM artist a
      WHERE a.id = $1
      LIMIT 1
    `,
    [artistId],
  );

  return result.rows[0] ?? null;
}

export async function findArtistAlbums({ artistId, page, limit }) {
  // 1. 아티스트 존재 여부 확인
  const artistResult = await db.query(
    `
      SELECT id
      FROM artist
      WHERE id = $1
      LIMIT 1
    `,
    [artistId],
  );

  if (artistResult.rows.length === 0) {
    return null;
  }

  // 2. 전체 앨범 개수 조회
  const countResult = await db.query(
    `
      SELECT COUNT(*)::int AS total
      FROM album
      WHERE artist_id = $1
    `,
    [artistId],
  );

  const total = countResult.rows[0].total;
  const offset = (page - 1) * limit;

  // 3. 앨범 목록 조회
  const albumsResult = await db.query(
    `
      SELECT
        a.id AS album_id,
        a.title,
        a.album_type,
        a.release_date,
        a.cover_image_url,
        (
          SELECT COUNT(*)::int
          FROM song s
          WHERE s.album_id = a.id
        ) AS track_count
      FROM album a
      WHERE a.artist_id = $1
      ORDER BY a.release_date DESC NULLS LAST, a.id DESC
      LIMIT $2
      OFFSET $3
    `,
    [artistId, limit, offset],
  );

  return {
    albums: albumsResult.rows,
    total,
  };
}

export async function createArtistFollow({ artistId, userId }) {
  // 1. 아티스트 존재 여부 확인
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

  // 2. 팔로우 등록
  const insertResult = await db.query(
    `INSERT INTO user_artist_follow (
       user_id,
       artist_id
     )
     VALUES ($1, $2)
     ON CONFLICT (user_id, artist_id) DO NOTHING
     RETURNING follow_id, artist_id`,
    [userId, artistId],
  );

  if (insertResult.rows.length > 0) {
    return {
      ...insertResult.rows[0],
      is_followed: true,
      created: true,
    };
  }

  // 3. 이미 팔로우 중이면 기존 데이터 조회
  const existingResult = await db.query(
    `SELECT follow_id, artist_id
     FROM user_artist_follow
     WHERE user_id = $1
       AND artist_id = $2
     LIMIT 1`,
    [userId, artistId],
  );

  return {
    ...existingResult.rows[0],
    is_followed: true,
    created: false,
  };
}

export async function deleteArtistFollow({ artistId, userId }) {
  // 1. 아티스트 존재 여부 확인
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

  // 2. 현재 로그인한 사용자의 팔로우만 삭제
  await db.query(
    `DELETE FROM user_artist_follow
     WHERE user_id = $1
       AND artist_id = $2`,
    [userId, artistId],
  );

  return {
    artist_id: artistId,
    is_followed: false,
  };
}

export async function findArtistSongs({ artistId, page, limit }) {
  // 1. 아티스트 존재 여부 확인
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

  // 2. 해당 아티스트의 전체 곡 수 조회
  const countResult = await db.query(
    `SELECT COUNT(*)::int AS total
     FROM song s
     JOIN album a ON a.id = s.album_id
     WHERE a.artist_id = $1`,
    [artistId],
  );

  const total = countResult.rows[0].total;
  const offset = (page - 1) * limit;

  // 3. 곡 목록 조회
  const songsResult = await db.query(
    `SELECT
       s.id AS song_id,
       s.title,
       s.duration_seconds,
       s.release_date,
       a.id AS album_id,
       a.title AS album_title
     FROM song s
     JOIN album a ON a.id = s.album_id
     WHERE a.artist_id = $1
     ORDER BY s.release_date DESC NULLS LAST, s.id DESC
     LIMIT $2 OFFSET $3`,
    [artistId, limit, offset],
  );

  return {
    songs: songsResult.rows,
    total,
  };
}

import db from "../../config/database.js";
import db from "../config/database.js";

export async function deleteNewsLike(userId, newsId) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // 1. 소식이 존재하는지 확인하고 행을 잠금
    const newsResult = await client.query(
      `
      SELECT id, COALESCE(like_count, 0)::int AS like_count
      FROM news_feed
      WHERE id = $1
      FOR UPDATE
      `,
      [newsId],
    );

    if (newsResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return null;
    }

    // 2. 로그인한 사용자의 좋아요만 삭제
    const deleteResult = await client.query(
      `
      DELETE FROM news_like
      WHERE user_id = $1
        AND news_id = $2
      RETURNING news_id
      `,
      [userId, newsId],
    );

    let likeCount = newsResult.rows[0].like_count;

    // 3. 실제로 삭제된 경우에만 좋아요 수 감소
    if (deleteResult.rowCount > 0) {
      const updateResult = await client.query(
        `
        UPDATE news_feed
        SET like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)
        WHERE id = $1
        RETURNING like_count
        `,
        [newsId],
      );

      likeCount = updateResult.rows[0].like_count;
    }

    await client.query("COMMIT");

    return {
      news_id: newsId,
      like_count: likeCount,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

export async function createNewsLike(userId, newsId) {
  const client = await db.connect();

  try {
    await client.query("BEGIN");

    // 1. 소식이 존재하는지 확인하고 행 잠금
    const newsResult = await client.query(
      `SELECT id, COALESCE(like_count, 0)::int AS like_count
       FROM news_feed
       WHERE id = $1
       FOR UPDATE`,
      [newsId],
    );

    if (newsResult.rows.length === 0) {
      await client.query("ROLLBACK");
      return null;
    }

    // 2. 이미 좋아요를 눌렀는지 확인
    const existingLike = await client.query(
      `SELECT news_id
       FROM news_like
       WHERE user_id = $1 AND news_id = $2`,
      [userId, newsId],
    );

    let created = false;
    let likeCount = newsResult.rows[0].like_count;

    // 3. 아직 좋아요를 누르지 않았다면 등록
    if (existingLike.rowCount === 0) {
      await client.query(
        `INSERT INTO news_like (user_id, news_id)
         VALUES ($1, $2)`,
        [userId, newsId],
      );

      // 4. 좋아요 수 증가
      const updateResult = await client.query(
        `UPDATE news_feed
         SET like_count = COALESCE(like_count, 0) + 1
         WHERE id = $1
         RETURNING like_count`,
        [newsId],
      );

      likeCount = updateResult.rows[0].like_count;
      created = true;
    }

    await client.query("COMMIT");

    return {
      news_id: newsId,
      is_liked: true,
      like_count: likeCount,
      created,
    };
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}

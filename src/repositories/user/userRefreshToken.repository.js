import db from "../../config/database.js";

//새 토큰의 해시 저장
export async function createRefreshToken({ userId, tokenHash, expiresAt }) {
  const result = await db.query(
    `INSERT INTO user_refresh_token (
       user_id,
       token_hash,
       expires_at,
       revoked_at
     )
     VALUES ($1, $2, $3, NULL)
     RETURNING id`,
    [userId, tokenHash, expiresAt],
  );

  return result.rows[0];
}

//아직 만료되거나 폐기되지 않은 토큰 조회
export async function findActiveByHash(tokenHash) {
  const result = await db.query(
    `SELECT
       id,
       user_id,
       token_hash,
       expires_at,
       revoked_at
     FROM user_refresh_token
     WHERE token_hash = $1
       AND revoked_at IS NULL
       AND expires_at > NOW()
     LIMIT 1`,
    [tokenHash],
  );

  return result.rows[0] ?? null;
}

//기존 토큰 폐기
export async function revokeByHash(tokenHash) {
  const result = await db.query(
    `UPDATE user_refresh_token
     SET revoked_at = NOW()
     WHERE token_hash = $1
       AND revoked_at IS NULL
     RETURNING id, user_id`,
    [tokenHash],
  );

  return result.rows[0] ?? null;
}

import db from "../../config/database.js";

export async function findById(id) {
  const result = await db.query(
    `SELECT
       id,
       email,
       nickname,
       profile_image,
       oauth_provider,
       role,
       onboarded_at
     FROM users
     WHERE id = $1
     LIMIT 1`,
    [id],
  );

  return result.rows[0] ?? null;
}

export async function findByEmail(email) {
  const result = await db.query(
    `SELECT
       id,
       email,
       password_hash,
       nickname,
       role,
       onboarded_at
     FROM users
     WHERE email = $1
     LIMIT 1`,
    [email],
  );

  return result.rows[0] ?? null;
}

export async function findByNickname(nickname) {
  const result = await db.query(
    `SELECT
       id,
       email,
       password_hash,
       nickname,
       role,
       onboarded_at
     FROM users
     WHERE nickname = $1
     LIMIT 1`,
    [nickname],
  );

  return result.rows[0] ?? null;
}

export async function createUser({ email, passwordHash, nickname }) {
  const result = await db.query(
    `INSERT INTO users (
       email,
       password_hash,
       nickname,
       role,
       onboarded_at
     )
     VALUES ($1, $2, $3, 'user', NULL)
     RETURNING id, email, nickname, role, onboarded_at`,
    [email, passwordHash, nickname],
  );

  return result.rows[0];
}

export async function findOrCreateOAuthUser({
  provider,
  oauthId,
  email,
  nickname,
  profileImage,
}) {
  const result = await db.query(
    `INSERT INTO users (
       oauth_provider,
       oauth_id,
       email,
       nickname,
       profile_image,
       password_hash,
       role
     )
     VALUES ($1, $2, $3, $4, $5, NULL, 'user')
     ON CONFLICT (oauth_provider, oauth_id)
     DO UPDATE SET oauth_id = EXCLUDED.oauth_id
     RETURNING
       id,
       email,
       nickname,
       profile_image,
       oauth_provider,
       role,
       onboarded_at`,
    [provider, oauthId, email, nickname, profileImage],
  );

  return result.rows[0];
}

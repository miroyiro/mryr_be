import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";
import { randomBytes, createHash } from "node:crypto";
import { oauthProviders } from "../config/oauth.js";
import {
  findById,
  findByEmail,
  findByNickname,
  createUser,
  findOrCreateOAuthUser,
} from "../repositories/user/user.repository.js";
import {
  createRefreshToken,
  findActiveByHash,
  revokeByHash,
} from "../repositories/user/userRefreshToken.repository.js";
import {
  ACCESS_TOKEN_EXPIRES_IN,
  REFRESH_TOKEN_EXPIRES_DAYS,
} from "../constants/auth.constants.js";

function hashToken(token) {
  return createHash("sha256").update(token).digest("hex");
}

function createAccessToken(user) {
  if (!process.env.JWT_ACCESS_SECRET) {
    throw new Error("JWT_ACCESS_SECRET is not configured");
  }

  return jwt.sign(
    {
      role: user.role,
    },
    process.env.JWT_ACCESS_SECRET,
    {
      subject: String(user.id),
      expiresIn: ACCESS_TOKEN_EXPIRES_IN,
    },
  );
}

export async function signup({ email, password, nickname }) {
  const normalizedEmail = email.trim().toLowerCase();
  const normalizedNickname = nickname.trim();

  const existingEmail = await findByEmail(normalizedEmail);

  if (existingEmail) {
    const error = new Error("이미 사용 중인 이메일입니다.");
    error.statusCode = 409;
    throw error;
  }

  const existingNickname = await findByNickname(normalizedNickname);

  if (existingNickname) {
    const error = new Error("이미 사용 중인 닉네임입니다.");
    error.statusCode = 409;
    throw error;
  }

  const passwordHash = await bcrypt.hash(password, 12);

  const user = await createUser({
    email: normalizedEmail,
    passwordHash,
    nickname: normalizedNickname,
  });

  return {
    id: user.id,
    email: user.email,
    nickname: user.nickname,
    role: user.role,
    onboarded: user.onboarded_at !== null,
  };
}

export async function login({ email, password }) {
  const normalizedEmail = email.trim().toLowerCase();

  const user = await findByEmail(normalizedEmail);

  // 계정 존재 여부가 응답으로 드러나지 않도록 동일한 오류를 사용한다.
  if (!user || !user.password_hash) {
    const error = new Error("이메일 또는 비밀번호가 올바르지 않습니다.");
    error.statusCode = 401;
    throw error;
  }

  const passwordMatches = await bcrypt.compare(password, user.password_hash);

  if (!passwordMatches) {
    const error = new Error("이메일 또는 비밀번호가 올바르지 않습니다.");
    error.statusCode = 401;
    throw error;
  }

  const accessToken = createAccessToken(user);

  // 예측하기 어려운 Refresh Token 원문을 생성한다.
  const refreshToken = randomBytes(32).toString("base64url");

  // DB에는 원문이 아닌 SHA-256 해시를 저장한다.
  const tokenHash = hashToken(refreshToken);

  const expiresAt = new Date(
    Date.now() + REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000,
  );

  await createRefreshToken({
    userId: user.id,
    tokenHash,
    expiresAt,
  });

  return {
    accessToken,
    refreshToken,
    user: {
      id: user.id,
      email: user.email,
      nickname: user.nickname,
      role: user.role,
      onboarded: user.onboarded_at !== null,
    },
  };
}

export async function logout({ refreshToken }) {
  // Refresh Token 쿠키가 없는 경우에도 로그아웃을 마무리한다.
  if (!refreshToken) {
    return;
  }

  const tokenHash = hashToken(refreshToken);

  // 현재 Refresh Token만 폐기한다.
  await revokeByHash(tokenHash);
}

export async function getMe(userId) {
  const user = await findById(userId);

  if (!user) {
    const error = new Error("사용자 정보를 찾을 수 없습니다.");
    error.statusCode = 404;
    throw error;
  }

  return {
    id: user.id,
    email: user.email,
    nickname: user.nickname,
    profile_image: user.profile_image,
    oauth_provider: user.oauth_provider,
    role: user.role,
    onboarded: user.onboarded_at !== null,
  };
}

export async function refreshAccessToken(refreshToken) {
  // 1. 쿠키에 리프레시 토큰이 있는지 확인
  if (!refreshToken) {
    const error = new Error("리프레시 토큰이 없습니다.");
    error.statusCode = 401;
    throw error;
  }

  // 2. 토큰 원문이 아닌 해시로 DB 조회
  const tokenHash = hashToken(refreshToken);
  const savedToken = await findActiveByHash(tokenHash);

  if (!savedToken) {
    const error = new Error("유효하지 않거나 만료된 리프레시 토큰입니다.");
    error.statusCode = 401;
    throw error;
  }

  // 3. 토큰 소유자의 회원 정보 조회
  const user = await findById(savedToken.user_id);

  if (!user) {
    const error = new Error("사용자 정보를 찾을 수 없습니다.");
    error.statusCode = 404;
    throw error;
  }

  // 4. 기존 리프레시 토큰 폐기
  const revokedToken = await revokeByHash(tokenHash);

  if (!revokedToken) {
    const error = new Error("리프레시 토큰을 사용할 수 없습니다.");
    error.statusCode = 401;
    throw error;
  }

  // 5. 새로운 리프레시 토큰 생성
  const newRefreshToken = randomBytes(32).toString("base64url");
  const newTokenHash = hashToken(newRefreshToken);

  const expiresAt = new Date(
    Date.now() + REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000,
  );

  await createRefreshToken({
    userId: user.id,
    tokenHash: newTokenHash,
    expiresAt,
  });

  // 6. 새로운 액세스 토큰 발급
  const accessToken = createAccessToken(user);

  return {
    accessToken,
    refreshToken: newRefreshToken,
  };
}

export async function completeOAuthLogin({ provider, code, state }) {
  const config = oauthProviders[provider];

  if (!config || !code) {
    const error = new Error("OAuth 요청이 올바르지 않습니다.");
    error.statusCode = 400;
    throw error;
  }

  // 1. 인가 코드를 플랫폼 액세스 토큰으로 교환
  const tokenParams = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: config.clientId,
    redirect_uri: config.redirectUri,
    code,
  });

  if (provider === "naver") {
    tokenParams.set("client_secret", config.clientSecret);
  } else if (config.clientSecret) {
    tokenParams.set("client_secret", config.clientSecret);
  }

  const tokenResponse = await fetch(config.tokenUrl, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: tokenParams,
  });

  const tokenData = await tokenResponse.json();

  if (!tokenResponse.ok || !tokenData.access_token) {
    const error = new Error("OAuth 토큰 발급에 실패했습니다.");
    error.statusCode = 401;
    throw error;
  }

  // 2. 플랫폼에서 사용자 정보 조회
  const profileResponse = await fetch(config.userInfoUrl, {
    headers: {
      Authorization: `Bearer ${tokenData.access_token}`,
    },
  });

  const profileData = await profileResponse.json();

  if (!profileResponse.ok) {
    const error = new Error("OAuth 사용자 조회에 실패했습니다.");
    error.statusCode = 401;
    throw error;
  }

  // 3. 플랫폼별 응답 형식을 공통 형식으로 변환
  let profile;

  if (provider === "kakao") {
    const account = profileData.kakao_account ?? {};
    const kakaoProfile = account.profile ?? {};

    profile = {
      oauthId: String(profileData.id),
      email: account.email ?? null,
      nickname: kakaoProfile.nickname ?? null,
      profileImage: kakaoProfile.profile_image_url ?? null,
    };
  } else if (provider === "naver") {
    const naverProfile = profileData.response;

    if (!naverProfile?.id) {
      const error = new Error("네이버 사용자 정보가 올바르지 않습니다.");
      error.statusCode = 401;
      throw error;
    }

    profile = {
      oauthId: String(naverProfile.id),
      email: naverProfile.email ?? null,
      nickname: naverProfile.nickname ?? null,
      profileImage: naverProfile.profile_image ?? null,
    };
  } else {
    profile = {
      oauthId: String(profileData.sub ?? ""),
      email: profileData.email ?? null,
      nickname: profileData.name ?? null,
      profileImage: profileData.picture ?? null,
    };
  }

  if (!profile.oauthId) {
    const error = new Error("OAuth 사용자 ID를 확인할 수 없습니다.");
    error.statusCode = 401;
    throw error;
  }

  // 4. 기존 회원 조회 또는 신규 회원 생성
  const user = await findOrCreateOAuthUser({
    provider,
    oauthId: profile.oauthId,
    email: profile.email,
    nickname:
      profile.nickname?.trim() || `${provider}_${profile.oauthId.slice(-8)}`,
    profileImage: profile.profileImage,
  });

  // 5. 우리 서비스의 Refresh Token 발급
  const refreshToken = randomBytes(32).toString("base64url");
  const tokenHash = hashToken(refreshToken);

  const expiresAt = new Date(
    Date.now() + REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000,
  );

  await createRefreshToken({
    userId: user.id,
    tokenHash,
    expiresAt,
  });

  return { refreshToken };
}

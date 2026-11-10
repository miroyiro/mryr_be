import {
  signup,
  login,
  logout,
  getMe,
  refreshAccessToken,
} from "../services/auth.service.js";
import { validateSignup } from "../validators/auth.validator.js";
import { randomBytes } from "node:crypto";
import { oauthProviders } from "../config/oauth.js";
import { completeOAuthLogin } from "../services/auth.service.js";
import {
  REFRESH_TOKEN_COOKIE_NAME,
  REFRESH_TOKEN_EXPIRES_DAYS,
} from "../constants/auth.constants.js";

export async function signupController(req, res, next) {
  try {
    const errors = validateSignup(req.body);

    if (Object.keys(errors).length > 0) {
      return res.status(400).json({
        error: {
          code: "VALIDATION_ERROR",
          message: "입력값을 확인해 주세요.",
          details: errors,
        },
      });
    }

    const user = await signup(req.body);

    return res.status(201).json({
      data: {
        user: {
          ...user,
          onboarded: false,
        },
      },
    });
  } catch (error) {
    next(error);
  }
}

export async function loginController(req, res, next) {
  try {
    const { email, password } = req.body ?? {};

    if (
      typeof email !== "string" ||
      !email.trim() ||
      typeof password !== "string" ||
      !password
    ) {
      return res.status(400).json({
        error: {
          code: "VALIDATION_ERROR",
          message: "이메일과 비밀번호를 입력해 주세요.",
        },
      });
    }

    const result = await login({ email, password });

    const isProduction = process.env.NODE_ENV === "production";

    res.cookie(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken, {
      httpOnly: true,
      secure: isProduction,
      sameSite: "lax",
      path: "/api/auth",
      maxAge: REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000,
    });

    return res.status(200).json({
      data: {
        access_token: result.accessToken,
        expires_in: 1800,
        user: result.user,
      },
    });
  } catch (error) {
    next(error);
  }
}

export async function logoutController(req, res, next) {
  try {
    const refreshToken = req.cookies?.[REFRESH_TOKEN_COOKIE_NAME];

    await logout(refreshToken);

    // 로그인할 때 설정한 쿠키 옵션과 경로를 일치시킨다.
    res.clearCookie(REFRESH_TOKEN_COOKIE_NAME, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/api/auth",
    });

    return res.status(200).json({
      data: {
        logged_out: true,
      },
    });
  } catch (error) {
    next(error);
  }
}

export async function meController(req, res, next) {
  try {
    const user = await getMe(req.user.id);

    return res.status(200).json({
      data: user,
    });
  } catch (error) {
    next(error);
  }
}

export async function refreshController(req, res, next) {
  try {
    const refreshToken = req.cookies?.[REFRESH_TOKEN_COOKIE_NAME];

    const result = await refreshAccessToken(refreshToken);

    const isProduction = process.env.NODE_ENV === "production";

    // 새 리프레시 토큰을 쿠키에 저장
    res.cookie(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken, {
      httpOnly: true,
      secure: isProduction,
      sameSite: "lax",
      path: "/api/auth",
      maxAge: REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000,
    });

    // 새 액세스 토큰 반환
    return res.status(200).json({
      data: {
        access_token: result.accessToken,
        expires_in: 1800,
      },
    });
  } catch (error) {
    next(error);
  }
}

export function oauthStartController(req, res, next) {
  try {
    const { provider } = req.params;
    const config = oauthProviders[provider];

    if (
      !config ||
      !config.clientId ||
      !config.clientSecret ||
      !config.redirectUri
    ) {
      return res.status(400).json({
        error: {
          code: "UNSUPPORTED_OAUTH_PROVIDER",
          message: "지원하지 않거나 설정되지 않은 로그인 플랫폼입니다.",
        },
      });
    }

    // CSRF 방지용 임의 state 생성
    const state = randomBytes(32).toString("hex");

    res.cookie("oauth_state", state, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/api/auth",
      maxAge: 10 * 60 * 1000,
    });

    const url = new URL(config.authorizeUrl);

    url.searchParams.set("client_id", config.clientId);
    url.searchParams.set("redirect_uri", config.redirectUri);
    url.searchParams.set("response_type", "code");
    url.searchParams.set("state", state);

    if (config.scope) {
      url.searchParams.set("scope", config.scope);
    }

    return res.redirect(302, url.toString());
  } catch (error) {
    next(error);
  }
}

export async function oauthCallbackController(req, res, next) {
  const frontendUrl = process.env.FRONTEND_URL;

  try {
    const { provider } = req.params;
    const config = oauthProviders[provider];

    if (!config) {
      return res.status(400).json({
        error: {
          code: "UNSUPPORTED_OAUTH_PROVIDER",
          message: "지원하지 않는 로그인 플랫폼입니다.",
        },
      });
    }

    const { code, state, error: oauthError } = req.query;
    const savedState = req.cookies?.oauth_state;

    // state 검증 후 쿠키 제거
    res.clearCookie("oauth_state", {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      path: "/api/auth",
    });

    if (
      typeof state !== "string" ||
      typeof savedState !== "string" ||
      state !== savedState
    ) {
      return res.redirect(
        302,
        `${frontendUrl}/login?error=invalid_oauth_state`,
      );
    }

    if (oauthError || typeof code !== "string" || !code) {
      return res.redirect(302, `${frontendUrl}/login?error=oauth_cancelled`);
    }

    const result = await completeOAuthLogin({
      provider,
      code,
      state,
    });

    const isProduction = process.env.NODE_ENV === "production";

    res.cookie(REFRESH_TOKEN_COOKIE_NAME, result.refreshToken, {
      httpOnly: true,
      secure: isProduction,
      sameSite: "lax",
      path: "/api/auth",
      maxAge: REFRESH_TOKEN_EXPIRES_DAYS * 24 * 60 * 60 * 1000,
    });

    return res.redirect(302, `${frontendUrl}/auth/done`);
  } catch (error) {
    // 서버 로그에는 내부 오류를 남기되 민감한 정보를 URL에 노출하지 않음
    console.error("OAuth callback failed:", error.message);

    return res.redirect(302, `${frontendUrl}/login?error=oauth_failed`);
  }
}

import jwt from "jsonwebtoken";

export function authMiddleware(req, res, next) {
  const authorization = req.headers.authorization;

  if (!authorization || !authorization.startsWith("Bearer ")) {
    return res.status(401).json({
      error: {
        code: "UNAUTHORIZED",
        message: "로그인이 필요합니다.",
      },
    });
  }

  const accessToken = authorization.slice("Bearer ".length);

  try {
    const decoded = jwt.verify(accessToken, process.env.JWT_ACCESS_SECRET);

    req.user = {
      id: decoded.sub,
      role: decoded.role,
    };

    next();
  } catch (error) {
    return res.status(401).json({
      error: {
        code: "INVALID_ACCESS_TOKEN",
        message: "유효하지 않거나 만료된 인증 토큰입니다.",
      },
    });
  }
}

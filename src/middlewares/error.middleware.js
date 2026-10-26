export function errorMiddleware(err, req, res, next) {
  if (res.headersSent) {
    return next(err);
  }

  const statusCode = err.statusCode || 500;

  if (statusCode >= 500) {
    console.error(err);
  }

  const messages = {
    400: "요청값이 올바르지 않습니다.",
    401: "이메일 또는 비밀번호가 올바르지 않습니다.",
    409: "이미 사용 중인 값입니다.",
    500: "서버 내부 오류가 발생했습니다.",
  };

  return res.status(statusCode).json({
    error: {
      code:
        statusCode === 401
          ? "INVALID_CREDENTIALS"
          : statusCode === 409
            ? "CONFLICT"
            : statusCode === 400
              ? "VALIDATION_ERROR"
              : "INTERNAL_SERVER_ERROR",
      message: messages[statusCode] || "요청을 처리할 수 없습니다.",
    },
  });
}

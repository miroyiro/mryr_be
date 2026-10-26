export function validateSignup({ email, password, nickname }) {
  const errors = {};

  if (typeof email !== "string" || !email.trim()) {
    errors.email = "이메일을 입력해 주세요.";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
    errors.email = "올바른 이메일 형식이 아닙니다.";
  }

  if (typeof password !== "string" || password.length < 8) {
    errors.password = "비밀번호는 8자 이상이어야 합니다.";
  }

  if (
    typeof nickname !== "string" ||
    !nickname.trim() ||
    nickname.trim().length > 30
  ) {
    errors.nickname = "닉네임은 1~30자로 입력해 주세요.";
  }

  return errors;
}

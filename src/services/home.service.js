import {
  findHomeFeedByUserId,
  findGuestHomeFeed,
  findPopularHomeFeed,
  saveOnboardingFollows,
} from "../repositories/home.repository.js";

export async function getHomeFeed(userId, limit = 5) {
  if (!userId) {
    const error = new Error("로그인이 필요합니다.");
    error.statusCode = 401;
    throw error;
  }

  return await findHomeFeedByUserId(userId, limit);
}

export async function getGuestHomeFeed() {
  return await findGuestHomeFeed();
}

export async function getPopularHomeFeed({ category, limit = 5 }) {
  return await findPopularHomeFeed({ category, limit });
}

export async function saveOnboardingFollow(userId, follows) {
  const normalizedFollows = follows.map((item) => {
    if (!item || typeof item !== "object") {
      const error = new Error("관심사 항목 형식이 올바르지 않습니다.");
      error.statusCode = 400;
      throw error;
    }

    const { type, id, name } = item;

    if (!["artist", "anime"].includes(type)) {
      const error = new Error("type은 artist 또는 anime여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (typeof name !== "string" || !name.trim()) {
      const error = new Error("관심사 이름은 필수입니다.");
      error.statusCode = 400;
      throw error;
    }

    if (id !== null && (!Number.isInteger(id) || id <= 0)) {
      const error = new Error("id는 양의 정수 또는 null이어야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    return {
      type,
      id,
      name: name.trim(),
    };
  });

  // 같은 항목이 중복 제출되지 않도록 입력 단계에서 제거
  const uniqueFollows = [
    ...new Map(
      normalizedFollows.map((item) => [
        `${item.type}:${item.id ?? item.name.toLowerCase()}`,
        item,
      ]),
    ).values(),
  ];

  return saveOnboardingFollows(userId, uniqueFollows);
}

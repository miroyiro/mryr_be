import {
  getHomeFeed,
  getGuestHomeFeed,
  getPopularHomeFeed,
  saveOnboardingFollow,
} from "../services/home.service.js";

export async function getHomeFeedController(req, res, next) {
  try {
    // authMiddleware가 설정한 로그인 사용자 ID
    const userId = req.user.id;

    // limit이 없으면 기본 5개
    const requestedLimit = Number(req.query.limit ?? 5);

    // 잘못된 값은 기본값으로 처리하고, 최대 20개로 제한
    const limit =
      Number.isInteger(requestedLimit) && requestedLimit > 0
        ? Math.min(requestedLimit, 20)
        : 5;

    const data = await getHomeFeed(userId, limit);

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getGuestHomeFeedController(req, res, next) {
  try {
    const data = await getGuestHomeFeed();

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getPopularHomeFeedController(req, res, next) {
  try {
    const { category } = req.query;
    const requestedLimit = Number(req.query.limit ?? 5);

    // category는 생략하거나 jpop, anime 중 하나여야 함
    if (category !== undefined && !["jpop", "anime"].includes(category)) {
      const error = new Error("category는 jpop 또는 anime여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    // limit은 양의 정수, 최대 20개
    if (!Number.isInteger(requestedLimit) || requestedLimit < 1) {
      const error = new Error("limit은 1 이상의 정수여야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    const limit = Math.min(requestedLimit, 20);

    const data = await getPopularHomeFeed({
      category: category ?? null,
      limit,
    });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function saveOnboardingFollowController(req, res, next) {
  try {
    const { follows } = req.body;

    if (!Array.isArray(follows) || follows.length === 0) {
      const error = new Error("follows는 비어 있지 않은 배열이어야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    if (follows.length > 50) {
      const error = new Error("한 번에 최대 50개까지 저장할 수 있습니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await saveOnboardingFollow(req.user.id, follows);

    return res.status(201).json({ data });
  } catch (error) {
    next(error);
  }
}

import {
  searchAll,
  getPopularKeywords,
  getRecentSearches,
  deleteRecentSearch,
} from "../services/search.service.js";

export async function searchController(req, res, next) {
  try {
    const q = typeof req.query.q === "string" ? req.query.q.trim() : "";

    const type = req.query.type;
    const rawLimit = req.query.limit;

    // 검색어 검증
    if (q.length < 2) {
      const error = new Error("검색어는 2자 이상 입력해야 합니다.");
      error.statusCode = 400;
      throw error;
    }

    // type 검증
    const allowedTypes = ["artist", "anime", "event"];

    if (
      type !== undefined &&
      (typeof type !== "string" || !allowedTypes.includes(type))
    ) {
      const error = new Error(
        "type은 artist, anime, event 중 하나여야 합니다.",
      );
      error.statusCode = 400;
      throw error;
    }

    // limit 검증
    let limit = 10;

    if (rawLimit !== undefined) {
      if (
        typeof rawLimit !== "string" ||
        !/^\d+$/.test(rawLimit) ||
        Number(rawLimit) < 1 ||
        Number(rawLimit) > 50
      ) {
        const error = new Error("limit은 1부터 50 사이의 정수여야 합니다.");
        error.statusCode = 400;
        throw error;
      }

      limit = Number(rawLimit);
    }

    const data = await searchAll({ q, type, limit });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function popularSearchController(req, res, next) {
  try {
    const rawLimit = req.query.limit;
    let limit = 10;

    if (rawLimit !== undefined) {
      if (
        typeof rawLimit !== "string" ||
        !/^\d+$/.test(rawLimit) ||
        Number(rawLimit) < 1 ||
        Number(rawLimit) > 50
      ) {
        const error = new Error("limit은 1부터 50 사이의 정수여야 합니다.");
        error.statusCode = 400;
        throw error;
      }

      limit = Number(rawLimit);
    }

    const result = await getPopularKeywords({ limit });

    return res.status(200).json(result);
  } catch (error) {
    next(error);
  }
}

export async function recentSearchController(req, res, next) {
  try {
    const rawLimit = req.query.limit;
    let limit = 10;

    if (rawLimit !== undefined) {
      if (
        typeof rawLimit !== "string" ||
        !/^\d+$/.test(rawLimit) ||
        Number(rawLimit) < 1 ||
        Number(rawLimit) > 50
      ) {
        const error = new Error("limit은 1부터 50 사이의 정수여야 합니다.");
        error.statusCode = 400;
        throw error;
      }

      limit = Number(rawLimit);
    }

    const userId = req.user.id;
    const data = await getRecentSearches({ userId, limit });

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function deleteRecentSearchController(req, res, next) {
  try {
    const searchHistoryId = Number(req.params.id);

    if (!Number.isSafeInteger(searchHistoryId) || searchHistoryId <= 0) {
      const error = new Error("검색 기록 ID가 올바르지 않습니다.");
      error.statusCode = 400;
      throw error;
    }

    const userId = req.user.id;

    const result = await deleteRecentSearch({
      userId,
      searchHistoryId,
    });

    return res.status(200).json({
      data: result,
    });
  } catch (error) {
    next(error);
  }
}

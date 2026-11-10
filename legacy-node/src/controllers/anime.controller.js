import {
  getAnimeList,
  getAnimeDetail,
  getAnimeEpisodes,
  followAnime,
  unfollowAnime,
  createAnimeReview,
  getAnimeReviews,
  updateAnimeReview,
  deleteAnimeReview,
  getAnimeStreaming,
} from "../services/anime.service.js";

const VALID_SEASONS = ["winter", "spring", "summer", "fall"];

const VALID_STATUSES = ["releasing", "finished", "not_yet_released"];

const VALID_SORTS = ["popular", "latest", "title"];

export async function getAnimeListController(req, res, next) {
  try {
    const {
      season,
      seasonYear,
      genre,
      status,
      weekday,
      sort = "popular",
    } = req.query;

    const page = req.query.page === undefined ? 1 : Number(req.query.page);

    const limit = req.query.limit === undefined ? 20 : Number(req.query.limit);

    if (!Number.isInteger(page) || page < 1) {
      return res.status(400).json({
        message: "page는 1 이상의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      return res.status(400).json({
        message: "limit은 1~50 사이의 정수여야 합니다.",
      });
    }

    if (season !== undefined && !VALID_SEASONS.includes(season)) {
      return res.status(400).json({
        message: "season은 winter, spring, summer, fall 중 하나여야 합니다.",
      });
    }

    let parsedSeasonYear;

    if (seasonYear !== undefined) {
      parsedSeasonYear = Number(seasonYear);

      if (
        !Number.isInteger(parsedSeasonYear) ||
        parsedSeasonYear < 1900 ||
        parsedSeasonYear > 2100
      ) {
        return res.status(400).json({
          message: "seasonYear는 1900~2100 사이의 정수여야 합니다.",
        });
      }
    }

    if (genre !== undefined && (typeof genre !== "string" || !genre.trim())) {
      return res.status(400).json({
        message: "genre는 비어 있지 않은 문자열이어야 합니다.",
      });
    }

    if (status !== undefined && !VALID_STATUSES.includes(status)) {
      return res.status(400).json({
        message: "status 값이 올바르지 않습니다.",
      });
    }

    let parsedWeekday;

    if (weekday !== undefined) {
      parsedWeekday = Number(weekday);

      if (
        !Number.isInteger(parsedWeekday) ||
        parsedWeekday < 0 ||
        parsedWeekday > 6
      ) {
        return res.status(400).json({
          message: "weekday는 0~6 사이의 정수여야 합니다.",
        });
      }
    }

    if (!VALID_SORTS.includes(sort)) {
      return res.status(400).json({
        message: "sort는 popular, latest, title 중 하나여야 합니다.",
      });
    }

    const result = await getAnimeList({
      season,
      seasonYear: parsedSeasonYear,
      genre: genre?.trim(),
      status,
      weekday: parsedWeekday,
      sort,
      page,
      limit,
    });

    return res.status(200).json({
      data: result.data,
      meta: {
        page,
        limit,
        total: result.total,
      },
    });
  } catch (error) {
    next(error);
  }
}

export async function getAnimeDetailController(req, res, next) {
  try {
    const animeId = Number(req.params.id);

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    const data = await getAnimeDetail({
      animeId,
      userId: req.user?.id ?? null,
    });

    if (!data) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function getAnimeEpisodesController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const order = req.query.order ?? "asc";

    // 애니 ID 검증
    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    // 정렬값 검증
    if (!["asc", "desc"].includes(order)) {
      return res.status(400).json({
        message: "order는 asc 또는 desc여야 합니다.",
      });
    }

    const data = await getAnimeEpisodes({
      animeId,
      order,
    });

    // 작품 자체가 존재하지 않는 경우
    if (data === null) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}
export async function followAnimeController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const userId = req.user.id;

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    const result = await followAnime({
      animeId,
      userId,
    });

    if (result === null) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    const { data, created } = result;

    return res.status(created ? 201 : 200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function unfollowAnimeController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const userId = req.user.id;

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    const data = await unfollowAnime({
      animeId,
      userId,
    });

    if (data === null) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function createAnimeReviewController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const userId = req.user.id;
    const { rating, content } = req.body ?? {};

    // 애니 ID 검증
    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    // 평점 검증: 1~5 사이의 정수
    if (!Number.isInteger(rating) || rating < 1 || rating > 5) {
      return res.status(400).json({
        message: "평점은 1부터 5 사이의 정수여야 합니다.",
      });
    }

    // 리뷰 내용 검증
    if (
      typeof content !== "string" ||
      content.trim().length === 0 ||
      content.trim().length > 2000
    ) {
      return res.status(400).json({
        message: "리뷰 내용은 1~2000자여야 합니다.",
      });
    }

    const result = await createAnimeReview({
      animeId,
      userId,
      rating,
      content: content.trim(),
    });

    if (result === null) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    if (result.conflict) {
      return res.status(409).json({
        message: "이미 해당 작품에 리뷰를 작성했습니다.",
      });
    }

    return res.status(201).json({
      data: result.data,
    });
  } catch (error) {
    next(error);
  }
}

export async function getAnimeReviewsController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const page = Number(req.query.page ?? 1);
    const limit = Number(req.query.limit ?? 20);

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(page) || page < 1) {
      return res.status(400).json({
        message: "page는 1 이상의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(limit) || limit < 1 || limit > 50) {
      return res.status(400).json({
        message: "limit는 1부터 50 사이의 정수여야 합니다.",
      });
    }

    const result = await getAnimeReviews({
      animeId,
      page,
      limit,
    });

    if (result === null) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({
      data: result.reviews,
      pagination: {
        page,
        limit,
        total: result.total,
        total_pages: Math.ceil(result.total / limit),
      },
    });
  } catch (error) {
    next(error);
  }
}

export async function updateAnimeReviewController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const reviewId = Number(req.params.reviewId);
    const userId = req.user.id;
    const body = req.body ?? {};

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(reviewId) || reviewId < 1) {
      return res.status(400).json({
        message: "리뷰 ID는 양의 정수여야 합니다.",
      });
    }

    // 허용된 필드만 수정
    const allowedFields = ["rating", "content"];
    const keys = Object.keys(body);

    if (keys.length === 0 || keys.some((key) => !allowedFields.includes(key))) {
      return res.status(400).json({
        message: "rating 또는 content 중 하나 이상을 전달해야 합니다.",
      });
    }

    if (
      Object.hasOwn(body, "rating") &&
      (!Number.isInteger(body.rating) || body.rating < 1 || body.rating > 5)
    ) {
      return res.status(400).json({
        message: "평점은 1부터 5 사이의 정수여야 합니다.",
      });
    }

    if (
      Object.hasOwn(body, "content") &&
      (typeof body.content !== "string" ||
        body.content.trim().length === 0 ||
        body.content.trim().length > 2000)
    ) {
      return res.status(400).json({
        message: "리뷰 내용은 1~2000자여야 합니다.",
      });
    }

    const result = await updateAnimeReview({
      animeId,
      reviewId,
      userId,
      rating: body.rating,
      content: Object.hasOwn(body, "content") ? body.content.trim() : undefined,
    });

    if (result === null) {
      return res.status(404).json({
        message: "애니 작품 또는 리뷰를 찾을 수 없습니다.",
      });
    }

    if (result.forbidden) {
      return res.status(403).json({
        message: "본인이 작성한 리뷰만 수정할 수 있습니다.",
      });
    }

    return res.status(200).json({
      data: result,
    });
  } catch (error) {
    next(error);
  }
}

export async function deleteAnimeReviewController(req, res, next) {
  try {
    const animeId = Number(req.params.id);
    const reviewId = Number(req.params.reviewId);
    const userId = req.user.id;

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    if (!Number.isInteger(reviewId) || reviewId < 1) {
      return res.status(400).json({
        message: "리뷰 ID는 양의 정수여야 합니다.",
      });
    }

    const result = await deleteAnimeReview({
      animeId,
      reviewId,
      userId,
    });

    if (result === null) {
      return res.status(404).json({
        message: "애니 작품 또는 리뷰를 찾을 수 없습니다.",
      });
    }

    if (result.forbidden) {
      return res.status(403).json({
        message: "본인이 작성한 리뷰만 삭제할 수 있습니다.",
      });
    }

    return res.status(200).json({
      data: result,
    });
  } catch (error) {
    next(error);
  }
}

export async function getAnimeStreamingController(req, res, next) {
  try {
    const animeId = Number(req.params.id);

    if (!Number.isInteger(animeId) || animeId < 1) {
      return res.status(400).json({
        message: "애니 ID는 양의 정수여야 합니다.",
      });
    }

    const data = await getAnimeStreaming({ animeId });

    if (data === null) {
      return res.status(404).json({
        message: "애니 작품을 찾을 수 없습니다.",
      });
    }

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

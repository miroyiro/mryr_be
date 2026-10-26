import { likeNews, unlikeNews } from "../services/news.service.js";

export async function unlikeNewsController(req, res, next) {
  try {
    const newsId = Number(req.params.id);
    const userId = req.user.id;

    if (!Number.isInteger(newsId) || newsId <= 0) {
      const error = new Error("소식 ID가 올바르지 않습니다.");
      error.statusCode = 400;
      throw error;
    }

    const data = await unlikeNews(userId, newsId);

    return res.status(200).json({ data });
  } catch (error) {
    next(error);
  }
}

export async function likeNewsController(req, res, next) {
  try {
    const newsId = Number(req.params.id);
    const userId = req.user.id;

    // 소식 ID 검증
    if (!Number.isInteger(newsId) || newsId <= 0) {
      const error = new Error("소식 ID가 올바르지 않습니다.");
      error.statusCode = 400;
      throw error;
    }

    const result = await likeNews(userId, newsId);

    return res.status(result.created ? 201 : 200).json({
      data: {
        news_id: result.news_id,
        is_liked: true,
        like_count: result.like_count,
      },
    });
  } catch (error) {
    next(error);
  }
}

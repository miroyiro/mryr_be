import {
  createNewsLike,
  deleteNewsLike,
} from "../repositories/news.repository.js";

export async function unlikeNews(userId, newsId) {
  const result = await deleteNewsLike(userId, newsId);

  if (!result) {
    const error = new Error("해당 소식을 찾을 수 없습니다.");
    error.statusCode = 404;
    throw error;
  }

  return {
    news_id: result.news_id,
    is_liked: false,
    like_count: result.like_count,
  };
}

export async function likeNews(userId, newsId) {
  const result = await createNewsLike(userId, newsId);

  if (!result) {
    const error = new Error("해당 소식을 찾을 수 없습니다.");
    error.statusCode = 404;
    throw error;
  }

  return result;
}

// 기존 좋아요 취소 서비스
export async function unlikeNews(userId, newsId) {
  const result = await deleteNewsLike(userId, newsId);

  if (!result) {
    const error = new Error("해당 소식을 찾을 수 없습니다.");
    error.statusCode = 404;
    throw error;
  }

  return {
    news_id: result.news_id,
    is_liked: false,
    like_count: result.like_count,
  };
}

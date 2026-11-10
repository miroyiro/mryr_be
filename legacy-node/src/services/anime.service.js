import {
  findAnimeList,
  findAnimeDetail,
  findAnimeEpisodes,
  createAnimeFollow,
  deleteAnimeFollow,
  insertAnimeReview,
  findAnimeReviews,
  updateAnimeReviewById,
  deleteAnimeReviewById,
  findAnimeStreaming,
} from "../repositories/anime.repository.js";

export async function getAnimeList(filters) {
  return findAnimeList(filters);
}

export async function getAnimeDetail({ animeId, userId }) {
  return findAnimeDetail({ animeId, userId });
}

export async function getAnimeEpisodes({ animeId, order }) {
  return findAnimeEpisodes({ animeId, order });
}

export async function followAnime({ animeId, userId }) {
  return createAnimeFollow({ animeId, userId });
}

export async function unfollowAnime({ animeId, userId }) {
  return deleteAnimeFollow({ animeId, userId });
}

export async function createAnimeReview({ animeId, userId, rating, content }) {
  return insertAnimeReview({
    animeId,
    userId,
    rating,
    content,
  });
}

export async function getAnimeReviews({ animeId, page, limit }) {
  return findAnimeReviews({ animeId, page, limit });
}

export async function updateAnimeReview({
  animeId,
  reviewId,
  userId,
  rating,
  content,
}) {
  return updateAnimeReviewById({
    animeId,
    reviewId,
    userId,
    rating,
    content,
  });
}

export async function deleteAnimeReview({ animeId, reviewId, userId }) {
  return deleteAnimeReviewById({
    animeId,
    reviewId,
    userId,
  });
}

export async function getAnimeStreaming({ animeId }) {
  return findAnimeStreaming({ animeId });
}

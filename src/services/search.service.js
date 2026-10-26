import {
  findSearchResults,
  findPopularKeywords,
  findRecentSearches,
  hideRecentSearch,
} from "../repositories/search/popularKeyword.repository.js";

export async function searchAll({ q, type, limit }) {
  return await findSearchResults({ q, type, limit });
}

export async function getPopularKeywords({ limit }) {
  return await findPopularKeywords(limit);
}

export async function getRecentSearches({ userId, limit }) {
  return await findRecentSearches(userId, limit);
}

export async function deleteRecentSearch({ userId, searchHistoryId }) {
  const deleted = await hideRecentSearch(userId, searchHistoryId);

  if (!deleted) {
    const error = new Error("검색 기록을 찾을 수 없습니다.");
    error.statusCode = 404;
    throw error;
  }

  return {
    id: deleted.id,
    deleted: true,
  };
}
3

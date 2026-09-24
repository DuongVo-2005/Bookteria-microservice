import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

export const searchBooks = async (q, categoryId, authorId, page, size, sort) => {
  return await httpClient.get(API.SEARCH_BOOKS, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    params: {
      q,
      categoryId,
      authorId,
      sort,
      page: Math.max(0, page),
      size: Math.max(1, size),
    },
  });
};

// GET /search/books/suggest?q=&limit= — trả List<String> (từ khoá gợi ý theo
// tiêu đề khớp tiền tố), KHÔNG phải kết quả sách đầy đủ như searchBooks.
export const suggestBooks = async (q, limit = 8) => {
  return await httpClient.get(API.SEARCH_BOOKS_SUGGEST, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    params: { q, limit },
  });
};

// §22.1 (be-report.md, 2026-09-18) — chỉ nhận 1 trong 2 (q HOẶC hashtag),
// không cả 2 cùng lúc (đúng contract BE). Chỉ post PUBLIC được index nên kết
// quả không bao giờ lộ bài FRIENDS/PRIVATE, kể cả của chính viewer.
export const searchPosts = async (q, hashtag, page = 0, size = 10) => {
  return await httpClient.get(API.SEARCH_POSTS, {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { q, hashtag, page, size },
  });
};

export const getTrendingHashtags = async (limit = 10) => {
  return await httpClient.get(API.SEARCH_HASHTAGS_TRENDING, {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { limit },
  });
};

// Chỉ group PUBLIC được index.
export const searchGroups = async (q, category, page = 0, size = 10) => {
  return await httpClient.get(API.SEARCH_GROUPS, {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { q, category, page, size },
  });
};

// ADMIN only — công cụ vận hành, đồng bộ lại index Elasticsearch từ dữ liệu
// gốc (post-service/group-service).
export const reindexPosts = async () => {
  return await httpClient.post(API.SEARCH_REINDEX_POSTS, {}, { headers: { Authorization: `Bearer ${getToken()}` } });
};

export const reindexGroups = async () => {
  return await httpClient.post(API.SEARCH_REINDEX_GROUPS, {}, { headers: { Authorization: `Bearer ${getToken()}` } });
};

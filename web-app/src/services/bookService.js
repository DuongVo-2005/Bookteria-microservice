import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

export const getBooks = async (page, size, categoryId, authorId) => {
  return await httpClient.get(API.BOOKS, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    params: {
      page: page,
      size: size,
      categoryId: categoryId,
      authorId: authorId,
    },
  });
};

// Phase 6 (be-report.md, 2026-09-18): sắp xếp theo điểm trending giảm dần,
// không nhận filter category/author (khác getBooks).
export const getTrendingBooks = async (page, size) => {
  return await httpClient.get(`${API.BOOKS}/trending`, {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { page, size },
  });
};

// Tối đa 10 sách, không phân trang (đúng contract be-report.md).
export const getMyRecommendations = async () => {
  return await httpClient.get(API.RECOMMENDATIONS, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

export const getBookById = async (bookId) => {
  return await httpClient.get(`${API.BOOK_DETAIL}/${bookId}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const getBookBySlug = async (slug) => {
  return await httpClient.get(`${API.BOOK_BY_SLUG}/${slug}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const getBooksByCategory = async (categoryId) => {
  return await httpClient.get(`${API.BOOK_BY_CATEGORY}/${categoryId}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const getBooksByAuthor = async (authorId) => {
  return await httpClient.get(`${API.BOOK_BY_AUTHOR}/${authorId}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const getAuthors = async () => {
  return await httpClient.get(API.AUTHORS, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const getCategories = async () => {
  return await httpClient.get(API.CATEGORIES, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const getPublishers = async () => {
  return await httpClient.get(API.PUBLISHERS, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

export const createBook = async (bookData) => {
  return await httpClient.post(API.BOOKS, bookData, {
    headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" },
  });
};

export const updateBook = async (bookId, bookData) => {
  return await httpClient.put(`${API.BOOK_DETAIL}/${bookId}`, bookData, {
    headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" },
  });
};

export const deleteBook = async (bookId) => {
  return await httpClient.delete(`${API.BOOK_DETAIL}/${bookId}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
};

// BA Backlog OPS-02 (be-report.md, bổ sung 2026-09-19): ADMIN gộp sách trùng
// — bookId là sách GIỮ LẠI, duplicateBookId bị xoá sau khi chuyển Review/
// ReadingList sang sách gốc. Đã publish BOOK_MERGED nên GroupPost.bookRef
// tự re-point qua Kafka (vài giây), nhưng nội dung Post (post-service) chứa
// bookId dạng text tự do (nếu có) sẽ KHÔNG tự cập nhật (BE tự ghi trong FE Contract).
export const mergeBooks = async (bookId, duplicateBookId) => {
  return await httpClient.post(
    `${API.BOOK_DETAIL}/${bookId}/merge`,
    { duplicateBookId },
    { headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" } }
  );
};

// BA v2 Phase 1 P1-02 (be-report.md, bổ sung 2026-09-19) — "Review Bombing":
// khoá review MỚI cho 1 sách (review cũ không bị ảnh hưởng). ADMIN hoặc user
// có permission review:lock.
export const setReviewsLock = async (bookId, locked) => {
  return await httpClient.patch(`${API.BOOK_DETAIL}/${bookId}/reviews-lock`, null, {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { locked },
  });
};

// BA v2 Phase 3 §4.3 (be-report.md, bổ sung 2026-09-22) — Shelf Privacy.
// Thiếu document = mặc định PUBLIC (chưa từng cài đặt gì), GET luôn trả 200.
export const getMyShelfPrivacy = async () => {
  return await httpClient.get(API.SHELF_PRIVACY, { headers: { Authorization: `Bearer ${getToken()}` } });
};

export const updateMyShelfPrivacy = async (privacy) => {
  return await httpClient.patch(API.SHELF_PRIVACY, { privacy }, { headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" } });
};

// Xem Kệ sách của NGƯỜI KHÁC — 403 { code: 1048 } nếu bị chặn bởi PRIVATE
// hoặc FRIENDS_ONLY (không phải bạn bè); chủ shelf/ADMIN luôn xem được.
export const getUserReadingList = async (userId, page = 0, size = 12, status) => {
  return await httpClient.get(`${API.USER_READING_LIST_BASE}/${userId}/reading-list`, {
    headers: { Authorization: `Bearer ${getToken()}` },
    params: { page, size, status },
  });
};

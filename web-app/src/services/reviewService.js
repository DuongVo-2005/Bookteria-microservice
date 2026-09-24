import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

export const getReviews = async (bookId, page, size) => {
  return await httpClient.get(`${API.BOOK_REVIEWS}/${bookId}/reviews`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
    params: {
      page: page,
      size: size,
    },
  });
};

// BA Backlog FEAT-03 (be-report.md, bổ sung 2026-09-19): hasSpoiler optional,
// mặc định false ở BE nếu không gửi (không breaking cho lời gọi cũ).
export const createReview = async (bookId, rating, content, hasSpoiler = false) => {
  return await httpClient.post(
    `${API.BOOK_REVIEWS}/${bookId}/reviews`,
    {
      rating: rating,
      content: content,
      hasSpoiler: hasSpoiler,
    },
    {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        "Content-Type": "application/json",
      },
    }
  );
};

export const updateReview = async (reviewId, rating, content, hasSpoiler = false) => {
  return await httpClient.put(
    `${API.REVIEW_DETAIL}/${reviewId}`,
    {
      rating: rating,
      content: content,
      hasSpoiler: hasSpoiler,
    },
    {
      headers: {
        Authorization: `Bearer ${getToken()}`,
        "Content-Type": "application/json",
      },
    }
  );
};

export const deleteReview = async (reviewId) => {
  return await httpClient.delete(`${API.REVIEW_DETAIL}/${reviewId}`, {
    headers: {
      Authorization: `Bearer ${getToken()}`,
    },
  });
};

// Toggle — gọi lại lần 2 tự bỏ vote (helpfulByMe: false trong response).
export const toggleReviewHelpful = async (reviewId) => {
  return await httpClient.post(
    `${API.REVIEW_DETAIL}/${reviewId}/helpful`,
    {},
    { headers: { Authorization: `Bearer ${getToken()}` } }
  );
};

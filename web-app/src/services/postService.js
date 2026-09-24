import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

export const getMyPosts = async (page) => {
  return await httpClient.get(API.MY_POST, { headers: authHeader(), params: { page, size: 10 } });
};

export const getFeed = async (page = 0, size = 10) => {
  return await httpClient.get(API.POST_FEED, { headers: authHeader(), params: { page, size } });
};

export const getFriendsFeed = async (page = 0, size = 10) => {
  return await httpClient.get(API.POST_FEED_FRIENDS, { headers: authHeader(), params: { page, size } });
};

// Phase 6 (be-report.md, 2026-09-18): gộp bạn bè + sách quan tâm, KHÔNG gồm
// bài đăng trong group (2 khái niệm khác nhau ở BE, không trộn).
export const getPersonalizedFeed = async (page = 0, size = 10) => {
  return await httpClient.get(`${API.POST_FEED}/personalized`, { headers: authHeader(), params: { page, size } });
};

export const getUserPosts = async (userId, page = 0, size = 10) => {
  return await httpClient.get(`${API.POST_USERS}/${userId}/posts`, { headers: authHeader(), params: { page, size } });
};

export const getPostById = async (postId) => {
  return await httpClient.get(`${API.POST_BASE}/${postId}`, { headers: authHeader() });
};

// PostRequest thật (be-report.md Phase 3): { content, visibility?, bookId?,
// mediaUrls? } — visibility/bookId/mediaUrls đều optional, mặc định PUBLIC.
export const createPost = async (content, { visibility, bookId, mediaUrls } = {}) => {
  const body = { content };
  if (visibility) body.visibility = visibility;
  if (bookId) body.bookId = bookId;
  if (mediaUrls && mediaUrls.length > 0) body.mediaUrls = mediaUrls;
  return await httpClient.post(API.CREATE_POST, body, { headers: jsonHeader() });
};

export const toggleLikePost = async (postId) => {
  return await httpClient.post(`${API.POST_BASE}/${postId}/like`, {}, { headers: authHeader() });
};

export const getPostComments = async (postId, page = 0, size = 10) => {
  return await httpClient.get(`${API.POST_BASE}/${postId}/comments`, { headers: authHeader(), params: { page, size } });
};

export const addPostComment = async (postId, content) => {
  return await httpClient.post(`${API.POST_BASE}/${postId}/comments`, { content }, { headers: jsonHeader() });
};

export const updatePostComment = async (commentId, content) => {
  return await httpClient.put(`${API.POST_COMMENTS}/${commentId}`, { content }, { headers: jsonHeader() });
};

export const deletePostComment = async (commentId) => {
  return await httpClient.delete(`${API.POST_COMMENTS}/${commentId}`, { headers: authHeader() });
};

// Phase 4 (be-report.md, 2026-09-18): tác giả HOẶC platform ADMIN mới xoá
// được post (trước đó post-service không có deletePost nào cả).
export const deletePost = async (postId) => {
  return await httpClient.delete(`${API.POST_BASE}/${postId}`, { headers: authHeader() });
};

// ShareRequest thật: content optional (có thể repost không kèm lời bình).
export const sharePost = async (postId, content) => {
  const body = {};
  if (content && content.trim()) body.content = content.trim();
  return await httpClient.post(`${API.POST_BASE}/${postId}/share`, body, { headers: jsonHeader() });
};

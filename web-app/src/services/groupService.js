import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });
const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });

export const createGroup = async (data) => {
  return await httpClient.post(API.GROUPS, data, { headers: jsonHeader() });
};

export const getGroups = async (search, category, page = 0, size = 10) => {
  return await httpClient.get(API.GROUPS, { headers: authHeader(), params: { search, category, page, size } });
};

export const getMyGroups = async () => {
  return await httpClient.get(`${API.GROUPS}/my`, { headers: authHeader() });
};

// Phase 6 (be-report.md, 2026-09-18): sắp xếp theo điểm popularity giảm dần.
export const getPopularGroups = async (category, page = 0, size = 10) => {
  return await httpClient.get(`${API.GROUPS}/popular`, { headers: authHeader(), params: { category, page, size } });
};

export const getGroupDetail = async (groupId) => {
  return await httpClient.get(`${API.GROUPS}/${groupId}`, { headers: authHeader() });
};

// Phase 4 (be-report.md, 2026-09-18): group OWNER HOẶC platform ADMIN mới
// xoá được nhóm (trước đó group-service không có deleteGroup nào cả).
export const deleteGroup = async (groupId) => {
  return await httpClient.delete(`${API.GROUPS}/${groupId}`, { headers: authHeader() });
};

export const joinGroup = async (groupId) => {
  return await httpClient.post(`${API.GROUPS}/${groupId}/join`, {}, { headers: authHeader() });
};

export const leaveGroup = async (groupId) => {
  return await httpClient.delete(`${API.GROUPS}/${groupId}/leave`, { headers: authHeader() });
};

export const getGroupMembers = async (groupId) => {
  return await httpClient.get(`${API.GROUPS}/${groupId}/members`, { headers: authHeader() });
};

export const changeMemberRole = async (groupId, userId, role) => {
  return await httpClient.put(`${API.GROUPS}/${groupId}/members/${userId}/role`, { role }, { headers: jsonHeader() });
};

export const removeMember = async (groupId, userId) => {
  return await httpClient.delete(`${API.GROUPS}/${groupId}/members/${userId}`, { headers: authHeader() });
};

export const transferOwnership = async (groupId, userId) => {
  return await httpClient.post(`${API.GROUPS}/${groupId}/members/${userId}/transfer-ownership`, {}, { headers: authHeader() });
};

export const getGroupPosts = async (groupId, page = 0, size = 10) => {
  return await httpClient.get(`${API.GROUPS}/${groupId}/posts`, { headers: authHeader(), params: { page, size } });
};

// GroupPostCreateRequest thật chỉ nhận {content, bookId} — bookId là String,
// KHÔNG gửi kèm bookTitle/authorName/coverImage (BE tự join lại từ
// book-service, trả về qua field bookRef trong response).
export const createGroupPost = async (groupId, content, bookId) => {
  return await httpClient.post(`${API.GROUPS}/${groupId}/posts`, { content, bookId }, { headers: jsonHeader() });
};

export const toggleLikePost = async (groupId, postId) => {
  return await httpClient.post(`${API.GROUPS}/${groupId}/posts/${postId}/like`, {}, { headers: authHeader() });
};

export const addPostComment = async (groupId, postId, content) => {
  return await httpClient.post(`${API.GROUPS}/${groupId}/posts/${postId}/comments`, { content }, { headers: jsonHeader() });
};

export const deleteGroupPost = async (groupId, postId) => {
  return await httpClient.delete(`${API.GROUPS}/${groupId}/posts/${postId}`, { headers: authHeader() });
};

// BA Backlog OPS-01 (be-report.md, bổ sung 2026-09-19): nhóm bật
// requireApproval thì POST .../posts trả status=PENDING_APPROVAL thay vì
// PUBLISHED — 3 hàm dưới cho OWNER/ADMIN nhóm duyệt/từ chối.
export const getPendingGroupPosts = async (groupId, page = 0, size = 10) => {
  return await httpClient.get(`${API.GROUPS}/${groupId}/posts/pending`, { headers: authHeader(), params: { page, size } });
};

export const approveGroupPost = async (groupId, postId) => {
  return await httpClient.patch(`${API.GROUPS}/${groupId}/posts/${postId}/approve`, {}, { headers: authHeader() });
};

export const rejectGroupPost = async (groupId, postId, reason) => {
  const body = {};
  if (reason && reason.trim()) body.reason = reason.trim();
  return await httpClient.patch(`${API.GROUPS}/${groupId}/posts/${postId}/reject`, body, { headers: jsonHeader() });
};

export const deletePostComment = async (groupId, postId, commentId) => {
  return await httpClient.delete(`${API.GROUPS}/${groupId}/posts/${postId}/comments/${commentId}`, { headers: authHeader() });
};

// BA v2 Phase 2 §2.3 (be-report.md, bổ sung 2026-09-19) — thay thế hoàn toàn
// Group.hidden (boolean) cũ bằng Group.status (ACTIVE/FROZEN/SUSPENDED).
// GET /groups/admin (ADMIN) trả TẤT CẢ nhóm kể cả SUSPENDED/Private, khác
// GET /groups thường (đã tự lọc theo discovery).
export const getAdminGroups = async (search, category, status, page = 0, size = 10) => {
  return await httpClient.get(`${API.GROUPS}/admin`, { headers: authHeader(), params: { search, category, status, page, size } });
};

export const updateGroupStatus = async (groupId, status) => {
  return await httpClient.patch(`${API.GROUPS}/${groupId}/status`, null, { headers: authHeader(), params: { status } });
};

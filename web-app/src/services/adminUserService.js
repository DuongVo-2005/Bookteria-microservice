import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

const jsonHeader = () => ({ Authorization: `Bearer ${getToken()}`, "Content-Type": "application/json" });
const authHeader = () => ({ Authorization: `Bearer ${getToken()}` });

// BA Backlog OPS-03 (be-report.md, bổ sung 2026-09-19) — BREAKING CHANGE:
// trước đây POST /users/{userId}/lock không cần body, giờ bắt buộc
// {reason, duration}. duration ∈ THREE_DAYS|SEVEN_DAYS|THIRTY_DAYS|PERMANENT.
export const lockUser = async (userId, reason, duration) => {
  return await httpClient.post(`${API.IDENTITY_USERS}/${userId}/lock`, { reason, duration }, { headers: jsonHeader() });
};

// Không đổi — không cần body.
export const unlockUser = async (userId) => {
  return await httpClient.post(`${API.IDENTITY_USERS}/${userId}/unlock`, {}, { headers: authHeader() });
};

// api-catalog.md #8, đã xác nhận từ trước — dùng lại để đọc `permissions`
// trực tiếp hiện có của user (BA v2 Phase 1, be-report.md, 2026-09-19) trước
// khi mở dialog chỉnh quyền, tránh phải tự đoán trạng thái ban đầu.
export const getIdentityUser = async (userId) => {
  return await httpClient.get(`${API.IDENTITY_USERS}/${userId}`, { headers: authHeader() });
};

// BA v2 Phase 1 P1-01 (be-report.md, bổ sung 2026-09-19) — THAY THẾ toàn bộ
// danh sách permission trực tiếp của user, không phải thêm/bớt từng cái.
export const updateUserPermissions = async (userId, permissions) => {
  return await httpClient.patch(`${API.IDENTITY_USERS}/${userId}/permissions`, { permissions }, { headers: jsonHeader() });
};

// BA v2 Phase 2 §2.2 (be-report.md, bổ sung 2026-09-19) — không có body,
// không trả mật khẩu (chỉ gửi qua email, tránh lộ qua log/network capture).
export const resetUserPassword = async (userId) => {
  return await httpClient.post(`${API.IDENTITY_USERS}/${userId}/reset-password`, {}, { headers: authHeader() });
};

// Vô hiệu hoá + anonymize tài khoản — KHÔNG có cơ chế khôi phục (khác
// unlockUser), đúng tinh thần "soft delete" tự đóng tài khoản.
export const deactivateUser = async (userId) => {
  return await httpClient.post(`${API.IDENTITY_USERS}/${userId}/deactivate`, {}, { headers: authHeader() });
};

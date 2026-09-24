import httpClient from "../configurations/httpClient";
import { API } from "../configurations/configuration";
import { getToken } from "./localStorageService";

// file-service — lần đầu FE gọi tới endpoint upload chung này (trước giờ
// avatar dùng endpoint riêng của profile-service, import ebook dùng endpoint
// riêng của reading-service). Whitelist BE: jpg/jpeg/png/gif/webp, tối đa
// 10MB (be-report.md Phase 7) — validate nhẹ phía client trước khi gửi để
// UX tốt hơn, BE vẫn là lớp chặn cuối cùng.
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
const MAX_SIZE_BYTES = 10 * 1024 * 1024;

export const validateMediaFile = (file) => {
  if (!file) return "Vui lòng chọn 1 file ảnh.";
  if (!ALLOWED_TYPES.includes(file.type)) return "Chỉ hỗ trợ ảnh JPG, PNG, GIF hoặc WEBP.";
  if (file.size > MAX_SIZE_BYTES) return "Kích thước ảnh vượt quá 10MB.";
  return null;
};

export const uploadMedia = async (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return await httpClient.post(API.MEDIA_UPLOAD, formData, {
    headers: { Authorization: `Bearer ${getToken()}`, "Content-Type": "multipart/form-data" },
  });
};

// BA Backlog GAP-05 (be-report.md, bổ sung 2026-09-19): BẮT BUỘC gọi sau khi
// file đã thật sự được gắn vào post/profile — nếu không, file tự bị xoá sau
// 24h dù đang được dùng (cleanupOrphanFiles() job của file-service).
export const confirmMediaAttach = async (fileId) => {
  return await httpClient.patch(`${API.MEDIA_BASE}/${fileId}/confirm-attach`, {}, { headers: { Authorization: `Bearer ${getToken()}` } });
};
